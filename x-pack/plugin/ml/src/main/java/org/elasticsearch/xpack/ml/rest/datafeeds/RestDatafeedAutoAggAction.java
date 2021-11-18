/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.ml.rest.datafeeds;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestToXContentListener;
import org.elasticsearch.xpack.core.ml.action.DatafeedAutoAggAction;
import org.elasticsearch.xpack.core.ml.action.PreviewDatafeedAction;

import java.io.IOException;
import java.util.List;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.xpack.ml.MachineLearning.BASE_PATH;

public class RestDatafeedAutoAggAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return List.of(
            Route.builder(POST, BASE_PATH + "datafeed/_auto_agg")
                .build()
        );
    }

    @Override
    public String getName() {
        return "ml_datafeed_auto_agg_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) throws IOException {
        DatafeedAutoAggAction.Request request = DatafeedAutoAggAction.Request.fromXContent(
            restRequest.contentOrSourceParamParser()
        );

        return channel -> client.execute(DatafeedAutoAggAction.INSTANCE, request, new RestToXContentListener<>(channel));
    }
}
