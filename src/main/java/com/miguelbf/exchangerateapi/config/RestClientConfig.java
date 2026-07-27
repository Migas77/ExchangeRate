package com.miguelbf.exchangerateapi.config;

import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;


@Configuration
public class RestClientConfig {

    private final ExchangeRatesClientProperties exchangeRatesClientProperties;

    public RestClientConfig(ExchangeRatesClientProperties exchangeRatesClientProperties) {
        this.exchangeRatesClientProperties = exchangeRatesClientProperties;
    }

    private static DefaultUriBuilderFactory createAccessKeyUriBuilderFactory(String baseUrl, String accessKey) {
        return new DefaultUriBuilderFactory(baseUrl) {
            @Override
            public @NonNull UriBuilder uriString(@NonNull String uriTemplate) {
                return super.uriString(uriTemplate).queryParam("access_key", accessKey);
            }

            @Override
            public @NonNull UriBuilder builder() {
                return super.builder().queryParam("access_key", accessKey);
            }
        };
    }

    @Bean
    public RestClient getExchangeRatesRestClient(ObjectProvider<RestClient.Builder> builderProvider) {
        RestClient.Builder builder = builderProvider.getIfAvailable(() -> {
            HttpClientSettings settings = HttpClientSettings.defaults();
            ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.httpComponents().build(settings);
            return RestClient.builder().requestFactory(requestFactory);
        });

        String baseUrl = exchangeRatesClientProperties.getBaseUrl();
        String accessKey = exchangeRatesClientProperties.getAccessKey();

        return builder
            .uriBuilderFactory(createAccessKeyUriBuilderFactory(baseUrl, accessKey))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

}
