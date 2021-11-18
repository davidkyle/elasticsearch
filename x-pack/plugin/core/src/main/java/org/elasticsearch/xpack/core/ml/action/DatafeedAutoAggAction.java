/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ml.action;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionType;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class DatafeedAutoAggAction extends ActionType<DatafeedAutoAggAction.Response> {

    public static final DatafeedAutoAggAction INSTANCE = new DatafeedAutoAggAction();
    public static final String NAME = "cluster:admin/xpack/ml/datafeeds/auto_agg";

    private DatafeedAutoAggAction() {
        super(NAME, Response::new);
    }

    public static class Request extends ActionRequest implements ToXContentObject {

        private static final String BLANK_ID = "";

        public static final ParseField DATAFEED_CONFIG = new ParseField("datafeed_config");
        public static final ParseField JOB_CONFIG = new ParseField("job_config");

        private static final ObjectParser<Request.Builder, Void> PARSER = new ObjectParser<>(
            "datafeed_auto_agg_action",
            Request.Builder::new
        );
        static {
                PARSER.declareObject(Request.Builder::setDatafeedBuilder, DatafeedConfig.STRICT_PARSER, DATAFEED_CONFIG);
                PARSER.declareObject(Request.Builder::setJobBuilder, Job.STRICT_PARSER, JOB_CONFIG);
            }

        public static Request fromXContent(XContentParser parser) {
            Request.Builder builder = PARSER.apply(parser, null);
            return builder.build();
        }

        private final DatafeedConfig datafeedConfig;
        private final Job.Builder jobConfig;



        public Request(StreamInput in) throws IOException {
            super(in);
            datafeedConfig = in.readOptionalWriteable(DatafeedConfig::new);
            jobConfig = in.readOptionalWriteable(Job.Builder::new);
        }

        public Request(DatafeedConfig datafeedConfig, Job.Builder jobConfig) {
            this.datafeedConfig = ExceptionsHelper.requireNonNull(datafeedConfig, DATAFEED_CONFIG.getPreferredName());
            this.jobConfig = ExceptionsHelper.requireNonNull(jobConfig, JOB_CONFIG.getPreferredName());
        }

        public DatafeedConfig getDatafeedConfig() {
            return datafeedConfig;
        }

        public Job.Builder getJobConfig() {
            return jobConfig;
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeOptionalWriteable(datafeedConfig);
            out.writeOptionalWriteable(jobConfig);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            if (datafeedConfig != null) {
                builder.field(DATAFEED_CONFIG.getPreferredName(), datafeedConfig);
            }
            if (jobConfig != null) {
                builder.field(JOB_CONFIG.getPreferredName(), jobConfig);
            }
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(datafeedConfig, jobConfig);
        }

        @Override
            public boolean equals(Object obj) {
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                Request other = (Request) obj;
                    return Objects.equals(datafeedConfig, other.datafeedConfig)
                    && Objects.equals(jobConfig, other.jobConfig);
            }

        public static class Builder {
            private DatafeedConfig.Builder datafeedBuilder;
            private Job.Builder jobBuilder;

            public Request.Builder setDatafeedBuilder(DatafeedConfig.Builder datafeedBuilder) {
                this.datafeedBuilder = datafeedBuilder;
                return this;
            }

            public Request.Builder setJobBuilder(Job.Builder jobBuilder) {
                this.jobBuilder = jobBuilder;
                return this;
            }

            public Request build() {
                if (datafeedBuilder != null) {
                    datafeedBuilder.setId("preview_id");
                    if (datafeedBuilder.getJobId() == null && jobBuilder == null) {
                        throw new IllegalArgumentException("[datafeed_config.job_id] must be set or a [job_config] must be provided");
                    }
                    if (datafeedBuilder.getJobId() == null) {
                        datafeedBuilder.setJobId("preview_job_id");
                    }
                }
                if (jobBuilder != null) {
                    jobBuilder.setId("preview_job_id");
                    if (datafeedBuilder == null && jobBuilder.getDatafeedConfig() == null) {
                        throw new IllegalArgumentException(
                            "[datafeed_config] must be present when a [job_config.datafeed_config] is not present"
                        );
                    }
                    if (datafeedBuilder != null && jobBuilder.getDatafeedConfig() != null) {
                        throw new IllegalArgumentException(
                            "[datafeed_config] must not be present when a [job_config.datafeed_config] is present"
                        );
                    }
                    // If the datafeed_config has been provided via the jobBuilder, set it here for easier serialization and use
                    if (jobBuilder.getDatafeedConfig() != null) {
                        datafeedBuilder = jobBuilder.getDatafeedConfig().setJobId(jobBuilder.getId()).setId(jobBuilder.getId());
                    }
                }

                return new Request(datafeedBuilder.build(), jobBuilder);
            }
        }
    }

    public static class Response extends ActionResponse implements ToXContentObject {

        private final BytesReference preview;
        private final AggregatorFactories.Builder aggs;

        public Response(StreamInput in) throws IOException {
            super(in);
            preview = in.readBytesReference();
            aggs = in.readOptionalWriteable(AggregatorFactories.Builder::new);
        }

        public Response(BytesReference preview, AggregatorFactories.Builder aggs) {
            this.preview = preview;
            this.aggs = aggs;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeBytesReference(preview);
            out.writeOptionalWriteable(aggs);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field("aggs", aggs);
            builder.startArray("preview");
            try (InputStream stream = preview.streamInput()) {
                builder.rawValue(stream, XContentType.JSON);
            }
            builder.endArray();
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(preview, aggs);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Response other = (Response) obj;
            return Objects.equals(preview, other.preview) &&
                Objects.equals(aggs, other.aggs);
        }

        @Override
        public final String toString() {
            return Strings.toString(this);
        }
    }
}
