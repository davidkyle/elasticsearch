/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;


import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

// Given a sentence, generates one FloatFeatureValue for each "relevant" Unicode
// script (see below): each such feature indicates the script and the ratio of
// UTF8 characters in that script, in the given sentence.
//
// What is a relevant script?  Recognizing all 100+ Unicode scripts would
// require too much code size and runtime.  Instead, we focus only on a few
// scripts that communicate a lot of language information: e.g., the use of
// Hiragana characters almost always indicates Japanese, so Hiragana is a
// "relevant" script for us.  The Latin script is used by dozens of language, so
// Latin is not relevant in this context.
public class RelevantScriptFeature {

    // Extract features from text and add to feature vector
    // NOTE: input text is assumed to be normalised (e.g. " this text is written in english")
    // TODO - manage exception handling
    public static FeatureVector getRelevantScriptFeature(String text) throws UnsupportedEncodingException {
        FeatureVector result = new FeatureVector();

        if (text.isEmpty()) {
            return result;
        }

        // counts[s] is the number of characters with script s.
        // Use treemap so results are sorted in scriptid order
        TreeMap<ScriptDetector.Script, Integer> counts = new TreeMap<>();

        int totalCount = 0;

        for (int i = 1; i <= text.length(); ++i) {
            String curr = text.substring(i - 1, i);
            byte[] bytes = text.substring(i - 1, i).getBytes("UTF8");
            int numBytes = bytes.length;

            // Skip spaces, numbers, punctuation, and all other non-alpha ASCII
            // characters: these characters are used in so many languages, they do not
            // communicate language-related information.
            // TODO - check whether we need to look at mark
            if ((numBytes == 1) && !curr.chars().allMatch(Character::isLetter)) {
                continue;
            }

            ScriptDetector.Script script = ScriptDetector.getScript(curr);

            // Upsert array
            counts.put(script, counts.getOrDefault(script, 0) + 1);

            totalCount++;
        }

        for (Map.Entry<ScriptDetector.Script, Integer> entry : counts.entrySet()) {
            ScriptDetector.Script scriptId = entry.getKey();
            int count = entry.getValue();
            if (count > 0) {
                float weight = (float) count / (float) totalCount;
                result.add(FeatureVector.FeatureType.CONTINUOUS_BAG_OF_RELEVANT_SCRIPTS,
                    FeatureVector.FeatureValue.getFeatureValue(scriptId.toInt(), weight));
            }
        }

        return result;
    }
}
