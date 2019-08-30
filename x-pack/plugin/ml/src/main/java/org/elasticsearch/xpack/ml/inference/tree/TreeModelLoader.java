/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.tree;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.client.Client;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.xpack.ml.inference.InferenceProcessor;
import org.elasticsearch.xpack.ml.inference.ModelLoader;
import org.elasticsearch.xpack.ml.inference.tree.xgboost.XgBoostJsonParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TreeModelLoader implements ModelLoader {

    private static final Logger logger = LogManager.getLogger(TreeModelLoader.class);

    public static final String MODEL_TYPE = "tree";

    private static String INDEX = "index";
    private static String TARGET_FIELD = "target_field";

    private final Client client;

    public TreeModelLoader(Client client) {
        this.client = client;
    }

    @Override
    public TreeModel load(String modelId, String processorTag, boolean ignoreMissing, Map<String, Object> config) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TreeEnsembleModel> modelRef = new AtomicReference<>();
        AtomicReference<Exception> exception = new AtomicReference<>();

        LatchedActionListener<TreeEnsembleModel> listener = new LatchedActionListener<>(
                ActionListener.wrap(modelRef::set, exception::set), latch
        );

        String index = ConfigurationUtils.readStringProperty(InferenceProcessor.TYPE, processorTag, config, INDEX);
        String targetField = ConfigurationUtils.readStringProperty(InferenceProcessor.TYPE, processorTag, config, TARGET_FIELD);

        load(modelId, index, config, listener);
        latch.await();
        if (exception.get() != null) {
            throw exception.get();
        }

        TreeEnsembleModel ensemble = modelRef.get();
        TreeModel model = new TreeModel(ensemble, targetField);

        logger.info("loaded model with " + ensemble.numTrees() + " trees");
        return model;
    }

    @Override
    public void consumeConfiguration(String processorTag, Map<String, Object> config) {
        ConfigurationUtils.readStringProperty(InferenceProcessor.TYPE, processorTag, config, INDEX);
        ConfigurationUtils.readStringProperty(InferenceProcessor.TYPE, processorTag, config, TARGET_FIELD);
    }

    public void load(String id, String index, Map<String, Object> config, ActionListener<TreeEnsembleModel> listener) {
        client.prepareGet(index, null, id).execute(ActionListener.wrap(
                response -> {
                    if (response.isExists()) {
                        listener.onResponse(createEnsemble(response.getSourceAsMap(), featureMapFromConfig(config)));
                    } else {
                        listener.onFailure(new ResourceNotFoundException("missing model [{}], [{}]", id, index));
                    }
                },
                listener::onFailure
        ));
    }

    private Map<String, Integer> featureMapFromConfig(Map<String, Object> config) {
        // TODO, this was hard coded for a demo
        Map<String, Integer> featureMap = new HashMap<>();
        featureMap.put("f0", 0);
        featureMap.put("f1", 1);
        featureMap.put("f2", 2);
        featureMap.put("f3", 3);
        return featureMap;
    }

    @SuppressWarnings("unchecked")
    private TreeEnsembleModel createEnsemble(Map<String, Object> source, Map<String, Integer> featureMap) throws IOException {
        List<Map<String, Object>> trees = (List<Map<String, Object>>) source.get("ensemble");
        if (trees == null) {
            throw new IllegalStateException("missing trees");
        }

        return XgBoostJsonParser.parse(trees, featureMap);
    }
}
