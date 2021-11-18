/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.search.SearchModule;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedJobValidator;
import org.elasticsearch.xpack.core.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.core.ml.job.config.AnalysisConfigTests;
import org.elasticsearch.xpack.core.ml.job.config.Detector;
import org.elasticsearch.xpack.core.ml.job.config.DetectorFunction;
import org.elasticsearch.xpack.ml.MachineLearning;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

// ./gradlew ':x-pack:plugin:ml:test' --tests "org.elasticsearch.xpack.ml.datafeed.AutoAggregationTests"
public class AutoAggregationTests extends ESTestCase {

    public void testCannotAggWithCategoryField() {
        AnalysisConfig config = AnalysisConfigTests.createRandomizedCategorizationConfig().build();
        assertThat(AutoAggregation.generateDatafeedAgg(config, "any"), nullValue());
    }

    @Override
    protected NamedXContentRegistry xContentRegistry() {
        return new NamedXContentRegistry(
            new SearchModule(Settings.EMPTY, List.of(new MachineLearning(Settings.EMPTY))).getNamedXContents()
        );
    }

    @Override
    protected NamedWriteableRegistry writableRegistry() {
        return new NamedWriteableRegistry(
            new SearchModule(Settings.EMPTY, List.of(new MachineLearning(Settings.EMPTY))).getNamedWriteables()
        );
    }

    public void testGenerateAggs() {
        final String AGGREGATED_FIELD =  "metric_field";
        Detector detector = new Detector.Builder(DetectorFunction.HIGH_AVG, AGGREGATED_FIELD).setByFieldName("by-field").build();
        var analysisConfig = new AnalysisConfig.Builder(List.of(detector)).setInfluencers(List.of("inf-a", "inf-b"))
            .setSummaryCountFieldName(AGGREGATED_FIELD).build();

        var aggs = AutoAggregation.generateDatafeedAgg(analysisConfig, "time_field");
        assertThat(aggs.getAggregatorFactories(), hasSize(greaterThan(0)));
        var firstAgg = aggs.getAggregatorFactories().iterator().next();
        if (firstAgg instanceof CompositeAggregationBuilder) {
            var composite = (CompositeAggregationBuilder)firstAgg;
            assertThat(composite.sources().get(0), instanceOf(DateHistogramValuesSourceBuilder.class));
        } else {
            assertThat(firstAgg, instanceOf(DateHistogramAggregationBuilder.class));
        }

        var df = new DatafeedConfig.Builder("a", "a")
            .setParsedAggregations(aggs)
            .setIndices(Collections.singletonList("index-1"))
            .build();
        DatafeedConfig.validateAggregations(aggs);
        DatafeedJobValidator.validate(df, analysisConfig, "time_field", xContentRegistry());
    }
}
