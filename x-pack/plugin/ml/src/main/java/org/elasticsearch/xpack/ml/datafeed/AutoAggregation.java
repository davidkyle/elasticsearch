/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.datafeed;

import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.xpack.core.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.core.ml.job.config.Detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class AutoAggregation {

    private AutoAggregation() {}

    public static AggregatorFactories.Builder generateDatafeedAgg(AnalysisConfig analysisConfig, String timeField) {
        if (analysisConfig.getDetectors().size() > 1 || analysisConfig.getCategorizationFieldName() != null) {
            return null;
        }

        Detector detector = analysisConfig.getDetectors().get(0);

        if (detector.getFunction().isAggregatable() == false) {
            return null;
        }

        Set<String> terms = Stream.concat(analysisConfig.getInfluencers().stream(), detector.getByOverPartitionTerms().stream())
            .collect(Collectors.toUnmodifiableSet());

        TimeValue histogramInterval = detector.getFunction().isCrossBucketSampling()
            ? TimeValue.timeValueMillis(analysisConfig.getBucketSpan().millis() / 10)
            : analysisConfig.getBucketSpan();

        AggregationBuilder aggs;

        if (terms.isEmpty()) {
            aggs = AggregationBuilders.dateHistogram("buckets")
                .field(timeField)
                .fixedInterval(new DateHistogramInterval(histogramInterval.getStringRep()));
        } else {
            List<CompositeValuesSourceBuilder<?>> sources = new ArrayList<>();
            sources.add(
                new DateHistogramValuesSourceBuilder(timeField).field(timeField)
                    .fixedInterval(new DateHistogramInterval(histogramInterval.getStringRep()))
            );
            terms.forEach(term -> sources.add(new TermsValuesSourceBuilder(term).field(term)));

            aggs = AggregationBuilders.composite("buckets", sources).size(1000);
        }

        aggs.subAggregation(AggregationBuilders.max(timeField).field(timeField));

        if (detector.getFunction().isCount() == false) {
            aggs.subAggregation(detector.getFunction().buildAgg(detector.getFieldName()));
        }

        return AggregatorFactories.builder().addAggregator(aggs);
    }
}
