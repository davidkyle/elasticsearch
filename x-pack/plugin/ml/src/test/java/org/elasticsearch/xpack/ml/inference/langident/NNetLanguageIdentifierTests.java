/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import org.elasticsearch.test.ESTestCase;

public class NNetLanguageIdentifierTests extends ESTestCase {
    public void testSimpleEn() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        NNetLanguageIdentifier.Result result = identifier.findLanguage("This is a simple English text.");

        assertEquals("en", result.getLanguage());
        // TODO test prob
    }

    public void testSimpleEl() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        NNetLanguageIdentifier.Result result =
            identifier.findLanguage(" ή αρνητική αναζήτηση λέξης κλειδιού καταστήστε τις μεμονωμένες λέξεις " +
                "κλειδιά περισσότερο στοχοθετημένες με τη μετατροπή τους σε");
        assertEquals("el", result.getLanguage());
    }

    public void testSimpleNe() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        // TODO - parsing and cleaning of Nepalese is inconsistent with cld3 - investigate
        NNetLanguageIdentifier.Result result =
            identifier.findLanguage("अरू ठाऊँबाटपनि खुलेको छ यो खाता अर अरू ठाऊँबाटपनि खुलेको छ यो खाता अर ू");
        assertEquals("ne", result.getLanguage());
    }

    public void testSimpleJa() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        String text = "オリンピック大会";

        NNetLanguageIdentifier.Result result = identifier.findLanguage(text);

        assertEquals("result: " + result, "ja", result.getLanguage());
    }

    public void testGoldExamples() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        int successes = 0;
        int failures = 0;
        StringBuilder failureMessages = new StringBuilder();
        for (int i = 0; i < LanguageExamples.goldLangText.length; ++i) {
            String expected = LanguageExamples.goldLangText[i][0];
            String text = LanguageExamples.goldLangText[i][1];

            String cld3Expected = LanguageExamples.goldLangResults[i][0];
            String cld3Actual = LanguageExamples.goldLangResults[i][1];
            String cld3ProbabilityStr = LanguageExamples.goldLangResults[i][2];

            float cld3Probability = Float.parseFloat(cld3ProbabilityStr);

            NNetLanguageIdentifier.Result result =
                identifier.findLanguage(text);


            if (expected == result.getLanguage()) {
                successes++;
            } else {
                failureMessages.append("FAILURE ").append(text).append('\n');
                failureMessages.append(cld3Actual).append(" ").append(cld3Probability).append(" ")
                        .append(expected).append(" ").append(result.getLanguage()).append(" ")
                        .append((float)result.getProbability()).append('\n');
                failures++;
            }

            assertEquals(text + ":" + result.toString(), cld3Actual, result.getLanguage());
//            assertEquals(cld3Expected, cld3Probability, result.getProbability(), 0.01);
//            if (cld3Actual != result.getLanguage() || Math.abs(result.getProbability() - cld3Probability) > 0.001) {
//                System.out.println("DIFF " + cld3Actual + " " + cld3Probability + " "
//                    + expected + " " + result.getLanguage() + " " + (float)result.getProbability());
//            }

            //System.out.println(expected + " " + result.getLanguage() + " " + (float)result.getProbability());

            String errors = failureMessages.toString();
            assertEquals("", errors);
        }
    }

    public void testDebug() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        String text = "This is simple english text.";

        NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
        assertEquals("en", result.getLanguage());
    }

    public void testDifferences() throws Exception {
        NNetLanguageIdentifier identifier = new NNetLanguageIdentifier();

        {
            String text = "ـــ په ۵ رجب ۱۴۲۹ هـ ق كال دوانت وايگل ولسوالي دجمچگل په سيمه كې داشغالگرو عسكرو" +
                    " خورا ستر بيس چې دوئ ته يې خورا ستراتيژيك اهميت درلود دمجاهدينو دمسلسلو بريدونو " +
                    "له امله تخليه كړ او گڼ شمیر غنيمتونه دمجاهدينو لاس ته ورغلل .";
            NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
//            System.out.println(result.toString()); TODO
        }
        {
            String text = "4 Özmızrak, 5 Güler, 6 Osman, 7 Hersek, 8 İlyasova, 9 Erden, 10 Mahmutoğlu, 11 Savaş, 12 Korkmaz, " +
                    "13 Muhammed, 14 Köksal, 15 Aldemir, All. Ergin AtamanFIBA EuroBasket 2017";
            NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
//            System.out.println(result.toString());  TODO
        }
        {
            // Multi-sentence TODO
            String text = "Vladimir Vladimirovich Putin (Rọ́síà: Владимир Владимирович Путин , IPA [vlɐˈdʲimʲɪr vlɐˈdʲimʲɪrəvʲɪtɕ " +
                    "ˈputʲɪn]; bibi 7 October 1952) je Aare ekeji ile Russia to ji se Alakoso Agba ti Russia lowolowo lati 2008.";
            NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
//            System.out.println(result.toString());  TODO
        }
        {
            String text = "San Juan (pípè /ˌsæn ˈwɑːn/, Pípè: [san ˈhwan], lati San Juan Bautista \"Johannu Onitebomi Mimo\") " +
                    "ni oluilu ati ilutitobijulo ni Puerto Rico.";
            NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
//            System.out.println(result.toString());  TODO
        }
        {
            // Multi-sentence TODO
            String text = "Wireshark'හි ජාල දත්ත පටිගතකල ගොනුවෙහි ආකෘතිය(format) libpcap, libpcap සහ WinPcap සදහාද සහය " +
                    "දක්වන්නකි,එමනිසා එම ගොනු ආකෘතියට සහය දක්වන tcpdump and CA NetMaster මෘදුකාංග සමග දත්ත හුවමාරු කලහැක.තවද එයට " +
                    "snoop, Network General, සහ Microsoft Network Monitor මෘදුකාංග වල ග්‍රහණයන්(captures) කියවිය හැක.";
            NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
//            System.out.println(result.toString());  TODO
        }
        {
            // Multi-sentence TODO
            String text = "BeagleBoardの改版として、BeagleBoard-xMが2010年8月27日から出荷開始された。基板外形は82.55x82.55mm" +
                    "（基板穴間隔は約68.5x71.0mm）で、OMAP3530の改良版であるDM3730を搭載>している。DRAMを512MBに拡張、オンボードイーサネットポート、" +
                    "4ポートのUSBハブを持つ。フラッシュメモリは搭載されていないため、microSDカードを起動メディアとして使用する必要がある。" +
                    "xMで追加されたカ メラポートはLeopard Board社のベアボーンのカメラ用。";
            NNetLanguageIdentifier.Result result = identifier.findLanguage(text);
//            System.out.println(result.toString()); TODO
        }
    }
}
