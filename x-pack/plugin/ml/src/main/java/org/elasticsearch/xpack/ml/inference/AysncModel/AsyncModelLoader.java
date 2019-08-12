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
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class AsyncModelLoader<T extends AsyncModel> implements ModelLoader {

    public static final String MODEL_TYPE = "model_stored_in_index";
    private static final String INDEX = "index";

    private final Client client;
    private final Function<AsyncModelLoader, T> modelSupplier;

    private AtomicBoolean loadingFinished = new AtomicBoolean(false);
    private volatile GetResponse response;
    private volatile Exception loadingException;
    private volatile AsyncModel loadedListener;


    public AsyncModelLoader(Client client, Function<AsyncModelLoader, T> modelSupplier) {
        this.client = client;
        this.modelSupplier = modelSupplier;
    }

    @Override
    synchronized public Model load(String modelId, String processorTag, boolean ignoreMissing, Map<String, Object> config) throws Exception {

        ActionListener<GetResponse> listener = ActionListener.wrap(this::setResponse, this::setLoadingException);

        String index = readIndexName(processorTag, config);
        String documentId = documentId(modelId, config);
        load(documentId, index, listener);

        loadedListener = modelSupplier.apply(this);
        return loadedListener;
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

    /**
     * Construct the document Id used in the GET request.
     * This function is intended to be overridden, this implementation simply returns {@code modelId}
     *
     * @param modelId The model Id
     * @param config  The processor config
     * @return  The document Id
     */
    protected String documentId(String modelId, Map<String, Object> config) {
        return modelId;
    }

    void registerLoadedListener(AsyncModel model) {

    }

    private void load(String id, String index, ActionListener<GetResponse> listener) {
        loadingFinished.compareAndSet(false, true);
        client.prepareGet(index, null, id).execute(listener);
    }

    synchronized private void setResponse(GetResponse response) {
        this.response = response;
        loadingFinished.set(true);
    }

    private void setLoadingException(Exception e) {
        this.loadingException = e;
        loadingFinished.set(true);
    }

    public boolean isLoadingFinished() {
        return loadingFinished.get();
    }

    public boolean isLoadedSucessfully() {
        return loadingFinished.get() && response != null;
    }

    public GetResponse getGetResponse() {
        return response;
    }

    public Exception getLoadingException() {
        return loadingException;
    }

}
