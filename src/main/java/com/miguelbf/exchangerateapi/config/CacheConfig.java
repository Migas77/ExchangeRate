package com.miguelbf.exchangerateapi.config;


import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
@EnableCaching
@AllArgsConstructor
public class CacheConfig {

    private ExchangeRatesClientProperties exchangeRatesClientProperties;

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(exchangeRatesClientProperties.getCachingTTL())
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new JacksonJsonRedisSerializer<>(RatesResponse.class)));
    }

    @Bean
    public RedisCacheManager cacheManager(
        RedisConnectionFactory redisConnectionFactory,
        RedisCacheConfiguration redisCacheConfiguration
    ) {
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .build();
    }

    @Bean
    static BeanPostProcessor eagerLettuceInitializer() {
        // removes huge overhead from first redis call (roughly 150 ms)
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) {
                if (bean instanceof LettuceConnectionFactory lettuceConnectionFactory) {
                    lettuceConnectionFactory.setEagerInitialization(true);
                }
                return bean;
            }
        };
    }

    @Bean
    ApplicationRunner warmUpRedis(RedisConnectionFactory factory) {
        // removes small remaining overhead for the first call after setting eager initialization
        // actually triggers LettuceConnectionFactory.doCreateLettuceConnection
        return args -> factory.getConnection().ping();
    }

}