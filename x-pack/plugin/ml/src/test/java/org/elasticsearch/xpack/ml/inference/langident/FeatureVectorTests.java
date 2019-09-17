/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.is;

public class FeatureVectorTests extends ESTestCase {

    public void testSimpleVector() {
        FeatureVector vector = new FeatureVector();

        final long initialValue = 4403827575739124461L;

        long value = initialValue;
        int size = 100;
        int i = 0;
        while(i < size) {
            vector.add(FeatureVector.FeatureType.CONTINUOUS_BAG_OF_NGRAMS_100_1, value++);
            ++i;
        }
        while(i < 2*size) {
            vector.add(FeatureVector.FeatureType.CONTINUOUS_BAG_OF_RELEVANT_SCRIPTS, value++);
            ++i;
        }

        assertThat(vector.size(), is(2*size));

        value = initialValue;
        i = 0;
        while(i < size) {
            assertThat(vector.getTypeAt(i), is(FeatureVector.FeatureType.CONTINUOUS_BAG_OF_NGRAMS_100_1));
            assertThat(vector.getValueAt(i), is(value++));
            ++i;
        }
        while(i < 2*size) {
            assertThat(vector.getTypeAt(i), is(FeatureVector.FeatureType.CONTINUOUS_BAG_OF_RELEVANT_SCRIPTS));
            assertThat(vector.getValueAt(i), is(value++));
            ++i;
        }
    }

    public void testFeatureValue() {
        // test coercion vs C++ results

        // Encode
        assertEquals("0", Long.toUnsignedString(FeatureVector.FeatureValue.getFeatureValue(0, 0.0f)));
        assertEquals("4466648494715650624", Long.toUnsignedString(FeatureVector.FeatureValue.getFeatureValue(1000000, 0.1234f)));
        assertEquals("4466648494714650634", Long.toUnsignedString(FeatureVector.FeatureValue.getFeatureValue(10, 0.1234f)));
        assertEquals("4631923393225054914", Long.toUnsignedString(FeatureVector.FeatureValue.getFeatureValue(23234, 3.1234f)));
        assertEquals("9187343229098393599", Long.toUnsignedString(FeatureVector.FeatureValue.getFeatureValue(2147483647, 3.402823E38f)));

        // Decode
        FeatureVector.FeatureValue.FloatFeatureValue f;

        f = FeatureVector.FeatureValue.getFeatureValue(0L);
        assertEquals(0, f.getId());
        assertEquals(0f, f.getWeight(), 0.0);

        f = FeatureVector.FeatureValue.getFeatureValue(4466648494715650624L);
        assertEquals(1000000, f.getId());
        assertEquals(0.1234f, f.getWeight(), 0.0);

        f = FeatureVector.FeatureValue.getFeatureValue(4466648494714650634L);
        assertEquals(10, f.getId());
        assertEquals(0.1234f, f.getWeight(), 0.0);

        f = FeatureVector.FeatureValue.getFeatureValue(4631923393225054914L);
        assertEquals(23234, f.getId());
        assertEquals(3.1234f, f.getWeight(), 0.0);

        f = FeatureVector.FeatureValue.getFeatureValue(9187343229098393599L);
        assertEquals(2147483647, f.getId());
        assertEquals(3.402823E38f, f.getWeight(), 0.0);
    }
}
