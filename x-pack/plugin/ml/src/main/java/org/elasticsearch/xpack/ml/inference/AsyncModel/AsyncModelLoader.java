/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.AsyncModel;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.ingest.ConfigurationUtils;
import org.elasticsearch.xpack.ml.inference.InferenceProcessor;
import org.elasticsearch.xpack.ml.inference.ModelLoader;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

// This class is full of races.
//
// The general idea is that this class can be used to load any type of model where the
// model state has to be fetched from an index. TODO the class is poorly named

// The load() handles fetching the index document and will return a subclass of AsyncModel (type T)
// and register that object as a listener to be notified once the loading has finished
// or an error occurred. But the load() method can only be called once, if its called
// more than once it should wait for the loading to finish then notify all the listeners.

public abstract class AsyncModelLoader<T extends AsyncModel> implements ModelLoader {

    private static final Logger logger = LogManager.getLogger(AsyncModelLoader.class);

    public static final String INDEX = "index";

    private final Client client;
    private final Function<Boolean, T> modelSupplier;

    private AtomicBoolean loadingFinished = new AtomicBoolean(false);
    private volatile GetResponse response;
    private volatile Exception loadingException;
    private volatile T loadedListener;


    protected AsyncModelLoader(Client client, Function<Boolean, T> modelSupplier) {
        this.client = client;
        this.modelSupplier = modelSupplier;
    }

    @Override
    public T load(String modelId, String processorTag, boolean ignoreMissing, Map<String, Object> config) {
        String index = readIndexName(processorTag, config);
        String documentId = documentId(modelId, config);

        // TODO if this method is called twice loadedListener will be overwritten.
        loadedListener = modelSupplier.apply(ignoreMissing);
        load(documentId, index);
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

    private void load(String id, String index) {
        ActionListener<GetResponse> listener = ActionListener.wrap(this::setResponse, this::setLoadingException);

        loadingFinished.compareAndSet(false, true);
        client.prepareGet(index, null, id).execute(listener);
    }

    private synchronized void setResponse(GetResponse response) {

        this.response = response;
        loadingFinished.set(true);
        if (loadedListener != null) {
            loadedListener.imLoaded(response);
        }
    }

    private void setLoadingException(Exception e) {
        this.loadingException = e;
        loadingFinished.set(true);

    }

    public GetResponse getGetResponse() {
        return response;
    }

    public Exception getLoadingException() {
        return loadingException;
    }

}
