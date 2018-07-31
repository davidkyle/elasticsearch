/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.analysis.AnalysisRegistry;
import org.elasticsearch.xpack.core.ml.MachineLearningField;
import org.elasticsearch.xpack.core.ml.action.PutJobAction;
import org.elasticsearch.xpack.core.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.core.ml.job.config.DataDescription;
import org.elasticsearch.xpack.core.ml.job.config.Detector;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.config.JobTests;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.ml.MlSingleNodeTestCase;
import org.elasticsearch.xpack.ml.job.JobManager;
import org.elasticsearch.xpack.ml.job.JobOrGroupResolver;
import org.elasticsearch.xpack.ml.job.UpdateJobProcessNotifier;
import org.elasticsearch.xpack.ml.job.categorization.CategorizationAnalyzerTests;
import org.elasticsearch.xpack.ml.job.persistence.JobProvider;
import org.elasticsearch.xpack.ml.notifications.Auditor;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobOrGroupResolverIT extends MlSingleNodeTestCase {

    private JobManager jobManager;
    private AnalysisRegistry analysisRegistry;
    private JobOrGroupResolver jobOrGroupResolver;

    @Before
    public void createComponents() throws Exception {
        JobProvider jobProvider = new JobProvider(client(), Settings.EMPTY);
        Auditor auditor = new Auditor(client(), "test_node");

        ClusterService clusterService = mock(ClusterService.class);
        ClusterSettings clusterSettings = new ClusterSettings(this.nodeSettings(),
                Collections.singleton(MachineLearningField.MAX_MODEL_MEMORY_LIMIT));
        when(clusterService.getClusterSettings()).thenReturn(clusterSettings);
        UpdateJobProcessNotifier updateJobProcessNotifier = mock(UpdateJobProcessNotifier.class);

        jobManager = new JobManager(node().getEnvironment(), Settings.EMPTY, jobProvider, clusterService, auditor,
                this.client(), updateJobProcessNotifier);

        jobOrGroupResolver = new JobOrGroupResolver(client(), Settings.EMPTY);
        analysisRegistry = CategorizationAnalyzerTests.buildTestAnalysisRegistry(node().getEnvironment());
        waitForMlTemplates();
    }

    public void testGroupsAndJobIds() throws IOException, InterruptedException {
        putJob(createJob("tom", null));
        putJob(createJob("dick", null));
        putJob(createJob("harry", Collections.singletonList("harry-group")));
        putJob(createJob("harry-jnr", Collections.singletonList("harry-group")));

        client().admin().indices().prepareRefresh(AnomalyDetectorsIndex.jobConfigIndexName()).get();

        Set<String> expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("_all", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("tom", "dick", "harry", "harry-jnr")), expandedIds);

        expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("*", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("tom", "dick", "harry", "harry-jnr")), expandedIds);

        expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("tom,harry", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("tom", "harry")), expandedIds);

        expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("harry-group", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("harry", "harry-jnr")), expandedIds);
    }

    public void testWildCardExpansion() throws IOException, InterruptedException {

        putJob(createJob("foo-1", null));
        putJob(createJob("foo-2", null));
        putJob(createJob("bar-1", Collections.singletonList("bar")));
        putJob(createJob("bar-2", Collections.singletonList("bar")));
        putJob(createJob("nbar", Collections.singletonList("bar")));

        client().admin().indices().prepareRefresh(AnomalyDetectorsIndex.jobConfigIndexName()).get();

        Set<String> expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("foo*", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("foo-1", "foo-2")), expandedIds);

        expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("*-1", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("bar-1", "foo-1")), expandedIds);

        expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("bar*", false, actionListener));
        assertEquals(new TreeSet<>(Arrays.asList("bar-1", "bar-2", "nbar")), expandedIds);

        expandedIds = blockingCall(actionListener ->
                jobOrGroupResolver.expandJobsIdsIndex("b*r-1", false, actionListener));
        assertEquals(new TreeSet<>(Collections.singletonList("bar-1")), expandedIds);
    }

    private Job.Builder createJob(String jobId, List<String> jobGroups) {
        Detector.Builder d1 = new Detector.Builder("info_content", "domain");
        d1.setOverFieldName("client");
        AnalysisConfig.Builder ac = new AnalysisConfig.Builder(Collections.singletonList(d1.build()));

        Job.Builder builder = new Job.Builder();
        builder.setId(jobId);
        builder.setAnalysisConfig(ac);
        builder.setDataDescription(new DataDescription.Builder());
        if (jobGroups != null && jobGroups.isEmpty() == false) {
            builder.setGroups(jobGroups);
        }
        return builder;
    }

    private void putJob(Job.Builder job) throws InterruptedException, IOException {
        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

        this.<PutJobAction.Response>blockingCall(actionListener -> {
            try {
                jobManager.putJob(new PutJobAction.Request(job), analysisRegistry, createEmptyClusterState(), actionListener);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        });
    }

    private <T> T blockingCall(Consumer<ActionListener<T>> function) throws InterruptedException {

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();
        AtomicReference<T> ref = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        ActionListener<T> listener = ActionListener.wrap(
                r -> {
                    ref.set(r);
                    latch.countDown();
                },
                e -> {
                    exceptionHolder.set(e);
                    latch.countDown();
                }
        );

        function.accept(listener);

        latch.await();
        assertNull(exceptionHolder.get());
        return ref.get();
    }

    private ClusterState createEmptyClusterState() {
        ClusterState.Builder builder = ClusterState.builder(new ClusterName("_name"));
        builder.metaData(MetaData.builder());
        return builder.build();
    }
}
