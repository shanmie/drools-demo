package com.example.drools.config2;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


import java.util.Arrays;
import java.util.Objects;

@Slf4j
@Component
@Configuration
public class ElasticsearchRestClientConfiguration {
    private static final int ADDRESS_LENGTH = 2;
    private static final String HTTP_SCHEME = "http";

    @Value("${elasticsearch.ip}")
    String[] ipAddress;

    @Bean
    @Order(1)
    public RestClientBuilder restClientBuilder() {
        HttpHost[] hosts = Arrays.stream(ipAddress)
                .map(this::makeHttpHost)
                .filter(Objects::nonNull)
                .toArray(HttpHost[]::new);
        log.debug("hosts:{}", Arrays.toString(hosts));
        System.out.println("host:---------->"+Arrays.toString(hosts));
        return RestClient.builder(hosts);
    }


    @Bean
    public RestHighLevelClient client(@Autowired RestClientBuilder restClientBuilder) {
        restClientBuilder.setMaxRetryTimeoutMillis(60000);
        restClientBuilder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setConnectTimeout(1000);
            requestConfigBuilder.setSocketTimeout(30000);
            requestConfigBuilder.setConnectionRequestTimeout(500);
            return requestConfigBuilder;
        });
        // 异步httpclient连接数配置
        restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setMaxConnTotal(100);
            httpClientBuilder.setMaxConnPerRoute(100);
            return httpClientBuilder;
        });
        return new RestHighLevelClient(restClientBuilder);
    }



    private HttpHost makeHttpHost(String s) {
        assert StringUtils.isNotEmpty(s);
        String[] address = s.split(":");
        if (address.length == ADDRESS_LENGTH) {
            String ip = address[0];
            int port = Integer.parseInt(address[1]);
            return new HttpHost(ip, port, HTTP_SCHEME);
        } else {
            return null;
        }
    }
}
