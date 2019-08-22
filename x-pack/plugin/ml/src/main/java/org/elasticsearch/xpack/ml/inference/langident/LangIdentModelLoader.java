/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.xpack.ml.inference.InferenceProcessor;
import org.elasticsearch.xpack.ml.inference.Model;
import org.elasticsearch.xpack.ml.inference.ModelLoader;

import java.util.Map;

import static org.elasticsearch.ingest.ConfigurationUtils.newConfigurationException;
import static org.elasticsearch.ingest.ConfigurationUtils.readStringProperty;

public class LangIdentModelLoader implements ModelLoader {

    public static final String MODEL_TYPE = "lang_ident";
    public static final String FIELD = "field";
    public static final String TARGET_LANGUAGE_FIELD = "target_language_field";
    public static final String TARGET_PROBABILITY_FIELD = "target_probability_field";
    public static final String TARGET_TOP_PROBABILTIES_FIELD = "target_top_probabilities_field";
    public static final String TARGET_IS_RELIABLE_FIELD = "target_is_reliable_field";

    @Override
    public void consumeConfiguration(String processorTag, Map<String, Object> config) {
        readStringProperty(InferenceProcessor.TYPE, processorTag, config, FIELD, null);
        readStringProperty(InferenceProcessor.TYPE, processorTag, config, TARGET_LANGUAGE_FIELD, null);
        readStringProperty(InferenceProcessor.TYPE, processorTag, config, TARGET_PROBABILITY_FIELD, null);
        readStringProperty(InferenceProcessor.TYPE, processorTag, config, TARGET_TOP_PROBABILTIES_FIELD, null);
        readStringProperty(InferenceProcessor.TYPE, processorTag, config, TARGET_IS_RELIABLE_FIELD, null);
    }

    @Override
    public Model load(String modelId, String processorTag, boolean ignoreMissing, Map<String, Object> config) {

        String field = readStringProperty(InferenceProcessor.TYPE, processorTag, config, FIELD);
        if (field == null) {
            throw newConfigurationException(InferenceProcessor.TYPE, processorTag, FIELD, "field must be specified");
        }

        String targetLanguageField = readStringProperty(InferenceProcessor.TYPE, processorTag, config,
                TARGET_LANGUAGE_FIELD, null);
        String targetProbabilityField = readStringProperty(InferenceProcessor.TYPE, processorTag, config,
                TARGET_PROBABILITY_FIELD, null);
        String targetTopProbabilitiesField = readStringProperty(InferenceProcessor.TYPE, processorTag, config,
                TARGET_TOP_PROBABILTIES_FIELD, null);
        String targetIsReliableField = readStringProperty(InferenceProcessor.TYPE, processorTag, config,
                TARGET_IS_RELIABLE_FIELD, null);

        return new LangIdentModel(field, targetLanguageField, targetProbabilityField,
                targetTopProbabilitiesField, targetIsReliableField,
                ignoreMissing);
    }
}
