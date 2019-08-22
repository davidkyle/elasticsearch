/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import com.google.common.base.Charsets;
import org.elasticsearch.test.ESTestCase;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import static org.hamcrest.Matchers.greaterThan;

public class FeatureUtilsTests extends ESTestCase {

    public void testHash32WithDefaultSeed() throws UnsupportedEncodingException {
        HashMap<String, String> expected = new HashMap<>();

        expected.put("$", "3182025368");
        expected.put("t", "138132021");
        expected.put("i", "2965039166");
        expected.put("l", "1580082402");
        expected.put("^", "2207093652");

        expected.put("in", "4112208493");
        expected.put("en", "2079238153");
        expected.put("^$", "4032735838");
        expected.put("tt", "817858275");
        expected.put("it", "1589737287");
        expected.put("n$", "2413977248");

        expected.put("rit", "1921565252");
        expected.put("^in", "2446331876");
        expected.put("tte", "1722883625");
        expected.put("^is", "1526307372");
        expected.put("^wr", "2206189520");

        expected.put("^is$", "790119734");
        expected.put("glis", "3888555831");
        expected.put("^tex", "260719639");
        expected.put("text", "1712111248");
        expected.put("ritt", "3582828510");

        expected.put("다", "2787769683");
        expected.put("세", "1500228512");
        expected.put("습", "2218653723");
        expected.put("할", "3913427461");
        expected.put("여", "2765963430");

        expected.put("한$", "2241076599");
        expected.put("다$", "1903255056");
        expected.put("니다", "2071203213");
        expected.put("있습", "530856195");
        expected.put("수$", "681457790");

        expected.put("포트$", "3931486187");
        expected.put("습니다", "3371415996");
        expected.put("^리포", "3050042250");
        expected.put("권한$", "2791953805");

        expected.put("습니다$", "2290190997");
        expected.put("^있습니", "2954770380");
        expected.put("^권한$", "3837435447");
        expected.put("부여할$", "815437673");
        expected.put("^부여할", "151570116");

        for (HashMap.Entry<String, String> entry : expected.entrySet()) {
            String text = entry.getKey();
            String hash = entry.getValue();

            int h = FeatureUtils.Hash32WithDefaultSeed(text);

            assertEquals(hash, Integer.toUnsignedString(h));
        }
    }

    public void testValidUTF8Length() {
        {
            // Truncate to UTF8 boundary (no cut)
            String strAZ = " a az qalıb breyn rinq intellektual oyunu üzrə yarışın zona mərhələləri " +
                "keçirilib miq un qalıqlarının dənizdən çıxarılması davam edir məhəmməd " +
                "peyğəmbərin karikaturalarını çap edən qəzetin baş redaktoru iş otağında " +
                "ölüb";

            int i = FeatureUtils.validUTF8Length(strAZ, 200);
            assertEquals(200, i);
            // string was truncated
            assertThat(strAZ.getBytes(Charsets.UTF_8).length, greaterThan(i));
        }
        {
            // Truncate to UTF8 boundary (cuts)
            String strBE = " а друкаваць іх не было тэхнічна магчыма бліжэй за вільню тым самым часам " +
                "нямецкае кіраўніцтва прапаноўвала апроч ўвядзення лацінкі яе";

            int i = FeatureUtils.validUTF8Length(strBE, 200);
            assertEquals(199, i);
            assertThat(strBE.getBytes(Charsets.UTF_8).length, greaterThan(i));
        }
        {
            // Don't truncate
            String strAR = "احتيالية بيع أي حساب";
            int i = FeatureUtils.validUTF8Length(strAR, 200);
            assertEquals(37, i);
            assertEquals(strAR.getBytes(Charsets.UTF_8).length, i);
        }
        {
            // Truncate to UTF8 boundary (cuts)
            String strZH = "产品的简报和公告 提交该申请后无法进行更改 请确认您的选择是正确的 " +
                "对于要提交的图书 我确认 我是版权所有者或已得到版权所有者的授权 " +
                "要更改您的国家 地区 请在此表的最上端更改您的";

            int i = FeatureUtils.validUTF8Length(strZH, 200);
            assertEquals(198, i);
            assertThat(strZH.getBytes(Charsets.UTF_8).length, greaterThan(i));
        }
    }

    public void testFindNumValidBytesToProcess() throws UnsupportedEncodingException {
        // Testing mainly covered by testValidUTF8Length
        String strZH = "产品的简报和公告 提交该申请后无法进行更改 请确认您的选择是正确的 " +
            "对于要提交的图书 我确认 我是版权所有者或已得到版权所有者的授权 " +
            "要更改您的国家 地区 请在此表的最上端更改您的";

        String text = FeatureUtils.truncateToNumValidBytes(strZH, NNetLanguageIdentifier.kMaxNumInputBytesToConsider);
        assertEquals(strZH.length(), text.length());
    }

    public void testCleanText() {
        HashMap<String, String> expected = new HashMap<>();

        expected.put("This has a tag in <br> it <ssss>&..///1/2@@3winter", " this has a tag in br it ssss winter ");
        expected.put(" This has a tag in <br> it <ssss>&..///1/2@@3winter ", " this has a tag in br it ssss winter ");
        expected.put(" This has a tag in <p> it </p><ssss>&..///1/2@@3winter ", " this has a tag in p it p ssss winter ");
        expected.put("  This has a tag in \n<p> it \r\n</p><ssss>&..///1/2@@3winter ", " this has a tag in p it p ssss winter ");
        expected.put(" !This has    a tag.in\n+|iW£#   <p> hello\nit </p><ssss>&..///1/2@@3winter ",
            " this has a tag in iw p hello it p ssss winter ");
        expected.put("北京——。", " 北京 ");
        expected.put("北京——中国共产党已为国家主席习近平或许无限期地继续执政扫清了道路。", " 北京 中国共产党已为国家主席习近平或许无限期地继续执政扫清了道路 ");

        for (HashMap.Entry<String, String> entry : expected.entrySet()) {
            String original = entry.getKey();
            String text = entry.getValue();

            String cleaned = FeatureUtils.cleanAndLowerText(original);
            assertEquals(text, cleaned);
        }
    }
}
