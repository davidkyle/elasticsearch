/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.dataframe;

import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksRequest;
import org.elasticsearch.action.admin.cluster.node.tasks.list.ListTasksResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.license.LicenseService;
import org.elasticsearch.persistent.PersistentTasksClusterService;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.tasks.TaskInfo;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.MockHttpTransport;
import org.elasticsearch.xpack.core.XPackSettings;
import org.elasticsearch.xpack.core.dataframe.action.PutDataFrameTransformAction;
import org.elasticsearch.xpack.core.dataframe.action.StartDataFrameTransformAction;
import org.elasticsearch.xpack.core.dataframe.transforms.DataFrameTransformConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.DestConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.QueryConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.SourceConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.pivot.AggregationConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.pivot.GroupConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.pivot.PivotConfig;
import org.elasticsearch.xpack.core.dataframe.transforms.pivot.SingleGroupSource;
import org.elasticsearch.xpack.core.dataframe.transforms.pivot.TermsGroupSource;
import org.elasticsearch.xpack.core.ml.action.GetDatafeedsStatsAction;
import org.elasticsearch.xpack.core.template.TemplateUtils;
import org.elasticsearch.xpack.dataframe.persistence.DataFrameInternalIndex;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DataFrameFailOverIT extends ESIntegTestCase {

    @Override
    protected boolean ignoreExternalCluster() {
        return true;
    }

    @Before
    public void waitForTemplates() throws Exception {
        assertBusy(() -> {
            ClusterState state = client().admin().cluster().prepareState().get().getState();
            assertTrue("Timed out waiting for the data frame templates to be installed",
                    TemplateUtils.checkTemplateExistsAndVersionIsGTECurrentVersion(DataFrameInternalIndex.INDEX_TEMPLATE_NAME, state));
        });
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder settings = Settings.builder().put(super.nodeSettings(nodeOrdinal));
        settings.put(XPackSettings.MACHINE_LEARNING_ENABLED.getKey(), true);
        settings.put(XPackSettings.SECURITY_ENABLED.getKey(), false);
        settings.put(LicenseService.SELF_GENERATED_LICENSE_TYPE.getKey(), "trial");
        settings.put(XPackSettings.MONITORING_ENABLED.getKey(), false);
        settings.put(XPackSettings.GRAPH_ENABLED.getKey(), false);
        return settings.build();
    }


    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(LocalStateDataFrame.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> getMockPlugins() {
        return Arrays.asList(TestSeedPlugin.class, MockHttpTransport.TestPlugin.class);
    }


    public void testFailOver() throws Exception {
        internalCluster().ensureAtLeastNumDataNodes(3);
//        ensureStableCluster(3);


        // For the sake of a speedy test set the re-assign task interval to a short period
        PersistentTasksClusterService persistentTasksClusterService =
                internalCluster().getInstance(PersistentTasksClusterService.class, internalCluster().getMasterName());
        persistentTasksClusterService.setRecheckInterval(TimeValue.timeValueMillis(200));

        String indexName = "df-source";
        String transformId = "failover-test";
        indexSomeData(indexName);
        createTransform(transformId, indexName);
        startTransform(transformId);

        String dfNode = nodeName(transformId);
        assertNotNull("Node where the task is running not found", dfNode);
        logger.info("Got node " + dfNode);

        internalCluster().stopRandomNode(settings -> dfNode.equals(settings.get("node.name")));




    }

    private void createTransform(String id, String sourceIndex) throws IOException {
        SourceConfig sourceConfig = new SourceConfig(new String[]{sourceIndex}, QueryConfig.matchAll());

        String targetFieldName = "reviewer";
        SingleGroupSource groupSource = new TermsGroupSource("user_id");
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(targetFieldName, Collections.singletonMap(groupSource.getType().value(), getSourceAsMap(groupSource)));
        GroupConfig groupConfig = new GroupConfig(source, Collections.singletonMap(targetFieldName, groupSource));

        AggregatorFactories.Builder aggBuilder = new AggregatorFactories.Builder();
        aggBuilder.addAggregator(
                AggregationBuilders.avg("avg_rating").field("stars"));
        Map<String, Object> aggSource = getSourceAsMap(aggBuilder);
        AggregationConfig aggConfig = new AggregationConfig(aggSource, aggBuilder);

        PivotConfig pivotConfig = new PivotConfig(groupConfig, aggConfig, null);

        DataFrameTransformConfig config = new DataFrameTransformConfig(id, sourceConfig, new DestConfig("pivot-destination"),
                Collections.emptyMap(), pivotConfig, null);
        PutDataFrameTransformAction.Request putRequest = new PutDataFrameTransformAction.Request(config);

        AcknowledgedResponse acked = client().execute(PutDataFrameTransformAction.INSTANCE, putRequest).actionGet();
        assertTrue(acked.isAcknowledged());
    }

    private void startTransform(String transformId) {
        StartDataFrameTransformAction.Response response =
                client().execute(StartDataFrameTransformAction.INSTANCE,  new StartDataFrameTransformAction.Request(transformId, false))
                        .actionGet();
        assertTrue(response.isAcknowledged());
    }

    private String nodeName(String transformId) {
        ListTasksResponse listTasksResponse = client().admin().cluster().listTasks(new ListTasksRequest().setDetailed(true)).actionGet();

        logger.info("TASKS ===");
        logger.info(listTasksResponse);


        Optional<TaskInfo> dfTask = listTasksResponse.getTasks().stream().filter(task -> task.getType().equals("persistent"))
                .filter(task -> task.getDescription().equals("data_frame_" + transformId)).findFirst();
        if (dfTask.isPresent()) {
            return dfTask.get().getTaskId().getNodeId();
        } else {
            return null;
        }
    }

    private void indexSomeData(String indexName) {
        client().admin().indices().prepareCreate(indexName)
                .addMapping("_doc", "time", "type=date", "user", "type=keyword").get();

//        long now = System.currentTimeMillis();
//        long oneWeekAgo = now - 604800000;
//
//        indexDocs(indexName, 100, oneWeekAgo, now);
    }

    private void indexDocs(String index, long numDocs, long start, long end) {
        int maxDelta = (int) (end - start - 1);
        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk();
        for (int i = 0; i < numDocs; i++) {
            IndexRequest indexRequest = new IndexRequest(index, "_doc");
            long timestamp = start + randomIntBetween(0, maxDelta);
            assert timestamp >= start && timestamp < end;
            indexRequest.source("time", timestamp, "user", "foo");
            bulkRequestBuilder.add(indexRequest);
        }
        BulkResponse bulkResponse = bulkRequestBuilder
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .get();

        assertFalse(bulkResponse.hasFailures());
    }

    private static Map<String, Object> getSourceAsMap(ToXContent toXContent) {
        try (XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()) {
            XContentBuilder content = toXContent.toXContent(xContentBuilder, ToXContent.EMPTY_PARAMS);
            return XContentHelper.convertToMap(BytesReference.bytes(content), true, XContentType.JSON).v2();
        } catch (IOException e) {
            // should not happen
            fail("failed to create random single group source");
        }
        return null;
    }
}
