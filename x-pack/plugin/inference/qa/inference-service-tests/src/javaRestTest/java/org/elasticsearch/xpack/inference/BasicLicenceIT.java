/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.inference;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.inference.TaskType;
import org.elasticsearch.test.cluster.ElasticsearchCluster;
import org.elasticsearch.test.cluster.local.distribution.DistributionType;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.junit.ClassRule;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.xpack.inference.InferenceBaseRestTest.mockSparseServiceModelConfig;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

public class BasicLicenceIT extends ESRestTestCase {
    @ClassRule
    public static ElasticsearchCluster cluster = ElasticsearchCluster.local()
        .distribution(DistributionType.DEFAULT)
        .setting("xpack.license.self_generated.type", "basic") // create cluster with a basic licence
        .setting("xpack.security.enabled", "true")
        .user("x_pack_rest_user", "x-pack-test-password")
        .plugin("inference-service-test")
        .build();

    @Override
    protected String getTestRestCluster() {
        return cluster.getHttpAddresses();
    }

    @Override
    protected Settings restClientSettings() {
        String token = basicAuthHeaderValue("x_pack_rest_user", new SecureString("x-pack-test-password".toCharArray()));
        return Settings.builder()
            .put(ThreadContext.PREFIX + ".Authorization", token)
            .build();
    }

    public void testInferenceWithBasicLicense() throws Exception {
        putModel("foo", mockSparseServiceModelConfig(null, true), TaskType.SPARSE_EMBEDDING);
    }

    protected Map<String, Object> putModel(String modelId, String modelConfig, TaskType taskType) throws IOException {
        String endpoint = Strings.format("_inference/%s/%s?error_trace", taskType, modelId);
        return putRequest(endpoint, modelConfig);
    }

    Map<String, Object> putRequest(String endpoint, String body) throws IOException {
        var request = new Request("PUT", endpoint);
        request.setJsonEntity(body);
        var response = client().performRequest(request);
        assertOkOrCreated(response);
        return entityAsMap(response);
    }

    protected static void assertOkOrCreated(Response response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        // Once EntityUtils.toString(entity) is called the entity cannot be reused.
        // Avoid that call with check here.
        if (statusCode == 200 || statusCode == 201) {
            return;
        }

        String responseStr = EntityUtils.toString(response.getEntity());
        assertThat(responseStr, response.getStatusLine().getStatusCode(), anyOf(equalTo(200), equalTo(201)));
    }
}
