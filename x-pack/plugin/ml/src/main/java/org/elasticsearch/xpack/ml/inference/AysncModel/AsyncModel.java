/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.AysncModel;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.xpack.ml.inference.Model;

import java.util.function.BiConsumer;

public abstract class AsyncModel implements Model {


    private final AsyncModelLoader loader;


    public AsyncModel(AsyncModelLoader loader) {
        this.loader = loader;

        if (loader.isLoadedSucessfully()) {
            createModel(loader.getGetResponse());
        } else {
            // register
        }
    }

    @Override
    public void infer(IngestDocument document, BiConsumer<IngestDocument, Exception> handler) {

    }

    protected abstract void createModel(GetResponse getResponse);
}
