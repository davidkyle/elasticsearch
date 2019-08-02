/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import java.util.ArrayList;
import java.util.Objects;

/**
 * A feature vector contains type and value pairs - e.g.
 * continuous-bag-of-ngrams(id_dim=1000,size=2): 4403827575739124461
 * where type is an enum, and value is a uint64
 */
public class FeatureVector {
    private final ArrayList<Element> features = new ArrayList<>();

    public void add(FeatureType featureType, long featureValue) {
        features.add(new Element(featureType, featureValue));
    }

    public int size() {
        return features.size();
    }

    public void clear() {
        features.clear();
    }

    public FeatureType getTypeAt(int index) throws IndexOutOfBoundsException {
        return features.get(index).getFeatureType();
    }

    public long getValueAt(int index) throws IndexOutOfBoundsException {
        return features.get(index).getFeatureValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureVector that = (FeatureVector) o;
        return features.equals(that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(features);
    }

    private class Element {
        private final FeatureType featureType;
        private final long featureValue;

        public Element(FeatureType featureType, long featureValue) {
            this.featureType = featureType;
            this.featureValue = featureValue;
        }

        public FeatureType getFeatureType() {
            return featureType;
        }

        public long getFeatureValue() {
            return featureValue;
        }
    }

    // Feature values can we discrete or continuous
    public static class FeatureValue {
        // Create a discrete value from a continuous value
        public static long getFeatureValue(int id, float weight) {
            // this is a union in C++ - so create a int64 from these 32 bit values
            int weightAsInt = Float.floatToRawIntBits(weight);

            // Pack together as union
            long ret = (long) weightAsInt << 32 | id & 0xFFFFFFFFL;

            return ret;
        }

        // Create a continuous value from a discrete value
        public static FloatFeatureValue getFeatureValue(long value) {
            // unpack union
            int id = (int) (value & 0xFFFFFFFFL);

            int weightAsInt = (int) (value >>> 32);
            float weight = Float.intBitsToFloat(weightAsInt);

            return new FloatFeatureValue(id, weight);
        }

        public static class FloatFeatureValue {
            private final int id;
            private final float weight;

            FloatFeatureValue(int id, float weight) {
                this.id = id;
                this.weight = weight;
            }

            int getId() {
                return id;
            }

            float getWeight() {
                return weight;
            }
        }
    }

    /**
     * Initially hard code these variables to values used by trained model
     */
    enum FeatureType {
        CONTINUOUS_BAG_OF_NGRAMS_100_1,
        CONTINUOUS_BAG_OF_NGRAMS_1000_2,
        CONTINUOUS_BAG_OF_NGRAMS_5000_3,
        CONTINUOUS_BAG_OF_NGRAMS_5000_4,
        CONTINUOUS_BAG_OF_RELEVANT_SCRIPTS,
        SCRIPT
    }

    public static boolean isContinuous(FeatureType type) {
        switch (type) {
            case CONTINUOUS_BAG_OF_NGRAMS_100_1:
            case CONTINUOUS_BAG_OF_NGRAMS_1000_2:
            case CONTINUOUS_BAG_OF_NGRAMS_5000_3:
            case CONTINUOUS_BAG_OF_NGRAMS_5000_4:
            case CONTINUOUS_BAG_OF_RELEVANT_SCRIPTS:
                return true;
            case SCRIPT:
                return false;
        }
        return false;
    }
}
