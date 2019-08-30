/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.AsyncModel;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.threadpool.TestThreadPool;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AsyncModelLoaderTests extends ESTestCase {

    private static final String PROCESSOR_TAG = "model_processor";

    private TestThreadPool threadPool;
    private int sequentialDocumentId = 0;

    private class ConcreteLoader extends AsyncModelLoader<ConcreteModel> {

        ConcreteLoader(Client client, Function<Boolean, ConcreteModel> modelSupplier) {
            super(client, modelSupplier);
        }
    }

    @Before
    public void setUp() {
        threadPool = new TestThreadPool("AsyncModelLoaderTests");
    }

    @After
    public void tearDown() {
        terminate(threadPool);
    }

    private class ConcreteModel extends AsyncModel {

        private List<Tuple<IngestDocument, BiConsumer<IngestDocument, Exception>>> requests = new ArrayList<>();
        private AtomicLong createModelCount = new AtomicLong();

        private ConcreteModel(boolean ignoreMissing) {
            super(ignoreMissing);
        }

        @Override
        protected void inferPrivate(IngestDocument document, BiConsumer<IngestDocument, Exception> handler) {
            requests.add(new Tuple<>(document, handler));
        }

        @Override
        protected void createModel(GetResponse getResponse) {
            createModelCount.incrementAndGet();
        }
    }

    public void testSlowLoad() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);

        Client client = mockLatchedClient(mock(GetResponse.class), latch);
        ConcreteLoader loader = new ConcreteLoader(client, ConcreteModel::new);

        ConcreteModel loadingModel = loader.load("foo", PROCESSOR_TAG, false, config());

        Handler handler = new Handler();

        int preloadedCount = randomIntBetween(0, 10);
        for (int i=0; i<preloadedCount; i++) {
            loadingModel.infer(ingestDocument(), handler::handle);
        }

        // handler has not been called while loading
        assertThat(handler.docs, hasSize(0));
        assertThat(handler.errors, hasSize(0));

        latch.countDown();
        for (int i=0; i<10; i++) {
            loadingModel.infer(ingestDocument(), handler::handle);
        }

        assertBusy(() -> {
            assertThat(handler.docs, hasSize(preloadedCount + 10));
        });
    }

    @SuppressWarnings("unchecked")
    private Client mockClient(GetResponse response, TimeValue delay) {

        Client client = mock(Client.class);
        GetRequestBuilder requestBuilder = mock(GetRequestBuilder.class);
        when(client.prepareGet(any(), any(), any())).thenReturn(requestBuilder);

        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = (ActionListener<GetResponse>) invocation.getArguments()[0];
            threadPool.schedule(() -> listener.onResponse(response), delay,  "same");
            return null;
        }).when(requestBuilder).execute(any());

        return client;
    }

    @SuppressWarnings("unchecked")
    private Client mockLatchedClient(GetResponse response, CountDownLatch latch) {

        Client client = mock(Client.class);
        GetRequestBuilder requestBuilder = mock(GetRequestBuilder.class);
        when(client.prepareGet(any(), any(), any())).thenReturn(requestBuilder);

        doAnswer(invocation -> {
            ActionListener<GetResponse> listener = (ActionListener<GetResponse>) invocation.getArguments()[0];
                threadPool.generic().submit(() -> {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        listener.onFailure(e);
                    }
                    listener.onResponse(response);
                });

            return null;
        }).when(requestBuilder).execute(any());

        return client;
    }

    private IngestDocument ingestDocument() {
        return new IngestDocument("index", "type", "id_" + sequentialDocumentId++, "route",
                0L, VersionType.INTERNAL, Collections.emptyMap());
    }

    private Map<String, Object> config() {
        Map<String, Object> mutableMap = new HashMap<>();
        mutableMap.put(AsyncModelLoader.INDEX, "some_index");
        return mutableMap;
    }

    private class Handler {
        List<IngestDocument> docs = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        void handle(IngestDocument doc, Exception e) {
            if (doc != null) {
                docs.add(doc);
            } else {
                errors.add(e);
            }
        }
    }
}
