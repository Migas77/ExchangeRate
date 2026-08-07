package com.miguelbf.exchangerateapi.config;

import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.RestClientCustomizer;
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

    @Bean
    public RestClient getExchangeRatesRestClient(ObjectProvider<RestClientCustomizer> customizers) {
        HttpClientSettings settings = HttpClientSettings.defaults()
            .withConnectTimeout(exchangeRatesClientProperties.getConnectTimeout())
            .withReadTimeout(exchangeRatesClientProperties.getReadTimeout());
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.httpComponents().build(settings);

        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));

        String baseUrl = exchangeRatesClientProperties.getBaseUrl();
        String accessKey = exchangeRatesClientProperties.getAccessKey();

        return builder
            .uriBuilderFactory(createAccessKeyUriBuilderFactory(baseUrl, accessKey))
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
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

}
