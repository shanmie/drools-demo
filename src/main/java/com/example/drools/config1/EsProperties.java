package com.example.drools.config1;

public interface EsProperties {

    String servers = null;
    int connectTimeout = 1000;
    int socketTimeOut = 3 * 60 * 1000;
    int connectRequestTimeOut = 500;
    int maxConnectNum = 100;
    int maxConnectPerRoute = 100;
    boolean uniqueConnectTimeConfig = true;
    boolean uniqueConnectNumConfig = true;


    String getServers();

    Integer getTcpPort();

    int getConnectTimeout();

    int getSocketTimeOut();

    int getConnectRequestTimeOut();

    int getMaxConnectNum();

    int getMaxConnectPerRoute();

    boolean isUniqueConnectTimeConfig();
}
