/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.AysncModel;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.xpack.ml.inference.InferenceProcessor;
import org.elasticsearch.xpack.ml.inference.Model;
import org.elasticsearch.xpack.ml.inference.ModelLoader;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AsyncModelLoader implements ModelLoader {

    public static final String MODEL_TYPE = "model_stored_in_index";
    private static final String INDEX = "index";

    private final Client client;

    private AtomicBoolean isLoaded = new AtomicBoolean(false);
    private volatile GetResponse response;
    private volatile Exception loadingException;

    public AsyncModelLoader(Client client) {
        this.client = client;
    }

    @Override
    public Model load(String modelId, String processorTag, boolean ignoreMissing, Map<String, Object> config) throws Exception {

        ActionListener<GetResponse> listener = ActionListener.wrap(this::setResponse, this::setLoadingException);

        String index = readIndexName(processorTag, config);
        load(modelId, index, listener);

        return new AsyncModel(this);
    }

    @Override
    public void consumeConfiguration(String processorTag, Map<String, Object> config) {
        readIndexName(processorTag, config);
    }

    /**
     * Read the name of the index to get the model state from.
     * The default is to read the string value of object {@value #INDEX}.
     *
     * @param processorTag Tag
     * @param config The processor config
     * @return The name of the index containing the model
     */
    protected String readIndexName(String processorTag, Map<String, Object> config) {
        return ConfigurationUtils.readStringProperty(InferenceProcessor.TYPE, processorTag, config, INDEX);
    }


    private void load(String id, String index, ActionListener<GetResponse> listener) {
        isLoaded.compareAndSet(false, true);
        client.prepareGet(index, null, id).execute(listener);
    }

    private void setResponse(GetResponse response) {
        this.response = response;
        isLoaded.set(true);
    }

    private void setLoadingException(Exception e) {
        this.loadingException = e;
    }

}
