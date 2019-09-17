/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;


import java.util.ArrayList;
import java.util.Arrays;

/**
 * Classifier using a hand-coded feed-forward neural network.
 * <p>
 * No gradient computation, just inference.
 * <p>
 * Classification works as follows:
 * <p>
 * Discrete features - Embeddings - Concatenation - Hidden+ - Softmax
 * <p>
 * In words: given some discrete features, this class extracts the embeddings
 * for these features, concatenates them, passes them through one or two hidden
 * layers (each layer uses Relu) and next through a softmax layer that computes
 * an unnormalized score for each possible class.  Note: there is always a
 * softmax layer.
 */
public class EmbeddingNetwork {
    // Fills a Matrix object with the parameters in the given MatrixParams.  This
    // function is used to initialize weight matrices that are *not* embedding
    // matrices.
    public static Matrix fillMatrixParams(EmbeddingNetworkParams.Matrix sourceMatrix) {
        assert (sourceMatrix.getQuantType() == EmbeddingNetworkParams.QuantizationType.NONE);

        Matrix result = new Matrix(sourceMatrix.getRows());

        float[] weights = sourceMatrix.getFloatElements();
        assert (weights != null);
        int w = 0;
        for (int i = 0; i < sourceMatrix.getRows(); ++i) {
            // TODO - change to java types and make this less C++
            VectorWrapper vec = new VectorWrapper(sourceMatrix.getCols());
            for (int j = 0; j < sourceMatrix.getCols(); ++j) {
                vec.set(j, weights[w++]);
            }
            result.set(i, vec);
        }
        return result;
    }

    // TODO - change to java types and make this less C++
    public static class Matrix {
        public Matrix(int size) {
            rows = new VectorWrapper[size];
        }

        public void set(int i, VectorWrapper row) {
            assert (i >= 0 && i < size());
            rows[i] = row;
        }

        public VectorWrapper get(int i) {
            assert (i >= 0 && i < size());
            return rows[i];
        }

        int size() {
            return rows.length;
        }

        private final VectorWrapper[] rows;
    }

    public static class VectorWrapper {
        public VectorWrapper(int size) {
            cols = new float[size];
        }

        public VectorWrapper(float[] input, int size) {
            cols = Arrays.copyOf(input, size);
        }

        public void set(int i, float value) {
            assert (i >= 0 && i < size());
            cols[i] = value;
        }

        public float get(int i) {
            assert (i >= 0 && i < size());
            return cols[i];
        }

        int size() {
            return cols.length;
        }

        private final float[] cols;
    }

    // Class used to represent an embedding matrix.  Each row is the embedding on
    // a vocabulary element.  Number of columns = number of embedding dimensions.
    public class EmbeddingMatrix {
        public EmbeddingMatrix(EmbeddingNetworkParams.Matrix sourceMatrix) {
            rows = sourceMatrix.getRows();
            cols = sourceMatrix.getCols();
            quantType = sourceMatrix.getQuantType();
            data = sourceMatrix.getByteElements();
            quantScales = sourceMatrix.getQuantScales();

            // for now we ONLY support UINT8 quantType
            assert (quantType == EmbeddingNetworkParams.QuantizationType.UINT8);
        }

        // Returns vocabulary size; one embedding for each vocabulary element.
        public int size() {
            return rows;
        }

        // Returns number of weights in embedding of each vocabulary element.
        public int dim() {
            return cols;
        }

        // Returns quantization type for this embedding matrix.
        EmbeddingNetworkParams.QuantizationType quantType() {
            return quantType;
        }

        // Quant scales are stored as short uint16 max(15769) - note this doesn't work
        // for full range of uint16...
        // -> convert to float
        private float shortToFloat(short s) {
            // We fill in the new mantissa bits with 0, and don't do anything smarter.
            int i = (s << 16);
            return (float) s;

//            return Float.intBitsToFloat(i);
        }

