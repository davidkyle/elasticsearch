/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2018 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.datafeed.persistent.task;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.persistent.AllocatedPersistentTask;
import org.elasticsearch.persistent.PersistentTaskParams;
import org.elasticsearch.persistent.PersistentTasksCustomMetaData;
import org.elasticsearch.persistent.PersistentTasksExecutor;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.xpack.core.ml.MLMetadataField;
import org.elasticsearch.xpack.core.ml.MlMetadata;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedJobValidator;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.config.JobState;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;
import org.elasticsearch.xpack.ml.MachineLearning;
import org.elasticsearch.xpack.ml.datafeed.DatafeedManager;
import org.elasticsearch.xpack.ml.datafeed.DatafeedNodeSelector;

import java.io.IOException;
import java.util.Map;

public class StartDatafeedPersistentTasksExecutor extends PersistentTasksExecutor<StartDatafeedPersistentTasksExecutor.DatafeedTaskParams> {

    public static final String TASK_NAME = "xpack/ml/datafeed";

    private final DatafeedManager datafeedManager;
    private final IndexNameExpressionResolver resolver;

    public StartDatafeedPersistentTasksExecutor(Settings settings, DatafeedManager datafeedManager) {
        super(settings, TASK_NAME, MachineLearning.UTILITY_THREAD_POOL_NAME);
        this.datafeedManager = datafeedManager;
        this.resolver = new IndexNameExpressionResolver(settings);
    }

    @Override
    public PersistentTasksCustomMetaData.Assignment getAssignment(DatafeedTaskParams params,
                                                                  ClusterState clusterState) {
        return new DatafeedNodeSelector(clusterState, resolver, params.getDatafeedId()).selectNode();
    }

    @Override
    public void validate(DatafeedTaskParams params, ClusterState clusterState) {
        MlMetadata mlMetadata = clusterState.metaData().custom(MLMetadataField.TYPE);
        PersistentTasksCustomMetaData tasks = clusterState.getMetaData().custom(PersistentTasksCustomMetaData.TYPE);

        DatafeedConfig datafeed = (mlMetadata == null) ? null : mlMetadata.getDatafeed(params.getDatafeedId());
        if (datafeed == null) {
            throw ExceptionsHelper.missingDatafeedException(params.getDatafeedId());
        }
        Job job = mlMetadata.getJobs().get(datafeed.getJobId());
        if (job == null) {
            throw ExceptionsHelper.missingJobException(datafeed.getJobId());
        }

        validateConfig(job, datafeed, tasks);
        new DatafeedNodeSelector(clusterState, resolver, params.getDatafeedId()).checkDatafeedTaskCanBeCreated();
    }

    @Override
    protected void nodeOperation(AllocatedPersistentTask allocatedPersistentTask, DatafeedTaskParams params,
                                 Task.Status status) {
        DatafeedTask datafeedTask = (DatafeedTask) allocatedPersistentTask;
        datafeedTask.datafeedManager = datafeedManager;
        datafeedManager.run(datafeedTask, params.getJob(), params.getDatafeed(),
                (error) -> {
                    if (error != null) {
                        datafeedTask.markAsFailed(error);
                    } else {
                        datafeedTask.markAsCompleted();
                    }
                });
    }

    @Override
    protected AllocatedPersistentTask createTask(
            long id, String type, String action, TaskId parentTaskId,
            PersistentTasksCustomMetaData.PersistentTask<DatafeedTaskParams> persistentTask,
            Map<String, String> headers) {
        return new DatafeedTask(id, type, action, parentTaskId, persistentTask.getParams(), headers);
    }

    private void validateConfig(Job job, DatafeedConfig datafeed, PersistentTasksCustomMetaData tasks) {

        DatafeedJobValidator.validate(datafeed, job);
        JobState jobState = MlMetadata.getJobState(datafeed.getJobId(), tasks);
        if (jobState.isAnyOf(JobState.OPENING, JobState.OPENED) == false) {
            throw ExceptionsHelper.conflictStatusException("cannot start datafeed [" + datafeed.getId() + "] " +
                    "because job [" + job.getId() + "] is " + jobState);
        }
    }

    public static class DatafeedTaskParams implements PersistentTaskParams {
        private String datafeedId;
        private Job job;
        private DatafeedConfig datafeed;
        private long startTime;
        private Long endTime;
        private TimeValue timeout = TimeValue.timeValueSeconds(20);


        public String getDatafeedId() {
            return datafeedId;
        }

        public Job getJob() {
            return job;
        }

        public DatafeedConfig getDatafeed() {
            return datafeed;
        }

        public long getStartTime() {
            return startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        public TimeValue getTimeout() {
            return timeout;
        }

        @Override
        public String getWriteableName() {
            return TASK_NAME;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {

        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            return null;
        }
    }
}
