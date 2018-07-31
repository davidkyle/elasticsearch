/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.job;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.core.ml.job.config.Job;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

public class JobOrGroupResolver extends AbstractComponent {

    public static String ALL = "_all";

    private final Client client;

    public JobOrGroupResolver(Client client, Settings settings) {
        super(settings);
        this.client = client;
    }

    // TODO allowNoJobs - recreate that behaviour
    public void expandJobsIdsIndex(String expression, boolean allowNoJobs, ActionListener<Set<String>> listener) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(buildQuery(expression));
        sourceBuilder.sort(Job.ID.getPreferredName());
        String [] includes = new String[] {Job.GROUPS.getPreferredName(), Job.ID.getPreferredName()};
        sourceBuilder.fetchSource(includes, null);

        SearchRequest searchRequest = client.prepareSearch(AnomalyDetectorsIndex.jobConfigIndexName())
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setSource(sourceBuilder).request();

        executeAsyncWithOrigin(client.threadPool().getThreadContext(), ML_ORIGIN, searchRequest,
                ActionListener.<SearchResponse>wrap(
                        response -> {
//                            Map<String, List<String>> jobGroupsMap = new TreeMap<>();
                            Set<String> jobIds = new HashSet<>();
                            SearchHit[] hits = response.getHits().getHits();
                            for (SearchHit hit : hits) {
                                jobIds.add((String)hit.getSourceAsMap().get(Job.ID.getPreferredName()));
//                                        (List<String>)hit.getSourceAsMap().get(Job.GROUPS.getPreferredName())
//                                );
                            }

//                            GroupOrJobLookup lookup = new GroupOrJobLookup(jobGroupsMap);
//                            listener.onResponse(lookup.expandJobIds(expression, allowNoJobs));

                            listener.onResponse(jobIds);
                        },
                        listener::onFailure)
                , client::search);

    }

    private QueryBuilder buildQuery(String expression) {
        QueryBuilder jobQuery = new TermQueryBuilder(Job.JOB_TYPE.getPreferredName(), Job.ANOMALY_DETECTOR_JOB_TYPE);
        if (ALL.equals(expression) || Regex.isMatchAllPattern(expression)) {
            return jobQuery;
        }

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.filter(jobQuery);
        BoolQueryBuilder shouldQueries = new BoolQueryBuilder();

        List<String> terms = new ArrayList<>();
        String[] tokens = Strings.tokenizeToStringArray(expression, ",");
        for (String token : tokens) {
            if (Regex.isSimpleMatchPattern(token)) {
                shouldQueries.should(new WildcardQueryBuilder(Job.ID.getPreferredName(), token));
                shouldQueries.should(new WildcardQueryBuilder(Job.GROUPS.getPreferredName(), token));
            } else {
                terms.add(token);
            }
        }

        if (terms.isEmpty() == false) {
            shouldQueries.should(new TermsQueryBuilder(Job.ID.getPreferredName(), terms));
            shouldQueries.should(new TermsQueryBuilder(Job.GROUPS.getPreferredName(), terms));
        }

        if (shouldQueries.should().isEmpty() == false) {
            boolQueryBuilder.filter(shouldQueries);
        }

        return boolQueryBuilder;
    }
}
