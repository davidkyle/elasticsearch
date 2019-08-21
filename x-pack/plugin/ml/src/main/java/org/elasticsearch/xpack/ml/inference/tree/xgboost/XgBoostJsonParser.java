package org.elasticsearch.xpack.ml.inference.tree.xgboost;

import org.elasticsearch.xpack.ml.inference.tree.Tree;
import org.elasticsearch.xpack.ml.inference.tree.TreeEnsembleModel;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class XgBoostJsonParser {
    private static String NODE_ID = "nodeid";
    private static String LEAF = "leaf";
    private static String SPLIT = "split";
    private static String SPLIT_CONDITION = "split_condition";
    private static String CHILDREN = "children";

//    private static Logger logger = LogManager.getLogger(XgBoostJsonParser.class);


    public static TreeEnsembleModel parse(List<Map<String, Object>> json, Map<String, Integer> featureMap) throws IOException {

        TreeEnsembleModel.ModelBuilder modelBuilder = TreeEnsembleModel.modelBuilder(featureMap);

        for (Map<String, Object> treeMap : json) {
            Tree.TreeBuilder treeBuilder = Tree.TreeBuilder.newTreeBuilder();
            parseTree(treeMap, featureMap, treeBuilder);
            modelBuilder.addTree(treeBuilder.build());
        }

        return modelBuilder.build();
    }


    @SuppressWarnings("unchecked")
    private static void parseTree(Map<String, Object> treeMap, Map<String, Integer> featureMap,
                                  Tree.TreeBuilder treeBuilder) throws IOException {

        if (treeMap.containsKey(NODE_ID) == false) {
            throw new IOException("invalid object does not contain a nodeid");
        }

        Integer nodeId = (Integer)treeMap.get(NODE_ID);

        boolean isLeaf = treeMap.containsKey(LEAF);
        if (isLeaf) {
            Double value = (Double) treeMap.get(LEAF);
            treeBuilder.addLeaf(nodeId, value);
        } else {
            List<Map<String, Object>> children = (List<Map<String, Object>>) treeMap.get(CHILDREN);
            if (children == null) {
                throw new IOException("none leaf node has no children");
            }

            if (children.size() != 2) {
                throw new IOException("Node must have exactly 2 children, got " + children.size());
            }

            String feature = (String)treeMap.get(SPLIT);
            if (feature == null) {
                throw new IOException("missing feature for node [" + nodeId + "]");
            }

            Integer featureIndex = featureMap.get(feature);
            if (featureIndex == null) {
                throw new IOException("feature [" + feature + "] not found in the feature map");
            }
            Double threshold = (Double)treeMap.get(SPLIT_CONDITION);
            if (threshold == null) {
                throw new IOException("field [SPLIT_CONDITION] in node [" + nodeId + "] is not a double");
            }

            treeBuilder.addJunction(nodeId, featureIndex, true, threshold);

            for (Map<String, Object> child : children) {
                parseTree(child, featureMap, treeBuilder);
            }
        }
    }
}
