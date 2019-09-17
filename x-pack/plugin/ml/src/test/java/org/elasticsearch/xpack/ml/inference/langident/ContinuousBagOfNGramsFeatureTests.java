/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.langident;

import com.google.common.base.Charsets;
import org.elasticsearch.test.ESTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class ContinuousBagOfNGramsFeatureTests extends ESTestCase {

    private static ArrayList<Long> readFileToLongSet(String path) {
        ArrayList<Long> arrayList = new ArrayList<>();

        Scanner s = new Scanner(NNetParams.class.getResourceAsStream(path), Charsets.UTF_8);
        // Read first entry (array size) as integer
        int size = s.nextInt();
        for (int i = 0; i < size; i++) {
            arrayList.add(s.nextLong());
        }
        return arrayList;
    }

    private static void simpleTest(String text, String file, int nGramSize, int idDimension) throws Exception {
        FeatureVector result = ContinuousBagOfNGramsFeature.getNGramFeatureValue(text, nGramSize, idDimension);

        // Compare sorted for now
        ArrayList<Long> expectedSet = readFileToLongSet(file);
        ArrayList<Long> actualSet = new ArrayList<>();
        for (int i = 0; i < result.size(); ++i) {
            actualSet.add(result.getValueAt(i));
        }

        // sort for comparison
        Collections.sort(expectedSet);
        Collections.sort(actualSet);

        assertEquals(expectedSet, actualSet);
    }

    public void testSimpleEvaluate() throws Exception {
        // NOTE: this test uses the stripped and clean strings from cld3

        // AF
        simpleTest(" dit is n kort stukkie van die teks wat gebruik sal word vir die " +
                "toets van die akkuraatheid van die nuwe benadering ",
            "/1-continuous-bag-of-ngrams-1000-2",
            2, 1000);
        simpleTest(" dit is n kort stukkie van die teks wat gebruik sal word vir die " +
                "toets van die akkuraatheid van die nuwe benadering ",
            "/1-continuous-bag-of-ngrams-5000-4",
            4, 5000);
        simpleTest(" dit is n kort stukkie van die teks wat gebruik sal word vir die " +
                "toets van die akkuraatheid van die nuwe benadering ",
            "/1-continuous-bag-of-ngrams-5000-3",
            3, 5000);
        simpleTest(" dit is n kort stukkie van die teks wat gebruik sal word vir die " +
                "toets van die akkuraatheid van die nuwe benadering ",
            "/1-continuous-bag-of-ngrams-100-1",
            1, 100);

        // KO
        simpleTest(" 개별적으로 리포트 액세스 권한을 부여할 수 있습니다 액세스 권한 " +
                "부여사용자에게 프로필 리포트에 액세스할 수 있는 권한을 " +
                "부여하시려면 가용 프로필 상자에서 프로필 이름을 선택한 다음 ",
            "/44-continuous-bag-of-ngrams-1000-2",
            2, 1000);
        simpleTest(" 개별적으로 리포트 액세스 권한을 부여할 수 있습니다 액세스 권한 " +
                "부여사용자에게 프로필 리포트에 액세스할 수 있는 권한을 " +
                "부여하시려면 가용 프로필 상자에서 프로필 이름을 선택한 다음 ",
            "/44-continuous-bag-of-ngrams-5000-4",
            4, 5000);
        simpleTest(" 개별적으로 리포트 액세스 권한을 부여할 수 있습니다 액세스 권한 " +
                "부여사용자에게 프로필 리포트에 액세스할 수 있는 권한을 " +
                "부여하시려면 가용 프로필 상자에서 프로필 이름을 선택한 다음 ",
            "/44-continuous-bag-of-ngrams-5000-3",
            3, 5000);
        simpleTest(" 개별적으로 리포트 액세스 권한을 부여할 수 있습니다 액세스 권한 " +
                "부여사용자에게 프로필 리포트에 액세스할 수 있는 권한을 " +
                "부여하시려면 가용 프로필 상자에서 프로필 이름을 선택한 다음 ",
            "/44-continuous-bag-of-ngrams-100-1",
            1, 100);
    }
}