        float getQuantScales(int row) {
            assert (row >= 0 && row < size());
            return shortToFloat(quantScales[row]);
        }

        char getData(int row, int col) {
            assert (row >= 0 && row < size());
            assert (col >= 0 && col < dim());
            return data[row * dim() + col];
        }

        // Vocabulary size.
        private final int rows;

        // Number of elements in each embedding.
        private final int cols;

        // Pointer to the embedding weights, in row-major order.  This is a pointer
        // to an array of uint8 (cld3 float void * not supported)
        private final char[] data;

        private final EmbeddingNetworkParams.QuantizationType quantType;

        // Pointer to quantization scales.  nullptr if no quantization.  Otherwise,
        // quant_scales_[i] is scale for embedding of i-th vocabulary element.
        // NOTE: data is held as shorts that represent float16s
        private final short[] quantScales;
    }

    // class EmbeddingNetwork
    private final EmbeddingNetworkParams model;

    // Network parameters.

    // One weight matrix for each embedding.
    private final ArrayList<EmbeddingMatrix> embeddingMatrices;

    // One weight matrix and one vector of bias weights for each hidden layer.
    private final ArrayList<Matrix> hiddenWeights;
    private final ArrayList<VectorWrapper> hiddenBias;

    // Weight matrix and bias vector for the softmax layer.
    private final Matrix softmaxWeights;
    private final VectorWrapper softmaxBias;


    public EmbeddingNetwork(EmbeddingNetworkParams m) {
        model = m;
        int offsetSum = 0;
        embeddingMatrices = new ArrayList<>();
        for (int i = 0; i < model.embeddingDimSize(); ++i) {
            assert (offsetSum == model.concatOffset(i));
            offsetSum += model.embeddingDim(i) * model.embeddingNumFeatures(i);
            embeddingMatrices.add(new EmbeddingMatrix(model.getEmbeddingMatrix(i)));
        }

        assert (model.hiddenSize() == model.hiddenBiasSize());
        hiddenWeights = new ArrayList<>(model.hiddenSize());
        hiddenBias = new ArrayList<>(model.hiddenSize());
        for (int i = 0; i < model.hiddenSize(); ++i) {
            hiddenWeights.add(fillMatrixParams(model.getHiddenLayerMatrix(i)));

            EmbeddingNetworkParams.Matrix bias = model.getHiddenLayerBias(i);
            assert (bias.getCols() == 1);
            assert (bias.getQuantType() == EmbeddingNetworkParams.QuantizationType.NONE);
            hiddenBias.add(new VectorWrapper(bias.getFloatElements(), bias.getRows()));
        }

        assert (model.softmaxSize() == 1);
        softmaxWeights = fillMatrixParams(model.getSoftmaxMatrix());

        EmbeddingNetworkParams.Matrix softmaxBiasMat = model.getSoftmaxBias();
        assert (softmaxBiasMat.getCols() == 1);
        assert (softmaxBiasMat.getQuantType() == EmbeddingNetworkParams.QuantizationType.NONE);
        softmaxBias = new VectorWrapper(softmaxBiasMat.getFloatElements(), softmaxBiasMat.getRows());
    }

    public ArrayList<Float> computeFinalScores(ArrayList<FeatureVector> features) {
        // Consolidate the feature vector
        ArrayList<Float> concat = concatEmbeddings(features);

        return finishComputeFinalScores(concat);
    }

    private ArrayList<Float> finishComputeFinalScores(ArrayList<Float> concat) {
        ArrayList<Float> h0 = sparseReluProductPlusBias(false, getHiddenWeights().get(0),
            getHiddenBias().get(0), concat);

        // Assume 1 hidden layer
        assert(hiddenWeights.size() == 1);
        ArrayList<Float> scores = sparseReluProductPlusBias(true, softmaxWeights,
            softmaxBias, h0);

        return scores;
    }

