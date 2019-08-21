package org.elasticsearch.xpack.ml.inference.tree.xgboost;

import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.inference.tree.Tree;
import org.elasticsearch.xpack.ml.inference.tree.TreeEnsembleModel;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

public class XgBoostJsonParserTests extends ESTestCase {
    public void testSimpleParse() throws IOException {
        String json =
                "{ \"nodeid\": 0, \"depth\": 0, \"split\": \"f1\", \"split_condition\": -9.53674316e-07, \"yes\": 1, \"no\": 2, \"missing\": 1, \"children\": [\n" +
                "    { \"nodeid\": 1, \"depth\": 1, \"split\": \"f2\", \"split_condition\": -9.53674316e-07, \"yes\": 3, \"no\": 4, \"missing\": 3, \"children\": [\n" +
                "      { \"nodeid\": 3, \"leaf\": 0.78471756 },\n" +
                "      { \"nodeid\": 4, \"leaf\": -0.968530357 }\n" +
                "    ]},\n" +
                "    { \"nodeid\": 2, \"leaf\": -6.23624468 }\n" +
                "  ]}";


        Map<String, Integer> featureMap = new HashMap<>();
        featureMap.put("f1", 0);
        featureMap.put("f2", 1);

        TreeEnsembleModel model;
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, json)) {
            Map<String, Object> map = parser.map();

            model = XgBoostJsonParser.parse(Collections.singletonList(map), featureMap);

            Map<String, Double> doc = new HashMap<>();
            doc.put("f1", 0.1);
            doc.put("f2", 0.1);
            List<List<Tree.Node>> trace = model.trace(doc);
            System.out.println(trace);

            double prediction = model.predict(doc);
            System.out.println(prediction);
        }
    }

    @SuppressWarnings("unchecked")
    public void testParse() throws IOException {
        String json = "{\"ensemble\": [\n" +
                "  { \"nodeid\": 0, \"depth\": 0, \"split\": \"f0\", \"split_condition\": 18.3800011, \"yes\": 1, \"no\": 2, \"missing\": 1, \"children\": [\n" +
                "    { \"nodeid\": 1, \"leaf\": 470.438141 },\n" +
                "    { \"nodeid\": 2, \"leaf\": 441.225525 }\n" +
                "  ]},\n" +
                "  { \"nodeid\": 0, \"depth\": 0, \"split\": \"f0\", \"split_condition\": 10.9049997, \"yes\": 1, \"no\": 2, \"missing\": 1, \"children\": [\n" +
                "    { \"nodeid\": 1, \"depth\": 1, \"split\": \"f0\", \"split_condition\": 8.65499973, \"yes\": 3, \"no\": 4, \"missing\": 3, \"children\": [\n" +
                "      { \"nodeid\": 3, \"depth\": 2, \"split\": \"f0\", \"split_condition\": 7.04500008, \"yes\": 7, \"no\": 8, \"missing\": 7, \"children\": [\n" +
                "        { \"nodeid\": 7, \"depth\": 3, \"split\": \"f0\", \"split_condition\": 4.77499962, \"yes\": 15, \"no\": 16, \"missing\": 15, \"children\": [\n" +
                "          { \"nodeid\": 15, \"leaf\": 17.8773727 },\n" +
                "          { \"nodeid\": 16, \"depth\": 4, \"split\": \"f3\", \"split_condition\": 77.4000015, \"yes\": 31, \"no\": 32, \"missing\": 31, \"children\": [\n" +
                "            { \"nodeid\": 31, \"leaf\": 16.7193699 },\n" +
                "            { \"nodeid\": 32, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 78.9400024, \"yes\": 61, \"no\": 62, \"missing\": 61, \"children\": [\n" +
                "              { \"nodeid\": 61, \"leaf\": 8.74667072 },\n" +
                "              { \"nodeid\": 62, \"leaf\": 13.6302109 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]},\n" +
                "        { \"nodeid\": 8, \"depth\": 3, \"split\": \"f2\", \"split_condition\": 1022.72498, \"yes\": 17, \"no\": 18, \"missing\": 17, \"children\": [\n" +
                "          { \"nodeid\": 17, \"depth\": 4, \"split\": \"f1\", \"split_condition\": 57.9249992, \"yes\": 33, \"no\": 34, \"missing\": 33, \"children\": [\n" +
                "            { \"nodeid\": 33, \"depth\": 5, \"split\": \"f0\", \"split_condition\": 8.03999996, \"yes\": 63, \"no\": 64, \"missing\": 63, \"children\": [\n" +
                "              { \"nodeid\": 63, \"leaf\": 12.3259125 },\n" +
                "              { \"nodeid\": 64, \"leaf\": 10.1782122 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 34, \"leaf\": 1.59123743 }\n" +
                "          ]},\n" +
                "          { \"nodeid\": 18, \"depth\": 4, \"split\": \"f3\", \"split_condition\": 89.6999969, \"yes\": 35, \"no\": 36, \"missing\": 35, \"children\": [\n" +
                "            { \"nodeid\": 35, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 47.3199997, \"yes\": 65, \"no\": 66, \"missing\": 65, \"children\": [\n" +
                "              { \"nodeid\": 65, \"leaf\": 9.94041252 },\n" +
                "              { \"nodeid\": 66, \"leaf\": 1.25593567 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 36, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 41.5400009, \"yes\": 67, \"no\": 68, \"missing\": 67, \"children\": [\n" +
                "              { \"nodeid\": 67, \"leaf\": 5.41526508 },\n" +
                "              { \"nodeid\": 68, \"leaf\": -1.86407471 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]}\n" +
                "      ]},\n" +
                "      { \"nodeid\": 4, \"depth\": 2, \"split\": \"f3\", \"split_condition\": 92.9850006, \"yes\": 9, \"no\": 10, \"missing\": 9, \"children\": [\n" +
                "        { \"nodeid\": 9, \"depth\": 3, \"split\": \"f1\", \"split_condition\": 48.8600006, \"yes\": 19, \"no\": 20, \"missing\": 19, \"children\": [\n" +
                "          { \"nodeid\": 19, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 9.78999996, \"yes\": 37, \"no\": 38, \"missing\": 37, \"children\": [\n" +
                "            { \"nodeid\": 37, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 62.8600006, \"yes\": 69, \"no\": 70, \"missing\": 69, \"children\": [\n" +
                "              { \"nodeid\": 69, \"leaf\": 2.36037445 },\n" +
                "              { \"nodeid\": 70, \"leaf\": 7.76275396 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 38, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 38.2799988, \"yes\": 71, \"no\": 72, \"missing\": 71, \"children\": [\n" +
                "              { \"nodeid\": 71, \"leaf\": 3.42786431 },\n" +
                "              { \"nodeid\": 72, \"leaf\": 6.49917078 }\n" +
                "            ]}\n" +
                "          ]},\n" +
                "          { \"nodeid\": 20, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 10.3050003, \"yes\": 39, \"no\": 40, \"missing\": 39, \"children\": [\n" +
                "            { \"nodeid\": 39, \"depth\": 5, \"split\": \"f0\", \"split_condition\": 9.76499939, \"yes\": 73, \"no\": 74, \"missing\": 73, \"children\": [\n" +
                "              { \"nodeid\": 73, \"leaf\": 1.2359314 },\n" +
                "              { \"nodeid\": 74, \"leaf\": 0.0513916016 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 40, \"leaf\": -2.12875366 }\n" +
                "          ]}\n" +
                "        ]},\n" +
                "        { \"nodeid\": 10, \"depth\": 3, \"split\": \"f1\", \"split_condition\": 41.7750015, \"yes\": 21, \"no\": 22, \"missing\": 21, \"children\": [\n" +
                "          { \"nodeid\": 21, \"depth\": 4, \"split\": \"f1\", \"split_condition\": 41.5350037, \"yes\": 41, \"no\": 42, \"missing\": 41, \"children\": [\n" +
                "            { \"nodeid\": 41, \"depth\": 5, \"split\": \"f2\", \"split_condition\": 1017.32001, \"yes\": 75, \"no\": 76, \"missing\": 75, \"children\": [\n" +
                "              { \"nodeid\": 75, \"leaf\": 3.39938235 },\n" +
                "              { \"nodeid\": 76, \"leaf\": -0.816912234 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 42, \"depth\": 5, \"split\": \"f2\", \"split_condition\": 1021.21497, \"yes\": 77, \"no\": 78, \"missing\": 77, \"children\": [\n" +
                "              { \"nodeid\": 77, \"leaf\": -6.331038 },\n" +
                "              { \"nodeid\": 78, \"leaf\": -1.37406921 }\n" +
                "            ]}\n" +
                "          ]},\n" +
                "          { \"nodeid\": 22, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 9.25, \"yes\": 43, \"no\": 44, \"missing\": 43, \"children\": [\n" +
                "            { \"nodeid\": 43, \"leaf\": 7.57445431 },\n" +
                "            { \"nodeid\": 44, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 99.7200012, \"yes\": 79, \"no\": 80, \"missing\": 79, \"children\": [\n" +
                "              { \"nodeid\": 79, \"leaf\": 3.53175259 },\n" +
                "              { \"nodeid\": 80, \"leaf\": 0.886398315 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]}\n" +
                "      ]}\n" +
                "    ]},\n" +
                "    { \"nodeid\": 2, \"depth\": 1, \"split\": \"f1\", \"split_condition\": 66.1500015, \"yes\": 5, \"no\": 6, \"missing\": 5, \"children\": [\n" +
                "      { \"nodeid\": 5, \"depth\": 2, \"split\": \"f0\", \"split_condition\": 18.3850002, \"yes\": 11, \"no\": 12, \"missing\": 11, \"children\": [\n" +
                "        { \"nodeid\": 11, \"depth\": 3, \"split\": \"f0\", \"split_condition\": 14.4449997, \"yes\": 23, \"no\": 24, \"missing\": 23, \"children\": [\n" +
                "          { \"nodeid\": 23, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 12.6049995, \"yes\": 45, \"no\": 46, \"missing\": 45, \"children\": [\n" +
                "            { \"nodeid\": 45, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 88.3000031, \"yes\": 81, \"no\": 82, \"missing\": 81, \"children\": [\n" +
                "              { \"nodeid\": 81, \"leaf\": 2.6010797 },\n" +
                "              { \"nodeid\": 82, \"leaf\": -0.530121922 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 46, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 40.9049988, \"yes\": 83, \"no\": 84, \"missing\": 83, \"children\": [\n" +
                "              { \"nodeid\": 83, \"leaf\": -1.30897391 },\n" +
                "              { \"nodeid\": 84, \"leaf\": -3.96863937 }\n" +
                "            ]}\n" +
                "          ]},\n" +
                "          { \"nodeid\": 24, \"depth\": 4, \"split\": \"f1\", \"split_condition\": 45.0049973, \"yes\": 47, \"no\": 48, \"missing\": 47, \"children\": [\n" +
                "            { \"nodeid\": 47, \"depth\": 5, \"split\": \"f0\", \"split_condition\": 15.4849997, \"yes\": 85, \"no\": 86, \"missing\": 85, \"children\": [\n" +
                "              { \"nodeid\": 85, \"leaf\": -5.40670681 },\n" +
                "              { \"nodeid\": 86, \"leaf\": -8.27983284 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 48, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 56.6899986, \"yes\": 87, \"no\": 88, \"missing\": 87, \"children\": [\n" +
                "              { \"nodeid\": 87, \"leaf\": -12.0328913 },\n" +
                "              { \"nodeid\": 88, \"leaf\": -17.0602112 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]},\n" +
                "        { \"nodeid\": 12, \"depth\": 3, \"split\": \"f0\", \"split_condition\": 21.5450001, \"yes\": 25, \"no\": 26, \"missing\": 25, \"children\": [\n" +
                "          { \"nodeid\": 25, \"depth\": 4, \"split\": \"f1\", \"split_condition\": 45.7399979, \"yes\": 49, \"no\": 50, \"missing\": 49, \"children\": [\n" +
                "            { \"nodeid\": 49, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 60.5650024, \"yes\": 89, \"no\": 90, \"missing\": 89, \"children\": [\n" +
                "              { \"nodeid\": 89, \"leaf\": 18.9551449 },\n" +
                "              { \"nodeid\": 90, \"leaf\": 13.8164215 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 50, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 58.1699982, \"yes\": 91, \"no\": 92, \"missing\": 91, \"children\": [\n" +
                "              { \"nodeid\": 91, \"leaf\": 10.819478 },\n" +
                "              { \"nodeid\": 92, \"leaf\": 7.62072706 }\n" +
                "            ]}\n" +
                "          ]},\n" +
                "          { \"nodeid\": 26, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 26.4650002, \"yes\": 51, \"no\": 52, \"missing\": 51, \"children\": [\n" +
                "            { \"nodeid\": 51, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 55.7350006, \"yes\": 93, \"no\": 94, \"missing\": 93, \"children\": [\n" +
                "              { \"nodeid\": 93, \"leaf\": 5.94795942 },\n" +
                "              { \"nodeid\": 94, \"leaf\": 2.39619088 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 52, \"depth\": 5, \"split\": \"f2\", \"split_condition\": 1007.45496, \"yes\": 95, \"no\": 96, \"missing\": 95, \"children\": [\n" +
                "              { \"nodeid\": 95, \"leaf\": -5.02249479 },\n" +
                "              { \"nodeid\": 96, \"leaf\": -0.807426214 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]}\n" +
                "      ]},\n" +
                "      { \"nodeid\": 6, \"depth\": 2, \"split\": \"f0\", \"split_condition\": 25.2750015, \"yes\": 13, \"no\": 14, \"missing\": 13, \"children\": [\n" +
                "        { \"nodeid\": 13, \"depth\": 3, \"split\": \"f0\", \"split_condition\": 18.3549995, \"yes\": 27, \"no\": 28, \"missing\": 27, \"children\": [\n" +
                "          { \"nodeid\": 27, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 14.4949999, \"yes\": 53, \"no\": 54, \"missing\": 53, \"children\": [\n" +
                "            { \"nodeid\": 53, \"leaf\": -6.55451679 },\n" +
                "            { \"nodeid\": 54, \"leaf\": -16.1782494 }\n" +
                "          ]},\n" +
                "          { \"nodeid\": 28, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 22.5400009, \"yes\": 55, \"no\": 56, \"missing\": 55, \"children\": [\n" +
                "            { \"nodeid\": 55, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 69.0449982, \"yes\": 97, \"no\": 98, \"missing\": 97, \"children\": [\n" +
                "              { \"nodeid\": 97, \"leaf\": 5.74967909 },\n" +
                "              { \"nodeid\": 98, \"leaf\": 0.178715631 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 56, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 68.5200043, \"yes\": 99, \"no\": 100, \"missing\": 99, \"children\": [\n" +
                "              { \"nodeid\": 99, \"leaf\": 0.774093628 },\n" +
                "              { \"nodeid\": 100, \"leaf\": -3.68317914 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]},\n" +
                "        { \"nodeid\": 14, \"depth\": 3, \"split\": \"f2\", \"split_condition\": 1008.78003, \"yes\": 29, \"no\": 30, \"missing\": 29, \"children\": [\n" +
                "          { \"nodeid\": 29, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 26.3549995, \"yes\": 57, \"no\": 58, \"missing\": 57, \"children\": [\n" +
                "            { \"nodeid\": 57, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 75.3950043, \"yes\": 101, \"no\": 102, \"missing\": 101, \"children\": [\n" +
                "              { \"nodeid\": 101, \"leaf\": -3.79112887 },\n" +
                "              { \"nodeid\": 102, \"leaf\": -7.66311884 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 58, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 73.7200012, \"yes\": 103, \"no\": 104, \"missing\": 103, \"children\": [\n" +
                "              { \"nodeid\": 103, \"leaf\": -9.83749485 },\n" +
                "              { \"nodeid\": 104, \"leaf\": -7.75816107 }\n" +
                "            ]}\n" +
                "          ]},\n" +
                "          { \"nodeid\": 30, \"depth\": 4, \"split\": \"f0\", \"split_condition\": 29.2350006, \"yes\": 59, \"no\": 60, \"missing\": 59, \"children\": [\n" +
                "            { \"nodeid\": 59, \"depth\": 5, \"split\": \"f3\", \"split_condition\": 71.4450073, \"yes\": 105, \"no\": 106, \"missing\": 105, \"children\": [\n" +
                "              { \"nodeid\": 105, \"leaf\": -4.53095436 },\n" +
                "              { \"nodeid\": 106, \"leaf\": -7.3271451 }\n" +
                "            ]},\n" +
                "            { \"nodeid\": 60, \"depth\": 5, \"split\": \"f1\", \"split_condition\": 67.8099976, \"yes\": 107, \"no\": 108, \"missing\": 107, \"children\": [\n" +
                "              { \"nodeid\": 107, \"leaf\": -6.15153503 },\n" +
                "              { \"nodeid\": 108, \"leaf\": -8.42399311 }\n" +
                "            ]}\n" +
                "          ]}\n" +
                "        ]}\n" +
                "      ]}\n" +
                "    ]}\n" +
                "  ]}\n" +
                "]" +
                "}";


        Map<String, Integer> featureMap = new HashMap<>();
        featureMap.put("f0", 0);
        featureMap.put("f1", 1);
        featureMap.put("f2", 2);
        featureMap.put("f3", 3);

        TreeEnsembleModel model;
        try (XContentParser parser = createParser(JsonXContent.jsonXContent, json)) {
            Map<String, Object> map = parser.map();

            List<Map<String, Object>> ensemble = (List<Map<String, Object>>) map.get("ensemble");

            model = XgBoostJsonParser.parse(ensemble, featureMap);

            assertEquals(2, model.numTrees());

//            List<Integer> missingNodes = model.checkForNull();
//            System.out.println(missingNodes);
//            assertThat(missingNodes, hasSize(0));


            Map<String, Double> doc = new HashMap<>();
            doc.put("f0", 10.36);
            doc.put("f1", 43.67);
            doc.put("f2", 1012.1);
            doc.put("f3", 77.04);
            List<List<Tree.Node>> trace = model.trace(doc);
            System.out.println(trace);
            logger.info(trace);

            double prediction = model.predict(doc);
            System.out.println(prediction);
            logger.info("prediction: " + prediction);
            assertThat(prediction, is(greaterThan(450.0)));
            assertThat(prediction, is(lessThan(500.0)));
        }
    }
}
