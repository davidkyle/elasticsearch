/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.test.ESTestCase;

import java.io.FileNotFoundException;

public class EmbeddingNetworkTests extends ESTestCase {
    public void testHiddenMatrix() throws FileNotFoundException {
        EmbeddingNetwork network = new EmbeddingNetwork(new NNetParams());

        /*
        ArrayList<EmbeddingNetwork.Matrix> hiddenWeights = network.getHiddenWeights();
        for (int k = 0; k < hiddenWeights.size(); ++k) {
            EmbeddingNetwork.Matrix feature = hiddenWeights.get(k);
            for (int i = 0; i < feature.size(); ++i) {
                EmbeddingNetwork.VectorWrapper row = feature.get(i);
                for (int j = 0; j < row.size(); ++j) {
                    System.out.println(k + "(" + i + "," + j + ")=" + row.get(j));
                }
            }
        }

        ArrayList<EmbeddingNetwork.VectorWrapper> hiddenBias = network.getHiddenBias();
        for (int i = 0; i < hiddenBias.size(); ++i) {
            EmbeddingNetwork.VectorWrapper row = hiddenBias.get(i);
            for (int j = 0; j < row.size(); ++j) {
                System.out.println("(" + i + "," + j + ")=" + row.get(j));
            }
        }
        */
    }
}
