/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.util.Supplier;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.xpack.ml.inference.Model;

import java.io.FileNotFoundException;
import java.util.function.BiConsumer;

final class LangIdentModel implements Model {

    private static final Logger logger = LogManager.getLogger(LangIdentModel.class);

    private NNetLanguageIdentifier languageIdentifier = null;
    private final String field;
    // Target field names (can be null)
    private final String targetLanguageField;
    private final String targetProbabilityField;
    private final String targetTopProbabilitiesField;
    private final String targetIsReliableField;
    private final boolean ignoreMissing;

    LangIdentModel(String field,
                          String targetLanguageField, String targetProbabilityField,
                          String targetTopProbabilitiesField, String targetIsReliableField,
                          boolean ignoreMissing) {

        this.field = field;
        this.targetLanguageField = targetLanguageField;
        this.targetProbabilityField = targetProbabilityField;
        this.targetTopProbabilitiesField = targetTopProbabilitiesField;
        this.targetIsReliableField = targetIsReliableField;
        this.ignoreMissing = ignoreMissing;


        try {
            this.languageIdentifier = new NNetLanguageIdentifier();
        } catch (FileNotFoundException e) {
            logger.error((Supplier<?>) () -> new ParameterizedMessage("Could not load lang ident model"), e);
        }
    }

    @Override
    public void infer(IngestDocument ingestDocument, BiConsumer<IngestDocument, Exception> handler) {
        String text = ingestDocument.getFieldValue(field, String.class, ignoreMissing);
        if (text == null && ignoreMissing) {
            handler.accept(ingestDocument, null);
            return;
        } else if (text == null) {
            throw new IllegalArgumentException("field [" + field + "] is null, " +
                    "cannot identify language as source field is missing.");
        }

        try {
            NNetLanguageIdentifier.Result result = languageIdentifier.findLanguage(text);

            if (targetLanguageField != null) {
                ingestDocument.setFieldValue(targetLanguageField, result.getLanguage());
            }
            if (targetProbabilityField != null) {
                ingestDocument.setFieldValue(targetProbabilityField, result.getProbability());
            }
            if (targetTopProbabilitiesField != null) {
                ingestDocument.setFieldValue(targetTopProbabilitiesField, result.getTopProbabilities().toString());
            }
            if (targetIsReliableField != null) {
                ingestDocument.setFieldValue(targetIsReliableField, result.isReliable());
            }
        } catch (Exception e) {
            logger.error("failed to find language", e);
        }
        handler.accept(ingestDocument, null);
    }
}
