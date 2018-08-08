package org.elasticsearch.xpack.ml.datafeed.persistence;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteAction;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetAction;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.LoggingDeprecationHandler;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.core.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.core.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.core.ml.job.persistence.ElasticsearchMappings;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static org.elasticsearch.xpack.core.ClientHelper.ML_ORIGIN;
import static org.elasticsearch.xpack.core.ClientHelper.executeAsyncWithOrigin;

public class DatafeedConfigProvider extends AbstractComponent {

    private final Client client;

    public DatafeedConfigProvider(Client client, Settings settings) {
        super(settings);
        this.client = client;
    }

    /**
     * Persist the datafeed configuration to the config index.
     * It is an error if a datafeed with the same Id already exists -
     * the config will not be overwritten.
     *
     * @param config The datafeed configuration
     * @param listener Index response listener
     */
    public void putDatafeedConfig(DatafeedConfig config, ActionListener<IndexResponse> listener) {
        try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
            XContentBuilder source = config.toXContent(builder, ToXContent.EMPTY_PARAMS);
            IndexRequest indexRequest =  client.prepareIndex(AnomalyDetectorsIndex.configIndexName(),
                    ElasticsearchMappings.DOC_TYPE, DatafeedConfig.documentId(config.getId()))
                    .setSource(source)
                    .setOpType(DocWriteRequest.OpType.CREATE)
                    .request();

            executeAsyncWithOrigin(client, ML_ORIGIN, IndexAction.INSTANCE, indexRequest, listener);

        } catch (IOException e) {
            listener.onFailure(new ElasticsearchParseException("Failed to serialise datafeed config with id [" + config.getId() + "]", e));
        }
    }

    /**
     * Get the datafeed config specified by {@code datafeedId}.
     * If the datafeed document is missing a {@code ResourceNotFoundException}
     * is returned via the listener.
     *
     * @param datafeedId The datafeed ID
     * @param datafeedConfigListener The config listener
     */
    public void getDatafeedConfig(String datafeedId, ActionListener<DatafeedConfig.Builder> datafeedConfigListener) {
        GetRequest getRequest = new GetRequest(AnomalyDetectorsIndex.configIndexName(),
                ElasticsearchMappings.DOC_TYPE, DatafeedConfig.documentId(datafeedId));
        executeAsyncWithOrigin(client, ML_ORIGIN, GetAction.INSTANCE, getRequest, new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                if (getResponse.isExists() == false) {
                    datafeedConfigListener.onFailure(ExceptionsHelper.missingDatafeedException(datafeedId));
                    return;
                }
                BytesReference source = getResponse.getSourceAsBytesRef();
                parseLenientlyFromSource(source, datafeedConfigListener);
            }
            @Override
            public void onFailure(Exception e) {
                datafeedConfigListener.onFailure(e);
            }
        });
    }

    /**
     * Delete the datafeed config document
     *
     * @param datafeedId The datafeed id
     * @param actionListener Deleted datafeed listener
     */
    public void deleteDatafeedConfig(String datafeedId,  ActionListener<DeleteResponse> actionListener) {
        DeleteRequest request = new DeleteRequest(AnomalyDetectorsIndex.configIndexName(),
                ElasticsearchMappings.DOC_TYPE, DatafeedConfig.documentId(datafeedId));
        executeAsyncWithOrigin(client, ML_ORIGIN, DeleteAction.INSTANCE, request, new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
                if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
                    actionListener.onFailure(ExceptionsHelper.missingDatafeedException(datafeedId));
                    return;
                }
                assert deleteResponse.getResult() == DocWriteResponse.Result.DELETED;
                actionListener.onResponse(deleteResponse);
            }
            @Override
            public void onFailure(Exception e) {
                actionListener.onFailure(e);
            }
        });
    }


    /**
     * Get the datafeed config and update it by applying {@code configUpdater}
     * then index the modified config setting the version in the request.
     *
     * @param datafeedId The Id of the datafeed to update
     * @param configUpdater Updating function
     * @param updatedConfigListener Updated datafeed config listener
     */
    public void updateDatefeedConfig(String datafeedId, Function<DatafeedConfig.Builder, DatafeedConfig> configUpdater,
                          ActionListener<DatafeedConfig> updatedConfigListener) {
        GetRequest getRequest = new GetRequest(AnomalyDetectorsIndex.configIndexName(),
                ElasticsearchMappings.DOC_TYPE, DatafeedConfig.documentId(datafeedId));

        executeAsyncWithOrigin(client, ML_ORIGIN, GetAction.INSTANCE, getRequest, new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                if (getResponse.isExists() == false) {
                    updatedConfigListener.onFailure(ExceptionsHelper.missingDatafeedException(datafeedId));
                    return;
                }
                long version = getResponse.getVersion();
                try {
                    BytesReference source = getResponse.getSourceAsBytesRef();
                    DatafeedConfig.Builder configBulder = parseLenientlyFromSource(source);
                    DatafeedConfig updatedConfig = configUpdater.apply(configBulder);
                    try (XContentBuilder builder = XContentFactory.jsonBuilder()) {
                        XContentBuilder updatedSource = updatedConfig.toXContent(builder, ToXContent.EMPTY_PARAMS);
                        IndexRequest indexRequest = client.prepareIndex(AnomalyDetectorsIndex.configIndexName(),
                                ElasticsearchMappings.DOC_TYPE, DatafeedConfig.documentId(updatedConfig.getId()))
                                .setSource(updatedSource)
                                .setVersion(version)
                                .request();

                        executeAsyncWithOrigin(client, ML_ORIGIN, IndexAction.INSTANCE, indexRequest, ActionListener.wrap(
                                indexResponse -> {
                                    assert indexResponse.getResult() == DocWriteResponse.Result.UPDATED;
                                    updatedConfigListener.onResponse(updatedConfig);
                                },
                                updatedConfigListener::onFailure
                        ));
                    } catch (IOException e) {
                        updatedConfigListener.onFailure(
                                new ElasticsearchParseException("Failed to serialise datafeed config with id [" + datafeedId + "]", e));
                    }
                } catch (IOException e) {
                    updatedConfigListener.onFailure(new ElasticsearchParseException("failed to parse " + getResponse.getType(), e));
                }
            }
            @Override
            public void onFailure(Exception e) {
                updatedConfigListener.onFailure(e);
            }
        });
    }

    private void parseLenientlyFromSource(BytesReference source, ActionListener<DatafeedConfig.Builder> datafeedConfigListener)  {
        try (InputStream stream = source.streamInput();
             XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                     .createParser(NamedXContentRegistry.EMPTY, LoggingDeprecationHandler.INSTANCE, stream)) {
            datafeedConfigListener.onResponse(DatafeedConfig.LENIENT_PARSER.apply(parser, null));
        } catch (Exception e) {
            datafeedConfigListener.onFailure(e);
        }
    }

    private DatafeedConfig.Builder parseLenientlyFromSource(BytesReference source) throws IOException {
        try (InputStream stream = source.streamInput();
             XContentParser parser = XContentFactory.xContent(XContentType.JSON)
                     .createParser(NamedXContentRegistry.EMPTY, LoggingDeprecationHandler.INSTANCE, stream)) {
            return DatafeedConfig.LENIENT_PARSER.apply(parser, null);
        }
    }
}
