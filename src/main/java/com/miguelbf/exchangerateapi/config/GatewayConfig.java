package com.miguelbf.exchangerateapi.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;

import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.forward;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;


@Configuration
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> customRoutes() {

        return route("api_rate_limited")
            .route(path("/gw/{*segments}"), forward("/api{segments}"))
            .filter(rateLimit((Bucket4jFilterFunctions.RateLimitConfig config) -> config
                .setCapacity(10)
                .setPeriod(Duration.ofMinutes(1))
                .setKeyResolver(request -> {
                    String key = request.servletRequest().getRemoteAddr();
                    System.out.println(">>> Rate limiter key: " + key);
                    return key;
                })
            ))
            .build();


    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(
        LettuceConnectionFactory redisConnectionFactory
    ) {
        RedisClient redisClient = (RedisClient) redisConnectionFactory.getNativeClient();
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public AsyncProxyManager<String> proxyManager(
        StatefulRedisConnection<String, byte[]> bucket4jRedisConnection
    ) {
        return Bucket4jLettuce.casBasedBuilder(bucket4jRedisConnection)
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
            .build()
            .asAsync();
    }

}
