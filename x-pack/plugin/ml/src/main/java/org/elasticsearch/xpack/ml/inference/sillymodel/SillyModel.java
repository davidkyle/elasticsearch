/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.sillymodel;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.Randomness;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.xpack.ml.inference.AsyncModel.AsyncModel;

import java.util.Random;
import java.util.function.BiConsumer;

/**
 * Trivial model whose only purpose is to aid code design
 */
public class SillyModel extends AsyncModel {

    private static final String TARGET_FIELD = "hotdog_or_not";

    private final Random random;

    public SillyModel(boolean ignoreMissing) {
        super(ignoreMissing);
        random = Randomness.get();
    }

    @Override
    public void inferPrivate(IngestDocument document, BiConsumer<IngestDocument, Exception> handler) {
        document.setFieldValue(TARGET_FIELD, random.nextBoolean() ? "hotdog" : "not");
        handler.accept(document, null);
    }

    @Override
    protected void createModel(GetResponse getResponse) {

    }
}
