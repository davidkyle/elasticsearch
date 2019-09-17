/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.apache.commons.codec.Charsets;

import java.io.FileNotFoundException;
import java.util.Scanner;

public class NNetParams extends EmbeddingNetworkParams {
    private static final int EMBEDDINGS_SIZE = 6;
    private static final int EMBEDDING_NUM_FEATURES_SIZE = 6;
    private static final int EMBEDDING_DIM_SIZE = 6;
    private static final int HIDDEN_SIZE = 1;
    private static final int HIDDEN_BIAS_SIZE = 1;
    private static final int SOFTMAX_SIZE = 1;
    private static final int SOFTMAX_BIAS_SIZE = 1;
    private static final int CONCAT_LAYER_SIZE = 80;
    private static final int CONCAT_OFFSET_SIZE = 6;

    public static final int kEmbeddingsNumRows[] = {1000, 5000, 12, 103, 5000, 100};
    public static final int kEmbeddingsNumCols[] = {16, 16, 8, 8, 16, 16};
    public static final int kHiddenNumRows[] = {80};
    public static final int kHiddenNumCols[] = {208};
    public static final int kHiddenBiasNumRows[] = {208};
    public static final int kHiddenBiasNumCols[] = {1};
    public static final int kSoftmaxNumRows[] = {208};
    public static final int kSoftmaxNumCols[] = {109};
    public static final int kSoftmaxBiasNumRows[] = {109};
    public static final int kSoftmaxBiasNumCols[] = {1};
    public static final int kEmbeddingDimValues[] = {16, 16, 8, 8, 16, 16};
    public static final int kEmbeddingNumFeaturesValues[] = {1, 1, 1, 1, 1, 1};
    public static final int kEmbeddingFeaturesDomainSizeValues[] = {1000, 5000, 12, 103, 5000, 100};
    public static final int kConcatOffsetValues[] = {0, 16, 32, 40, 48, 64};
    public final short kEmbeddingsQuantScales0[];
    public final char kEmbeddingsWeights0[];
    public final short kEmbeddingsQuantScales1[];
    public final char kEmbeddingsWeights1[];
    public final short kEmbeddingsQuantScales2[];
    public final char kEmbeddingsWeights2[];
    public final short kEmbeddingsQuantScales3[];
    public final char kEmbeddingsWeights3[];
    public final short kEmbeddingsQuantScales4[];
    public final char kEmbeddingsWeights4[];
    public final short kEmbeddingsQuantScales5[];
    public final char kEmbeddingsWeights5[];
    public final float kHiddenWeights0[];
    public final float kHiddenBiasWeights0[];
    public final float kSoftmaxWeights0[];
    public final float kSoftmaxBiasWeights0[];

    private final short embeddingsQuantScales[][];
    private final char embeddingsWeights[][];
    private final float hiddenWeights[][];
    private final float hiddenBiasWeights[][];
    private final float softmaxWeights[][];
    private final float softmaxBiasWeights[][];

    // TODO resolve handling of this exception
    public NNetParams() throws FileNotFoundException {
        kEmbeddingsQuantScales0 = readFileToShortArray("/kEmbeddingsQuantScales0");
        kEmbeddingsQuantScales1 = readFileToShortArray("/kEmbeddingsQuantScales1");
        kEmbeddingsQuantScales2 = readFileToShortArray("/kEmbeddingsQuantScales2");
        kEmbeddingsQuantScales3 = readFileToShortArray("/kEmbeddingsQuantScales3");
        kEmbeddingsQuantScales4 = readFileToShortArray("/kEmbeddingsQuantScales4");
        kEmbeddingsQuantScales5 = readFileToShortArray("/kEmbeddingsQuantScales5");

        kEmbeddingsWeights0 = readFileToCharArray("/kEmbeddingsWeights0");
        kEmbeddingsWeights1 = readFileToCharArray("/kEmbeddingsWeights1");
        kEmbeddingsWeights2 = readFileToCharArray("/kEmbeddingsWeights2");
        kEmbeddingsWeights3 = readFileToCharArray("/kEmbeddingsWeights3");
        kEmbeddingsWeights4 = readFileToCharArray("/kEmbeddingsWeights4");
        kEmbeddingsWeights5 = readFileToCharArray("/kEmbeddingsWeights5");

        kHiddenWeights0 = readFileToFloatArray("/kHiddenWeights0");
        kHiddenBiasWeights0 = readFileToFloatArray("/kHiddenBiasWeights0");

        kSoftmaxWeights0 = readFileToFloatArray("/kSoftmaxWeights0");
        kSoftmaxBiasWeights0 = readFileToFloatArray("/kSoftmaxBiasWeights0");

        embeddingsQuantScales = new short[][]{
            kEmbeddingsQuantScales0, kEmbeddingsQuantScales1,
            kEmbeddingsQuantScales2, kEmbeddingsQuantScales3,
            kEmbeddingsQuantScales4, kEmbeddingsQuantScales5
        };
        embeddingsWeights = new char[][]{
            kEmbeddingsWeights0, kEmbeddingsWeights1,
            kEmbeddingsWeights2, kEmbeddingsWeights3,
            kEmbeddingsWeights4, kEmbeddingsWeights5
        };
        hiddenWeights = new float[][]{
            kHiddenWeights0
        };
        hiddenBiasWeights = new float[][]{
            kHiddenBiasWeights0
        };
        softmaxWeights = new float[][]{
            kSoftmaxWeights0
        };
        softmaxBiasWeights = new float[][]{
            kSoftmaxBiasWeights0
        };
    }

