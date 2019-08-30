/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.AsyncModel;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.xpack.ml.inference.Model;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BiConsumer;

/**
 * A model (implements the inference function) that has its model state loaded
 * from an index document via a {@link AsyncModelLoader}. When the AsyncModelLoader
 * has fetched the document it will notify this class and subclasses then know how to
 * construct the model.
 *
 * Any ingest documents arriving while waiting for the model state to load must be queued up.
 *
 * {@link #createModel(GetResponse)} should be implemented in subclasses to read
 * the model state from the GetResponse supplied by the loader.
 *
 * {@link #inferPrivate(IngestDocument, BiConsumer)} does the actual inference.
 */
public abstract class AsyncModel implements Model {

    private final boolean ignoreMissing;

    private volatile boolean isLoaded = false;
    private volatile Exception error;

    private final Queue<Tuple<IngestDocument, BiConsumer<IngestDocument, Exception>>> documentQueue;

    protected AsyncModel(boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing;
        documentQueue = new ConcurrentLinkedDeque<>();
    }

    @Override
    public void infer(IngestDocument document, BiConsumer<IngestDocument, Exception> handler) {
        if (isLoaded) {
            inferPrivate(document, handler);
            return;
        }

        if (error != null) {
            handler.accept(null, error);
            return;
        }

        // if we have a list of requests waiting to be used then they have to be queued up
        queueRequest(document, handler);
    }

    /**
     * Should be threadsafe
     * @param document The ingest document
     * @param handler Ingest handler
     */
    protected abstract void inferPrivate(IngestDocument document, BiConsumer<IngestDocument, Exception> handler);



    void imLoaded(GetResponse getResponse) {
        createModel(getResponse);
        drainQueuedToInfer();
        isLoaded = true;
    }

    void setError(Exception exception) {
        drainQueuedToError();
        this.error = exception;
    }

    private synchronized void queueRequest(IngestDocument document, BiConsumer<IngestDocument, Exception> handler) {
        documentQueue.add(new Tuple<>(document, handler));
    }

    private synchronized void drainQueuedToInfer() {
        for (Tuple<IngestDocument, BiConsumer<IngestDocument, Exception>> request : documentQueue) {
            inferPrivate(request.v1(), request.v2());
        }
    }

    private synchronized void drainQueuedToError() {
        for (Tuple<IngestDocument, BiConsumer<IngestDocument, Exception>> request : documentQueue) {
            request.v2().accept(null, error);
        }
    }

    public boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    protected abstract void createModel(GetResponse getResponse);
}
