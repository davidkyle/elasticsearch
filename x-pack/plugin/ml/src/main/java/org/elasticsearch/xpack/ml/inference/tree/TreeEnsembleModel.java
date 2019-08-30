/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TreeEnsembleModel {

    private final List<Tree> trees;
    private final Map<String, Integer> featureMap;

    private TreeEnsembleModel(List<Tree> trees, Map<String, Integer> featureMap) {
        this.trees = Collections.unmodifiableList(trees);
        this.featureMap = featureMap;
    }

    public int numFeatures() {
        return featureMap.size();
    }

    public int numTrees() {
        return trees.size();
    }

    public List<Integer> checkForNull() {
        List<Integer> missing = new ArrayList<>();
        for (Tree tree : trees) {
            missing.addAll(tree.missingNodes());
        }
        return missing;
    }

    public double predictFromDoc(Map<String, Object> features) {
        List<Double> featureVec = docToFeatureVector(features);
        List<Double> predictions = trees.stream().map(tree -> tree.predict(featureVec)).collect(Collectors.toList());
        return mergePredictions(predictions);
    }

    public double predict(Map<String, Double> features) {
        List<Double> featureVec = doubleDocToFeatureVector(features);
        List<Double> predictions = trees.stream().map(tree -> tree.predict(featureVec)).collect(Collectors.toList());
        return mergePredictions(predictions);
    }

    public List<List<Tree.Node>> trace(Map<String, Double> features) {
        List<Double> featureVec = doubleDocToFeatureVector(features);
        return trees.stream().map(tree -> tree.trace(featureVec)).collect(Collectors.toList());
    }

    double mergePredictions(List<Double> predictions) {
        return predictions.stream().mapToDouble(f -> f).summaryStatistics().getSum();
    }

    List<Double> doubleDocToFeatureVector(Map<String, Double> features) {
        List<Double> featureVec = Arrays.asList(new Double[featureMap.size()]);

        for (Map.Entry<String, Double> keyValue : features.entrySet()) {
            if (featureMap.containsKey(keyValue.getKey())) {
                featureVec.set(featureMap.get(keyValue.getKey()), keyValue.getValue());
            }
        }

        return featureVec;
    }

    List<Double> docToFeatureVector(Map<String, Object> features) {
        List<Double> featureVec = Arrays.asList(new Double[featureMap.size()]);

        for (Map.Entry<String, Object> keyValue : features.entrySet()) {
            if (featureMap.containsKey(keyValue.getKey())) {
                Double value = (Double)keyValue.getValue();
                if (value != null) {
                    featureVec.set(featureMap.get(keyValue.getKey()), value);
                }
            }
        }

        return featureVec;
    }

    public static ModelBuilder modelBuilder(Map<String, Integer> featureMap) {
        return new ModelBuilder(featureMap);
    }

    public static class ModelBuilder {
        private List<Tree> trees;
        private Map<String, Integer> featureMap;

        public ModelBuilder(Map<String, Integer> featureMap) {
            this.featureMap = featureMap;
            trees = new ArrayList<>();
        }

        public ModelBuilder addTree(Tree tree) {
            trees.add(tree);
            return this;
        }

        public TreeEnsembleModel build() {
            return new TreeEnsembleModel(trees, featureMap);
        }
    }
}
