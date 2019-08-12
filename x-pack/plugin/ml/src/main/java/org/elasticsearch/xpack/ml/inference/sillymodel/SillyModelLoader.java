/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.ml.inference.sillymodel;

import org.elasticsearch.client.Client;
import org.elasticsearch.xpack.ml.inference.AysncModel.AsyncModelLoader;

public class SillyModelLoader extends AsyncModelLoader {

    public SillyModelLoader(Client client) {
        super(client);
    }
}
