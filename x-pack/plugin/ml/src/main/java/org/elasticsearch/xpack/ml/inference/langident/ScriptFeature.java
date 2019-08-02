/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptFeature {
    private static UnicodeScript.ULScript getScriptIdOfFirstLetter(String text) {
        // TODO - do we need to look for marks as well?
        Matcher m = Pattern.compile("\\b\\p{L}").matcher(text);
        if (!m.find()) {
            return UnicodeScript.ULScript.ULScript_Common;
        }

        // TODO do we need to make this UTF8?
        Character.UnicodeScript unicodeScript = Character.UnicodeScript.of(m.group().codePointAt(0));

        return UnicodeScript.unicodeScriptToULScript(unicodeScript);
    }

    public static FeatureVector getScriptFeature(String text) {
        FeatureVector features = new FeatureVector();

        features.add(FeatureVector.FeatureType.SCRIPT, getScriptFeatureValue(text));

        return features;
    }

    // Get script feature value for the string
    public static long getScriptFeatureValue(String text) {
        UnicodeScript.ULScript ulScript = getScriptIdOfFirstLetter(text);
        if (ulScript != UnicodeScript.ULScript.ULScript_Hani) {
            return ulScript.toInt();
        } else {
            // Out of the codepoints captured by ULScript_Hani, separately count those
            // in Hangul (Korean script) and those in a script other than Hangul.
            int numHangul = 0;
            int numNonHangul = 0;

            // Use builtin Java codepoints
            // TODO do we need to make this UTF8?
            // TODO this is different to cld3 as cld3 splits string in onespan code
            // TODO this assumes whole string is same characterset
            for (int codepoint : (Iterable<Integer>) text.codePoints()::iterator) {
                if (codepoint == 0x20) {
                    continue;
                }

                // Check if the current codepoint is within the ranges associated with
                // Hangul.
                if ((codepoint >= 0x1100 && codepoint <= 0x11FF) ||  // Hangul Jamo
                    (codepoint >= 0xA960 && codepoint <= 0xA97F) ||  // Jamo Extended A
                    (codepoint >= 0xD7B0 && codepoint <= 0xD7FF) ||  // Jamo Extended B
                    (codepoint >= 0x3130 && codepoint <= 0x318F) ||  // Compatibility Jamo
                    (codepoint >= 0xFFA0 && codepoint <= 0xFFDC) ||  // Halfwidth Jamo
                    (codepoint >= 0xAC00 && codepoint <= 0xD7AF)) {  // Hangul Syllables
                    numHangul++;
                } else {
                    numNonHangul++;
                }
            }

            if (numHangul > numNonHangul) {
                return UnicodeScript.ULScript.NUM_ULSCRIPTS.toInt();
            } else {
                return UnicodeScript.ULScript.ULScript_Hani.toInt();
            }
        }
    }
}
