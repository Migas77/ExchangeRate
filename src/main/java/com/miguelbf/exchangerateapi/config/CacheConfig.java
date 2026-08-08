package com.miguelbf.exchangerateapi.config;


import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import com.miguelbf.exchangerateapi.model.dto.RatesResponse;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@AllArgsConstructor
@Profile("!ci & !no-redis")
public class CacheConfig {

    private ExchangeRatesClientProperties exchangeRatesClientProperties;
    private DataRedisProperties redisProperties;

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        RedisStandaloneConfiguration standaloneConfig =
            new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());

        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            standaloneConfig.setPassword(redisProperties.getPassword());
        }

        return new LettuceConnectionFactory(standaloneConfig);
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(exchangeRatesClientProperties.getCachingTtl())
            .disableCachingNullValues()
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new JacksonJsonRedisSerializer<>(RatesResponse.class)));
    }

    @Bean
    public RedisCacheManager cacheManager(
        @Qualifier("lettuceConnectionFactory") RedisConnectionFactory redisConnectionFactory,
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
    ApplicationRunner warmUpRedis(@Qualifier("lettuceConnectionFactory") RedisConnectionFactory factory) {
        // removes small remaining overhead for the first call after setting eager initialization
        // actually triggers LettuceConnectionFactory.doCreateLettuceConnection
        return args -> factory.getConnection().ping();
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort());

        if (redisProperties.getPassword() == null || redisProperties.getPassword().isBlank()) {
            throw new IllegalStateException("Redis password not set.");
        }

        config.setPassword(redisProperties.getPassword());
        config.setCodec(new TypedJsonJacksonCodec(String.class, RatesResponse.class));
        return Redisson.create(config);
    }

    @Bean
    @Primary
    public CacheManager redissonCacheManager(RedissonClient redissonClient) {
        Map<String, org.redisson.spring.cache.CacheConfig> config = new HashMap<>();

        // per-cache-name settings: ttl, maxIdleTime, maxSize
        config.put("liveRates", new org.redisson.spring.cache.CacheConfig(
            exchangeRatesClientProperties.getCachingTtl().toMillis(),
            TimeUnit.MINUTES.toMillis(10)
        ));

        return new RedissonSpringCacheManager(redissonClient, config);
    }

}