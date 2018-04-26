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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.persistent.AllocatedPersistentTask;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.xpack.core.ml.action.StartDatafeedAction;
import org.elasticsearch.xpack.ml.datafeed.DatafeedManager;

import java.util.Map;

public class DatafeedTask extends AllocatedPersistentTask implements StartDatafeedAction.DatafeedTaskMatcher {

    private final String datafeedId;
    private final long startTime;
    private final Long endTime;
    /* only pck protected for testing */
    volatile DatafeedManager datafeedManager;

    public DatafeedTask(long id, String type, String action, TaskId parentTaskId,
                        StartDatafeedPersistentTasksExecutor.DatafeedTaskParams params,
                        Map<String, String> headers) {
        super(id, type, action, "datafeed-" + params.getDatafeedId(), parentTaskId, headers);
        this.datafeedId = params.getDatafeedId();
        this.startTime = params.getStartTime();
        this.endTime = params.getEndTime();
    }

    public String getDatafeedId() {
        return datafeedId;
    }

    public long getDatafeedStartTime() {
        return startTime;
    }

    @Nullable
    public Long getEndTime() {
        return endTime;
    }

    public boolean isLookbackOnly() {
        return endTime != null;
    }

    @Override
    protected void onCancelled() {
        // If the persistent task framework wants us to stop then we should do so immediately and
        // we should wait for an existing datafeed import to realize we want it to stop.
        // Note that this only applied when task cancel is invoked and stop datafeed api doesn't use this.
        // Also stop datafeed api will obey the timeout.
        stop(getReasonCancelled(), TimeValue.ZERO);
    }

    public void stop(String reason, TimeValue timeout) {
        if (datafeedManager != null) {
            datafeedManager.stopDatafeed(this, reason, timeout);
        }
    }

    public void isolate() {
        if (datafeedManager != null) {
            datafeedManager.isolateDatafeed(getAllocationId());
        }
    }
}

