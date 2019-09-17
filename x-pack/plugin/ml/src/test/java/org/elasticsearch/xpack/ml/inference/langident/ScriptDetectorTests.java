/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.test.ESTestCase;

public class ScriptDetectorTests extends ESTestCase {

    public void testGreekScript() {
        // The first two conditions check first / last character from the Greek and
        // Coptic script. The last two ones are negative tests.
        assertTrue(
                ScriptDetector.Script.kScriptGreek == ScriptDetector.getScript("Ͱ") &&
                        ScriptDetector.Script.kScriptGreek == ScriptDetector.getScript("Ͽ") &&
                        ScriptDetector.Script.kScriptGreek == ScriptDetector.getScript("δ") &&
                        ScriptDetector.Script.kScriptGreek == ScriptDetector.getScript("Θ") &&
                        ScriptDetector.Script.kScriptGreek == ScriptDetector.getScript("Δ") &&
                        ScriptDetector.Script.kScriptGreek != ScriptDetector.getScript("a") &&
                        ScriptDetector.Script.kScriptGreek != ScriptDetector.getScript("0")
        );
    }

    public void testCyrillicScript() {
        assertTrue(
                ScriptDetector.Script.kScriptCyrillic == ScriptDetector.getScript("Ѐ") &&
                        ScriptDetector.Script.kScriptCyrillic == ScriptDetector.getScript("ӿ") &&
                        ScriptDetector.Script.kScriptCyrillic == ScriptDetector.getScript("ш") &&
                        ScriptDetector.Script.kScriptCyrillic == ScriptDetector.getScript("Б") &&
                        ScriptDetector.Script.kScriptCyrillic == ScriptDetector.getScript("Ӱ")
        );
    }

    public void testHebrewScript() {
        assertTrue(
                ScriptDetector.Script.kScriptHebrew == ScriptDetector.getScript("֑") &&
                        ScriptDetector.Script.kScriptHebrew == ScriptDetector.getScript("״") &&
                        ScriptDetector.Script.kScriptHebrew == ScriptDetector.getScript("ד") &&
                        ScriptDetector.Script.kScriptHebrew == ScriptDetector.getScript("ה") &&
                        ScriptDetector.Script.kScriptHebrew == ScriptDetector.getScript("צ")
        );
    }

    public void testArabicScript() {
        assertTrue(ScriptDetector.Script.kScriptArabic == ScriptDetector.getScript("م") &&
            ScriptDetector.Script.kScriptArabic == ScriptDetector.getScript("خ"));
    }

    public void testHangulJamoScript() {
        assertTrue(ScriptDetector.Script.kScriptHangulJamo == ScriptDetector.getScript("ᄀ") &&
            ScriptDetector.Script.kScriptHangulJamo == ScriptDetector.getScript("ᇿ") &&
            ScriptDetector.Script.kScriptHangulJamo == ScriptDetector.getScript("ᄡ") &&
            ScriptDetector.Script.kScriptHangulJamo == ScriptDetector.getScript("ᆅ") &&
            ScriptDetector.Script.kScriptHangulJamo == ScriptDetector.getScript("ᅘ"));
    }

    public void testHiraganaScript() {
        assertTrue(ScriptDetector.Script.kScriptHiragana == ScriptDetector.getScript("ぁ") &&
            ScriptDetector.Script.kScriptHiragana == ScriptDetector.getScript("ゟ") &&
            ScriptDetector.Script.kScriptHiragana == ScriptDetector.getScript("こ") &&
            ScriptDetector.Script.kScriptHiragana == ScriptDetector.getScript("や") &&
            ScriptDetector.Script.kScriptHiragana == ScriptDetector.getScript("ぜ"));
    }

    public void testKatakanaScript() {
        assertTrue(ScriptDetector.Script.kScriptKatakana == ScriptDetector.getScript("゠") &&
            ScriptDetector.Script.kScriptKatakana == ScriptDetector.getScript("ヿ") &&
            ScriptDetector.Script.kScriptKatakana == ScriptDetector.getScript("ヂ") &&
            ScriptDetector.Script.kScriptKatakana == ScriptDetector.getScript("ザ") &&
            ScriptDetector.Script.kScriptKatakana == ScriptDetector.getScript("ヸ"));
    }

    public void testOtherScripts() {
        assertFalse(ScriptDetector.Script.kScriptOtherUtf8OneByte != ScriptDetector.getScript("^") ||
            ScriptDetector.Script.kScriptOtherUtf8OneByte != ScriptDetector.getScript("$"));


        // Unrecognized 2-byte scripts.  For info on the scripts mentioned below, see
        // http://www.unicode.org/charts/#scripts Note: the scripts below are uniquely
        // associated with a language.  Still, the number of queries in those
        // languages is small and we didn't want to increase the code size and
        // latency, so (at least for now) we do not treat them specially.
        // The following three tests are, respectively, for Armenian, Syriac and
        // Thaana.
        assertFalse(ScriptDetector.Script.kScriptOtherUtf8TwoBytes != ScriptDetector.getScript("Ձ") ||
            ScriptDetector.Script.kScriptOtherUtf8TwoBytes != ScriptDetector.getScript("ܔ") ||
            ScriptDetector.Script.kScriptOtherUtf8TwoBytes != ScriptDetector.getScript("ށ"));

        // Unrecognized 3-byte script: CJK Unified Ideographs: not uniquely associated
        // with a language.
        assertFalse(ScriptDetector.Script.kScriptOtherUtf8ThreeBytes != ScriptDetector.getScript("万") ||
            ScriptDetector.Script.kScriptOtherUtf8ThreeBytes != ScriptDetector.getScript("両"));

        // TODO - investigate these
        /*
        // Unrecognized 4-byte script: CJK Unified Ideographs Extension C.  Note:
        // there is a nice UTF-8 encoder / decoder at https://mothereff.in/utf-8
        assertFalse(ScriptDetector.Script.kScriptOtherUtf8FourBytes != ScriptDetector.getScript("\u00F0\u00AA\u009C\u0094"));

        // Unrecognized 4-byte script: CJK Unified Ideographs Extension E
        assertFalse(ScriptDetector.Script.kScriptOtherUtf8FourBytes != ScriptDetector.getScript("\u00F0\u00AB\u00A0\u00B5") ||
            ScriptDetector.Script.kScriptOtherUtf8FourBytes != ScriptDetector.getScript("\u00F0\u00AC\u00BA\u00A1"));
            */
    }
}
