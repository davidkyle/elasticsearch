/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import java.io.FileNotFoundException;
import java.util.*;

public class NNetLanguageIdentifier {
    public static final int kMaxNumInputBytesToConsider = 10000;
    public static final String kUnknown = "und";
    float kReliabilityThreshold = 0.7f;
    float kReliabilityHrBsThreshold = 0.5f;

    private static final int minNumBytes = 0;
    private static final int maxNumBytes = 0;

    private final EmbeddingNetwork network;

    public NNetLanguageIdentifier() throws FileNotFoundException {
        network = new EmbeddingNetwork(new NNetParams());
    }

    public Result findLanguage(String input) throws Exception {
        // truncate string if necessary
        String text = FeatureUtils.truncateToNumValidBytes(input, kMaxNumInputBytesToConsider);

        // clean up text
        text = FeatureUtils.cleanAndLowerText(text);

        // cld3 removes repetitive chunks or ones containing mostly spaces
        // this is omitted for now as it is an optimisation (TODO investigate)
        return findLanguageOfValidUTF8(text);
    }

    // Public for testing purposes
    public ArrayList<FeatureVector> getFeatures(String text) throws Exception {
        ArrayList<FeatureVector> features = new ArrayList<>();

        // Create feature vector based on cld3 embedding
        features.add(ContinuousBagOfNGramsFeature.getNGramFeatureValue(text, 2, 1000));
        features.add(ContinuousBagOfNGramsFeature.getNGramFeatureValue(text, 4, 5000));
        features.add(RelevantScriptFeature.getRelevantScriptFeature(text));
        features.add(ScriptFeature.getScriptFeature(text));
        features.add(ContinuousBagOfNGramsFeature.getNGramFeatureValue(text, 3, 5000));
        features.add(ContinuousBagOfNGramsFeature.getNGramFeatureValue(text, 1, 100));

        return features;
    }

    private void dumpFeatures(String text, ArrayList<FeatureVector> featureVectors) {
        System.out.println("'" + text + "'" + text.length() + "{");
        System.out.println("features:" + featureVectors.size());
        for (FeatureVector itr : featureVectors) {
            System.out.println("\tvector:{" + itr.size());
            for (int i = 0; i < itr.size(); ++i) {
                //FeatureType *featureType = itr->type(i);
                long featureValue = itr.getValueAt(i);

                //System.out.println("\t\t" + featureType->name() << ":" << featureValue);
                System.out.println("\t\t" + featureValue);
            }
            System.out.println("\t}");
        }
        System.out.println("}");
    }

    private Result findLanguageOfValidUTF8(String text) throws Exception {
        // Create the feature vector with it actually a list
        // of feature vectors
        ArrayList<FeatureVector> features = getFeatures(text);

        //dumpFeatures(text, features);

        ArrayList<Float> scores = network.computeFinalScores(features);

        int prediction_id = -1;
        float maxVal = -Float.MAX_VALUE;
        for (int i = 0; i < scores.size(); ++i) {
            if (scores.get(i) > maxVal) {
                prediction_id = i;
                maxVal = scores.get(i);
            }
        }

        // Compute probability.
        double diffSum = 0.0;
        for (int i = 0; i < scores.size(); ++i) {
            diffSum += Math.exp(scores.get(i) - maxVal);
        }
        double logSumExp = maxVal + Math.log(diffSum);

        float probability = (float) Math.exp(maxVal - logSumExp);
        String language = LangIdentParams.getLanguageName(prediction_id);
        boolean isReliable = resultIsReliable(language, probability);

        // get all probabilities
        ArrayList<Map.Entry<String, Float>> probabilities = new ArrayList<>();
        for (int i = 0; i < scores.size(); ++i) {
            String lang = LangIdentParams.getLanguageName(i);
            float prob = (float) Math.exp(scores.get(i) - logSumExp);
            Map.Entry<String, Float> entry = new AbstractMap.SimpleEntry<>(lang, prob);
            probabilities.add(entry);
        }
        // TODO - optimise this (if necessary)
        probabilities.sort(Map.Entry.comparingByValue());
        Collections.reverse(probabilities);

        return new Result(language, probability, probabilities.subList(0, Result.TOP_N), isReliable);
    }

    // Returns "true" if the languge prediction is reliable based on the
    // probability, and "false" otherwise.
    boolean resultIsReliable(String language, float probability) {
        if (language == "hr" || language == "bs") {
            return (probability >= kReliabilityHrBsThreshold);
        } else {
            return (probability >= kReliabilityThreshold);
        }
    }

    public static class Result {
        public final static int TOP_N = 5;

        private final String language;
        private final float probability;
        private final List<Map.Entry<String, Float>> topProbabilities;
        private final boolean isReliable;

        public Result(String language, float probability,
                      List<Map.Entry<String, Float>> topProbabilities, boolean isReliable) {
            this.language = language;
            this.probability = probability;
            this.topProbabilities = topProbabilities;
            this.isReliable = isReliable;
        }

        // Initialise empty (unknown)
        public Result() {
            // Fill result with unknown
            this.language = NNetLanguageIdentifier.kUnknown;
            this.probability = 0.0f;
            this.topProbabilities = new ArrayList<Map.Entry<String, Float>>();
            this.topProbabilities.add(new AbstractMap.SimpleEntry<>(this.language, this.probability));
            this.isReliable = false;
        }

        public String getLanguage() {
            return language;
        }

        public double getProbability() {
            return probability;
        }

        public List<Map.Entry<String, Float>> getTopProbabilities() { return topProbabilities; }

        public boolean isReliable() { return isReliable; }

        @Override
        public String toString() {
            return "Result{" +
                "language='" + language + '\'' +
                ", probability=" + probability +
                ", topProbabilities=" + topProbabilities +
                ", isReliable=" + isReliable +
                '}';
        }

    }
}
