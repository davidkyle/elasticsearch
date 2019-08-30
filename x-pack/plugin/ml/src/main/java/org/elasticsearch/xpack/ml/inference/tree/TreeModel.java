/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.tree;

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.xpack.ml.inference.Model;

import java.util.function.BiConsumer;

public class TreeModel implements Model {

    private String targetFieldName;
    private TreeEnsembleModel ensemble;


    TreeModel(TreeEnsembleModel ensemble, String targetFieldName) {
        this.ensemble = ensemble;
        this.targetFieldName = targetFieldName;
    }

    @Override
    public void infer(IngestDocument document, BiConsumer<IngestDocument, Exception> handler) {
        Double prediction = ensemble.predictFromDoc(document.getSourceAndMetadata());
        document.setFieldValue(targetFieldName, prediction);
        handler.accept(document, null);
    }
}
