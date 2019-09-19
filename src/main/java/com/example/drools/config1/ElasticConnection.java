package com.example.drools.config1;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.*;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.io.IOException;
import java.util.List;

/**
 * @author: zhengjianjian
 * email: 865524591@qq.com
 * <p>
 * Date: 2018-08-07 上午 11:09
 * Description:
 * Copyright(©) 2018 by zhengjianjian.
 */
@Slf4j
public class ElasticConnection {
    public static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }
    private static final String DEFAULT_SCHEMA = "http";

    private static final String ES_SERVER = "127.0.0.1:9200";

    private static final int CONNECT_TIME_OUT = 1000;
    private static final int SOCKET_TIME_OUT = 30000;
    private static final int CONNECTION_REQUEST_TIME_OUT = 500;

    private static final int MAX_CONNECT_NUM = 100;
    private static final int MAX_CONNECT_PER_ROUTE = 100;

    private static boolean UNIQUE_CONNECT_TIME_CONFIG = false;
    private static boolean UNIQUE_CONNECT_NUM_CONFIG = true;

    private static volatile ElasticConnection analyseElasticConnection;

    private static volatile ElasticConnection smsElasticConnection;

    private final RestHighLevelClient restHighLevelClient;

    private final TransportClient transportClient;

    private ElasticConnection(RestHighLevelClient restHighLevelClient, TransportClient transportClient) {
        this.restHighLevelClient = restHighLevelClient;
        this.transportClient = transportClient;
    }

    public RestHighLevelClient getRestHighLevelClient() {
        return restHighLevelClient;
    }

    public TransportClient getTransportClient() {
        return transportClient;
    }

    public static ElasticConnection getConnection(EsProperties esProperties) {
        if (analyseElasticConnection == null) {
            synchronized (ElasticConnection.class) {
                if (analyseElasticConnection == null) {
                    try {
                        String servers = esProperties != null ? esProperties.getServers() : ES_SERVER;
                        int connectTimeout = esProperties != null ? esProperties.getConnectTimeout() : CONNECT_TIME_OUT;
                        int socketTimeOut = esProperties != null ? esProperties.getSocketTimeOut() : SOCKET_TIME_OUT;
                        int connectRequestTimeOut = esProperties != null ? esProperties.getConnectRequestTimeOut() : CONNECTION_REQUEST_TIME_OUT;
                        int maxConnectNum = esProperties != null ? esProperties.getMaxConnectNum() : MAX_CONNECT_NUM;
                        int maxConnectPerRoute = esProperties != null ? esProperties.getMaxConnectPerRoute() : MAX_CONNECT_PER_ROUTE;
                        boolean isUniqueConnectTimeConfig = esProperties != null ? esProperties.isUniqueConnectTimeConfig() : UNIQUE_CONNECT_TIME_CONFIG;
                        boolean isUniqueConnectNumConfig = esProperties != null ? esProperties.isUniqueConnectTimeConfig() : UNIQUE_CONNECT_NUM_CONFIG;
                        //172.20.0.57:9200,172.20.0.58:9200,172.20.0.59:9200
                        if (Strings.isNullOrEmpty(servers)) {
                            throw new Exception("elasticsearch.server配置为空");
                        } else {
                            Settings settings = Settings.builder()
                                    .put("client.transport.ignore_cluster_name", true).build();
                            TransportClient client = new PreBuiltTransportClient(settings);
                            List<String> hosts = Splitter.on(",").splitToList(servers);
                            int size = hosts.size();
                            List<HttpHost> httpHosts = Lists.newArrayListWithExpectedSize(size);
                            hosts.forEach(host -> {
                                //172.20.0.57:9200
                                List<String> sp = Splitter.on(":").splitToList(host);
                                String ip = sp.get(0);
                                int port = Integer.valueOf(sp.get(1));
                                HttpHost httpHost = new HttpHost(ip, port, DEFAULT_SCHEMA);
                                httpHosts.add(httpHost);
                                /*try {
                                    client.addTransportAddress(new TransportAddress(InetAddress.getByName(ip), esProperties.getTcpPort()));
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                    log.error("TransportClient UnknownHostException",e);
                                }*/
                            });
                            RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[size]));
                            builder.setMaxRetryTimeoutMillis(socketTimeOut);
                            if (isUniqueConnectTimeConfig) {
                                builder.setRequestConfigCallback(config -> {
                                    config.setConnectTimeout(connectTimeout);
                                    config.setSocketTimeout(socketTimeOut);
                                    config.setConnectionRequestTimeout(connectRequestTimeOut);
                                    return config;
                                });
                            }
                            if (isUniqueConnectNumConfig) {
                                builder.setHttpClientConfigCallback(config -> {
                                    config.setMaxConnTotal(maxConnectNum);
                                    config.setMaxConnPerRoute(maxConnectPerRoute);
                                    return config;
                                });
                            }
                            RestHighLevelClient highLevelClient = new RestHighLevelClient(builder);
                            analyseElasticConnection = new ElasticConnection(highLevelClient,client);
                            Runtime.getRuntime().addShutdownHook(new Thread(() -> analyseElasticConnection.close()));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return analyseElasticConnection;
    }



    public void close() {
        if (restHighLevelClient != null) {
            try {
                restHighLevelClient.close();
            } catch (IOException e) {
                log.error("关闭es客户端异常", e);
            }
        }
        if (transportClient != null) {
            transportClient.close();
        }
    }

}
