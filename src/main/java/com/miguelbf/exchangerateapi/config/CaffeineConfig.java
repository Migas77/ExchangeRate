package com.miguelbf.exchangerateapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
@AllArgsConstructor
public class CaffeineConfig {

    private ExchangeRatesClientProperties exchangeRatesClientProperties;

    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder().expireAfterWrite(exchangeRatesClientProperties.getCachingTtl());
    }

    @Bean
    @Primary
    public CacheManager caffeineCacheManager(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }

}
