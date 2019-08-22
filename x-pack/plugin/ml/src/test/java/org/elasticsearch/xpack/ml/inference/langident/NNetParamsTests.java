/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.test.ESTestCase;

import java.io.FileNotFoundException;

public class NNetParamsTests extends ESTestCase {

    public void testInitFromTextFiles() throws FileNotFoundException {
        NNetParams params = new NNetParams();

        // Verify array sizes (compared to cld3)
        assertEquals(1000, params.kEmbeddingsQuantScales0.length);
        assertEquals(5000, params.kEmbeddingsQuantScales1.length);
        assertEquals(12, params.kEmbeddingsQuantScales2.length);
        assertEquals(103, params.kEmbeddingsQuantScales3.length);
        assertEquals(5000, params.kEmbeddingsQuantScales4.length);
        assertEquals(100, params.kEmbeddingsQuantScales5.length);
        assertEquals(16000, params.kEmbeddingsWeights0.length);
        assertEquals(80000, params.kEmbeddingsWeights1.length);
        assertEquals(96, params.kEmbeddingsWeights2.length);
        assertEquals(824, params.kEmbeddingsWeights3.length);
        assertEquals(80000, params.kEmbeddingsWeights4.length);
        assertEquals(1600, params.kEmbeddingsWeights5.length);
        assertEquals(208, params.kHiddenBiasWeights0.length);
        assertEquals(16640, params.kHiddenWeights0.length);
        assertEquals(109, params.kSoftmaxBiasWeights0.length);
        assertEquals(22672, params.kSoftmaxWeights0.length);
    }
}
