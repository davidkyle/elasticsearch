package org.elasticsearch.xpack.ml.inference.tree;

import org.elasticsearch.ingest.IngestDocument;
import org.elasticsearch.xpack.ml.inference.Model;

public class TreeModel implements Model {

    private String targetFieldName;
    private TreeEnsembleModel ensemble;


    TreeModel(TreeEnsembleModel ensemble, String targetFieldName) {
        this.ensemble = ensemble;
        this.targetFieldName = targetFieldName;
    }

    @Override
    public IngestDocument infer(IngestDocument document) {
        Double prediction = ensemble.predictFromDoc(document.getSourceAndMetadata());
        document.setFieldValue(targetFieldName, prediction);
        return document;
    }
}
