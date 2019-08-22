/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.test.ESTestCase;

import java.io.UnsupportedEncodingException;

public class RelevantScriptFeatureTests extends ESTestCase {
    private void testEvaluateResults(FeatureVector results,
                                     int size, int index, ScriptDetector.Script id, float weight) {
        assertEquals(size, results.size());
        assertTrue(index >= 0 && index < results.size());
        assertEquals(FeatureVector.FeatureType.CONTINUOUS_BAG_OF_RELEVANT_SCRIPTS, results.getTypeAt(index));
        FeatureVector.FeatureValue.FloatFeatureValue f =
            FeatureVector.FeatureValue.getFeatureValue(results.getValueAt(index));
        assertEquals(f.getId(), id.toInt());
        assertEquals(f.getWeight(), weight, 0.0001f);
    }

    public void testCommonCases() throws UnsupportedEncodingException {
        FeatureVector results = null;

        results = RelevantScriptFeature.getRelevantScriptFeature("just some plain text");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptOtherUtf8OneByte, 1.0f );

        results = RelevantScriptFeature.getRelevantScriptFeature("ヸヂ゠ヂ");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptKatakana, 1.0f );

        // 4 Latin letters mixed with 4 Katakana letters.
        results = RelevantScriptFeature.getRelevantScriptFeature("ヸtヂe゠xtヂ");
        testEvaluateResults(results, 2, 0, ScriptDetector.Script.kScriptOtherUtf8OneByte, 0.5f );
        testEvaluateResults(results, 2, 1, ScriptDetector.Script.kScriptKatakana, 0.5f );

        results = RelevantScriptFeature.getRelevantScriptFeature("\"just some 121212%^^( ヸヂ゠ヂ   text\"");
        testEvaluateResults(results, 2, 0, ScriptDetector.Script.kScriptOtherUtf8OneByte, 0.75f );
        testEvaluateResults(results, 2, 1, ScriptDetector.Script.kScriptKatakana, 0.25f );
    }

    public void testCornerCases() throws UnsupportedEncodingException {
        FeatureVector results = null;

        // Empty string.
        results = RelevantScriptFeature.getRelevantScriptFeature("");
        assertEquals(0, results.size());

        // Only whitespaces.
        results = RelevantScriptFeature.getRelevantScriptFeature("   ");
        assertEquals(0, results.size());

        // Only numbers and punctuation.
        results = RelevantScriptFeature.getRelevantScriptFeature("12----)(");
        assertEquals(0, results.size());

        // Only numbers, punctuation, and spaces.
        results = RelevantScriptFeature.getRelevantScriptFeature("12--- - ) ( ");
        assertEquals(0, results.size());

        // One UTF8 character by itself.
        results = RelevantScriptFeature.getRelevantScriptFeature("ゟ");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptHiragana, 1.0f );

        results = RelevantScriptFeature.getRelevantScriptFeature("ה");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptHebrew, 1.0f );

        // One UTF8 character with some numbers / punctuation / spaces: character at
        // one extremity or in the middle.
        results = RelevantScriptFeature.getRelevantScriptFeature("1234ゟ");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptHiragana, 1.0f );

        results = RelevantScriptFeature.getRelevantScriptFeature("ゟ12-(");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptHiragana, 1.0f );

        results = RelevantScriptFeature.getRelevantScriptFeature("8*1ゟ12----");
        testEvaluateResults(results, 1, 0, ScriptDetector.Script.kScriptHiragana, 1.0f );
    }
}
