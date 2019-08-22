/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import java.util.Map;
import java.util.TreeMap;

public class ContinuousBagOfNGramsFeature {

    // Extract features from text and add to feature vector
    // NOTE: input text is assumed to be normalised (e.g. " this text is written in english")
    public static FeatureVector getNGramFeatureValue(String text,
                                                     int nGramSize,
                                                     int idDimension) throws Exception {
        // First add terminators:
        // Split the text based on spaces to get tokens, adds "^"
        // to the beginning of each token, and adds "$" to the end of each token.
        // e.g.
        // " this text is written in english" goes to
        // "^$ ^this$ ^text$ ^is$ ^written$ ^in$ ^english$ ^$"
        StringBuilder newText = new StringBuilder("^");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                newText.append("$ ^");
            } else {
                newText.append(c);
            }
        }
        newText.append("$");

        // Find the char ngrams
        // ^$ ^this$ ^text$ ^is$ ^written$ ^in$ ^english$ ^$"
        // nGramSize = 2
        // [{h$},{sh},{li},{gl},{in},{en},{^$},...]
        Map<String, Integer> charNGrams = new TreeMap<>();

        int countSum = 0;
        for (int start = 0; start <= (newText.toString().length()) - nGramSize; ++start) {
            StringBuilder charNGram = new StringBuilder();

            int index;
            for (index = 0; index < nGramSize; ++index) {
                char currentChar = newText.toString().charAt(start + index);
                if (currentChar == ' ') {
                    break;
                }
                charNGram.append(currentChar);
            }

            if (index == nGramSize) {
                // upsert counts for the ngram
                charNGrams.put(charNGram.toString(),
                    charNGrams.getOrDefault(charNGram.toString(), 0) + 1);
                ++countSum;
            }
        }

        FeatureVector result = new FeatureVector();

        // Add to the feature vector values based on bespoke hashes
        FeatureVector.FeatureType type = toType(nGramSize, idDimension);

        for (Map.Entry<String, Integer> entry : charNGrams.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();

            float weight = (float) value / (float) countSum;

            int id = Integer.remainderUnsigned(FeatureUtils.Hash32WithDefaultSeed(key), idDimension);

            result.add(type, FeatureVector.FeatureValue.getFeatureValue(id, weight));
        }

        return result;
    }

    // Currently only a subset of specific features types are supported
    // These correspond to the feature types embedded in the model
    private static FeatureVector.FeatureType toType(int nGramSize, int idDimension) throws IllegalArgumentException {
        if (nGramSize == 1 && idDimension == 100) {
            return FeatureVector.FeatureType.CONTINUOUS_BAG_OF_NGRAMS_100_1;
        }
        if (nGramSize == 2 && idDimension == 1000) {
            return FeatureVector.FeatureType.CONTINUOUS_BAG_OF_NGRAMS_1000_2;
        }
        if (nGramSize == 3 && idDimension == 5000) {
            return FeatureVector.FeatureType.CONTINUOUS_BAG_OF_NGRAMS_5000_3;
        }
        if (nGramSize == 4 && idDimension == 5000) {
            return FeatureVector.FeatureType.CONTINUOUS_BAG_OF_NGRAMS_5000_4;
        }

        throw new IllegalArgumentException("CONTINUOUS_BAG_OF_NGRAMS feature not supported for + " +
            "[" + nGramSize + "]" +
            "[" + idDimension + "]" +
            "cannot identify language as source field is missing.");
    }
}
