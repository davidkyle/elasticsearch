/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.ml.action;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.bucket.composite.CompositeValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.DateHistogramValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.composite.TermsValuesSourceBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.ml.action.DatafeedAutoAggAction;
import org.elasticsearch.xpack.core.ml.action.PreviewDatafeedAction;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.core.ml.job.config.Detector;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.security.SecurityContext;
import org.elasticsearch.xpack.ml.datafeed.AutoAggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

public class TransportDatafeedAutoAggAction extends HandledTransportAction<DatafeedAutoAggAction.Request, DatafeedAutoAggAction.Response> {

    private final Client client;

    @Inject
    public TransportDatafeedAutoAggAction(TransportService transportService, ActionFilters actionFilters, Client client) {
        super(DatafeedAutoAggAction.NAME, transportService, actionFilters, DatafeedAutoAggAction.Request::new);
        this.client = client;
    }

    @Override
    protected void doExecute(Task task, DatafeedAutoAggAction.Request request, ActionListener<DatafeedAutoAggAction.Response> listener) {

        var aggs = AutoAggregation.generateDatafeedAgg(request.getJobConfig().getAnalysisConfig(), request.getJobConfig().getTimeField());

        DatafeedConfig dfc = new DatafeedConfig.Builder(request.getDatafeedConfig()).setParsedAggregations(aggs).build();

        preview(
            dfc,
            request.getJobConfig(),
            ActionListener.wrap(
                previewResponse -> { listener.onResponse(new DatafeedAutoAggAction.Response(previewResponse.getPreview(), aggs)); },
                listener::onFailure
            )
        );

    }

    private void preview(DatafeedConfig datafeed, Job.Builder job, ActionListener<PreviewDatafeedAction.Response> listener) {
        executeAsyncWithOrigin(
            client,
            ML_ORIGIN,
            PreviewDatafeedAction.INSTANCE,
            new PreviewDatafeedAction.Request(datafeed, job),
            listener
        );
    }
}