    // Computes y = weights * Relu(x) + b where Relu is optionally applied.
    private ArrayList<Float> sparseReluProductPlusBias(boolean apply_relu,
                                                       Matrix weights,
                                                       VectorWrapper b,
                                                       ArrayList<Float> x) {
        // Initialise y with b
        ArrayList<Float> y = new ArrayList<>(b.size());
        for (int i = 0; i < b.size(); ++i) {
            y.add(b.get(i));
        }

        // TODO - some loss in precision here compared to C++ - investigate further
        for (int i = 0; i < x.size(); ++i) {
            float scale = x.get(i);
            if (apply_relu) {
                if (scale > 0) {
                    for (int j = 0; j < y.size(); ++j) {
                        float value = y.get(j) + (weights.get(i).get(j) * scale);
                        y.set(j, value);
                    }
                }
            } else {
                for (int j = 0; j < y.size(); ++j) {
                    float value = y.get(j) + (weights.get(i).get(j) * scale);
                    y.set(j, value);
                }
            }
        }
        return y;
    }

    private ArrayList<Float> concatEmbeddings(ArrayList<FeatureVector> featureVectors) {
        // Initialise empty list with 0s
        // TODO this better...
        ArrayList<Float> concat = new ArrayList<>(model.concatLayerSize());
        for (int i = 0; i < model.concatLayerSize(); ++i) {
            concat.add(0.0f);
        }

        // "esIndex" stands for "embedding space index".
        for (int esIndex = 0; esIndex < featureVectors.size(); ++esIndex) {
            int concatOffset = model.concatOffset(esIndex);
            int embeddingDim = model.embeddingDim(esIndex);

            EmbeddingMatrix embeddingMatrix = embeddingMatrices.get(esIndex);
            assert (embeddingMatrix.dim() == embeddingDim);

            boolean isQuantized = (embeddingMatrix.quantType() != EmbeddingNetworkParams.QuantizationType.NONE);
            // For cld3 this is always true
            assert (isQuantized);

            FeatureVector featureVector = featureVectors.get(esIndex);
            int numFeatures = featureVector.size();
            for (int fi = 0; fi < numFeatures; ++fi) {
                FeatureVector.FeatureType featureType = featureVector.getTypeAt(fi);
                // TODO - base + embedding_dim ignored as base==0
                int featureOffset = concatOffset;
                assert (featureOffset < concat.size());

                // Weighted embeddings will be added starting from this address.
                float concatPtr = concat.get(featureOffset);

                // Multiplier for each embedding weight.
                float multiplier = 0.0f;
                int row = 0;
                long featureValue = featureVector.getValueAt(fi);
                if (FeatureVector.isContinuous(featureType)) {
                    // Continuous features (encoded as FloatFeatureValue).
                    FeatureVector.FeatureValue.FloatFeatureValue floatFeatureValue =
                        FeatureVector.FeatureValue.getFeatureValue(featureValue);
                    row = floatFeatureValue.getId();
                    multiplier = embeddingMatrix.getQuantScales(row);
                    multiplier *= floatFeatureValue.getWeight();
                } else {
                    // Discrete features: every present feature has implicit value 1.0.
                    row = (int)featureValue;
                    multiplier = embeddingMatrix.getQuantScales(row);
                }

                // Iterate across columns for this row
                for (int i = 0; i < embeddingDim; ++i) {
                    // 128 is bias for UINT8 quantization, only one we currently support.
                    float value = (embeddingMatrix.getData(row, i) - 128) * multiplier;
                    int concatIndex = featureOffset + i;
                    concat.set(concatIndex, concat.get(concatIndex) + value);
                }
            }
        }

        // Results same as cld3
        return concat;
    }

    // TODO

    public ArrayList<Matrix> getHiddenWeights() {
        return hiddenWeights;
    }

    public ArrayList<VectorWrapper> getHiddenBias() {
        return hiddenBias;
    }
}