    private static short[] readFileToShortArray(String path) throws FileNotFoundException {
        Scanner s = new Scanner(NNetParams.class.getResourceAsStream(path), Charsets.UTF_8);
        // Read first entry (array size) as integer
        short[] array = new short[s.nextInt()];
        for (int i = 0; i < array.length; i++) {
            array[i] = s.nextShort();
        }
        return array;
    }

    private static char[] readFileToCharArray(String path) throws FileNotFoundException {
        Scanner s = new Scanner(NNetParams.class.getResourceAsStream(path), Charsets.UTF_8);
        // Read first entry (array size) as integer
        char[] array = new char[s.nextInt()];
        for (int i = 0; i < array.length; i++) {
            // Read as short as it's uint8 and cast
            array[i] = (char) s.nextShort();
        }
        return array;
    }

    private static float[] readFileToFloatArray(String path) throws FileNotFoundException {
        Scanner s = new Scanner(NNetParams.class.getResourceAsStream(path), Charsets.UTF_8);
        // Read first entry (array size) as integer
        float[] array = new float[s.nextInt()];
        for (int i = 0; i < array.length; i++) {
            String text = s.next();

            try {
                float f = Float.parseFloat(text);
                array[i] = f;
            } catch (Exception e) {
                // TODO resolve error
                throw e;
            }
        }
        return array;
    }

    // Access methods for parameters - these methods should be defined interface so the core embedding code
    // can be applied to different types of network. TODO optionally prefix these with 'get'

    // Access methods for embeddings:
    public int embeddingsSize() {
        return EMBEDDINGS_SIZE;
    }

    public int embeddingsNumRows(int i) {
        return kEmbeddingsNumRows[i];
    }

    public int embeddingsNumCols(int i) {
        return kEmbeddingsNumCols[i];
    }

    public char[] embeddingsWeights(int i) {
        return embeddingsWeights[i];
    }

    public short[] embeddingsQuantScales(int i) {
        return embeddingsQuantScales[i];
    }

    public int embeddingDimSize() {
        return EMBEDDING_DIM_SIZE;
    }

    public int embeddingDim(int i) {
        return kEmbeddingDimValues[i];
    }

    public int embeddingNumFeaturesSize() {
        return EMBEDDING_NUM_FEATURES_SIZE;
    }

    public int embeddingNumFeatures(int i) {
        return kEmbeddingNumFeaturesValues[i];
    }


    // Access methods for hidden:
    public int hiddenSize() {
        return HIDDEN_SIZE;
    }

    public int hiddenNumRows(int i) {
        return kHiddenNumRows[i];
    }

    public int hiddenNumCols(int i) {
        return kHiddenNumCols[i];
    }

    public float[] hiddenWeights(int i) {
        return hiddenWeights[i];
    }

    // Access methods for hidden_bias:
    public int hiddenBiasSize() {
        return HIDDEN_BIAS_SIZE;
    }

    public int hiddenBiasNumRows(int i) {
        return kHiddenBiasNumRows[i];
    }

    public int hiddenBiasNumCols(int i) {
        return kHiddenBiasNumCols[i];
    }

    public float[] hiddenBiasWeights(int i) {
        return hiddenBiasWeights[i];
    }

    // Access methods for softmax:
    public int softmaxSize() {
        return SOFTMAX_SIZE;
    }

    public int softmaxNumRows(int i) {
        return kSoftmaxNumRows[i];
    }

    public int softmaxNumCols(int i) {
        return kSoftmaxNumCols[i];
    }

    public float[] softmaxWeights(int i) {
        return softmaxWeights[i];
    }

    // Access methods for softmax_bias:
    public int softmaxBiasSize() {
        return SOFTMAX_BIAS_SIZE;
    }

    public int softmaxBiasNumRows(int i) {
        return kSoftmaxBiasNumRows[i];
    }

    public int softmaxBiasNumCols(int i) {
        return kSoftmaxBiasNumCols[i];
    }

    public float[] softmaxBiasWeights(int i) {
        return softmaxBiasWeights[i];
    }

    // Access methods for concat layer
    public int concatLayerSize() {
        return CONCAT_LAYER_SIZE;
    }

    public int concatOffsetSize() {
        return CONCAT_OFFSET_SIZE;
    }

    public int concatOffset(int i) {
        return kConcatOffsetValues[i];
    }
}
