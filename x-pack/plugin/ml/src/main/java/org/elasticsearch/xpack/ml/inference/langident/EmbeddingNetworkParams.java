/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

/**
 *
 */
public abstract class EmbeddingNetworkParams {
    public enum QuantizationType {
        NONE,
        UINT8
    };

    // Simple representation of a matrix.  This small struct that doesn't own any
    // resource intentionally supports copy / assign, to simplify our APIs.
    public class Matrix {
        private final int rows;
        private final int cols;

        // Pointer to matrix elements, in row-major order
        // (https://en.wikipedia.org/wiki/Row-major_order) Not owned.
        // Elements can be bytes or floats
        private final char[] byteElements;
        private final float[] floatElements;

        private final QuantizationType quantType;

        // Quantization scales: one scale for each row. Not owned.
        private final short[] quantScales;

        public Matrix(int rows, int cols, char[] byteElements,
                      QuantizationType quantType, short[] quantScales) {
            this.rows = rows;
            this.cols = cols;
            this.byteElements = byteElements;
            this.floatElements = null;
            this.quantType = quantType;
            this.quantScales = quantScales;
        }

        public Matrix(int rows, int cols, QuantizationType quantType, float[] floatElements) {
            this.rows = rows;
            this.cols = cols;
            this.byteElements = null;
            this.floatElements = floatElements;
            this.quantType = quantType;
            this.quantScales = null;
        }

        public int getRows() {
            return rows;
        }

        public int getCols() {
            return cols;
        }

        public char[] getByteElements() {
            return byteElements;
        }

        public float[] getFloatElements() {
            return floatElements;
        }

        public QuantizationType getQuantType() {
            return quantType;
        }

        public short[] getQuantScales() {
            return quantScales;
        }
    }

    // Returns i-th embedding matrix.  Crashes on out of bounds indices.
    // This is the transpose of the corresponding matrix from the original proto.
    public Matrix getEmbeddingMatrix(int i) {
        assert (i >= 0 && i < embeddingsSize());
        Matrix matrix = new Matrix(
            embeddingsNumRows(i),
            embeddingsNumCols(i),
            embeddingsWeights(i),
            QuantizationType.UINT8,
            embeddingsQuantScales(i));
        return matrix;
    }

    // Returns i-th hidden layer.
    public Matrix getHiddenLayerMatrix(int i) {
        assert (i >= 0 && i < hiddenSize());
        Matrix matrix = new Matrix(
            hiddenNumRows(i),
            hiddenNumCols(i),
            QuantizationType.NONE,
            hiddenWeights(i));
        return matrix;
    }

    // Returns bias for i-th hidden layer.  Technically a Matrix, but we expect it
    // to be a row/column vector (i.e., num rows or num cols is 1).  However, we
    // don't CHECK for that: we just provide access to underlying data.
    public Matrix getHiddenLayerBias(int i) {
        assert (i >= 0 && i < hiddenBiasSize());
        Matrix matrix = new Matrix(
            hiddenBiasNumRows(i),
            hiddenBiasNumCols(i),
            QuantizationType.NONE,
            hiddenBiasWeights(i));
        return matrix;
    }

    // Returns weight matrix for the softmax layer.
    // This is the transpose of the corresponding matrix from the original proto.
    public Matrix getSoftmaxMatrix() {
        assert (softmaxSize() == 1);
        Matrix matrix = new Matrix(
            softmaxNumRows(0),
            softmaxNumCols(0),
            QuantizationType.NONE,
            softmaxWeights(0));
        return matrix;
    }

    // Returns bias for the softmax layer.  Technically a Matrix, but we expect it
    // to be a row/column vector (i.e., num rows or num cols is 1).  However, we
    // don't CHECK for that: we just provide access to underlying data.
    public Matrix getSoftmaxBias() {
        assert (softmaxSize() == 1);
        Matrix matrix = new Matrix(
            softmaxBiasNumRows(0),
            softmaxBiasNumCols(0),
            QuantizationType.NONE,
            softmaxBiasWeights(0));
        return matrix;
    }

    // Low-level API
    // ** Access methods for repeated MatrixParams embeddings.
    //
    abstract int embeddingsSize();

    // Returns number of rows of transpose(proto.embeddings(i)).
    abstract int embeddingsNumRows(int i);

    // Returns number of columns of transpose(proto.embeddings(i)).
    abstract int embeddingsNumCols(int i);

    // Returns pointer to elements of transpose(proto.embeddings(i)), in row-major
    // order.
    abstract char[] embeddingsWeights(int i);

    boolean embeddingsIsQuant(int i) {
        return false;
    }

    abstract short[] embeddingsQuantScales(int i);

    // ** Access methods for repeated MatrixParams hidden.
    //
    // Returns embedding_network_proto.hidden_size().
    abstract int hiddenSize();

    // Returns embedding_network_proto.hidden(i).rows().
    abstract int hiddenNumRows(int i);

    // Returns embedding_network_proto.hidden(i).rows().
    abstract int hiddenNumCols(int i);

    // Returns pointer to beginning of array of floats with all values from
    // embedding_network_proto.hidden(i).
    abstract float[] hiddenWeights(int i);

    // ** Access methods for repeated MatrixParams hidden_bias.
    //
    // Returns proto.hidden_bias_size().
    abstract int hiddenBiasSize();

    // Returns number of rows of proto.hidden_bias(i).
    abstract int hiddenBiasNumRows(int i);

    // Returns number of columns of proto.hidden_bias(i).
    abstract int hiddenBiasNumCols(int i);

    // Returns pointer to elements of proto.hidden_bias(i), in row-major order.
    abstract float[] hiddenBiasWeights(int i);

    // ** Access methods for optional MatrixParams softmax.
    //
    // Returns 1 if proto has optional field softmax, 0 otherwise.
    abstract int softmaxSize();

    // Returns number of rows of transpose(proto.softmax()).
    abstract int softmaxNumRows(int i);

    // Returns number of columns of transpose(proto.softmax()).
    abstract int softmaxNumCols(int i);

    // Returns pointer to elements of transpose(proto.softmax()), in row-major
    // order.
    abstract float[] softmaxWeights(int i);

    // ** Access methods for optional MatrixParams softmax_bias.
    //
    // Returns 1 if proto has optional field softmax_bias, 0 otherwise.
    abstract int softmaxBiasSize();

    // Returns number of rows of proto.softmax_bias().
    abstract int softmaxBiasNumRows(int i);

    // Returns number of columns of proto.softmax_bias().
    abstract int softmaxBiasNumCols(int i);

    // Returns pointer to elements of proto.softmax_bias(), in row-major order.
    abstract float[] softmaxBiasWeights(int i);

    // ** Access methods for repeated int32 embedding_dim.
    //
    // Returns proto.embedding_dim_size().
    abstract int embeddingDimSize();

    // Returns proto.embedding_dim(i).
    abstract int embeddingDim(int i);

    // ** Access methods for repeated int32 embedding_num_features.
    //
    // Returns proto.embedding_num_features_size().
    abstract int embeddingNumFeaturesSize();

    // Returns proto.embedding_num_features(i).
    abstract int embeddingNumFeatures(int i);

    // ** Access methods for repeated int32 concat_offset.
    //
    // Returns proto.concat_offset_size().
    abstract int concatOffset(int i);

    // Returns proto.concat_offset(i).
    abstract int concatOffsetSize();

    // ** Access methods for concat_layer_size.
    //
    // Returns proto.concat_layer_size().
    abstract int concatLayerSize();
}
