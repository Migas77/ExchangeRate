package com.miguelbf.exchangerateapi.config;

import com.miguelbf.exchangerateapi.config.properties.RateLimitProperties;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.forward;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;


@Configuration
public class GatewayConfig {

    @Bean
    public Function<ServerRequest, String> keyResolver() {
        return request -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                return "b4j::user:" + jwt.getSubject();
            }
            return "b4j::ip:" + request.servletRequest().getRemoteAddr();
        };
    }

    @Bean
    public RouterFunction<ServerResponse> customRoutes(
        HandlerFilterFunction<ServerResponse, ServerResponse> rateLimitFilter
    ) {

        return route("documentation")
            .route(path("/gw/swagger-ui/{*segments}"), forward("/swagger-ui{segments}"))
            .route(path("/gw/v3/api-docs/{*segments}"), forward("/v3/api-docs{segments}"))
            .route(path("/gw/v3/api-docs.yaml"), forward("/v3/api-docs.yaml"))
            .build().and(route("api_rate_limited")
                .route(path("/gw/{*segments}"), forward("{segments}"))
                .filter(rateLimitFilter)
                .build());
    }

    @Bean
    @Profile("!ci & !no-redis")
    public HandlerFilterFunction<ServerResponse, ServerResponse> redisRateLimitFilter(
        Function<ServerRequest, String> keyResolver,
        RateLimitProperties rateLimitProperties
    ) {
        return rateLimit((Bucket4jFilterFunctions.RateLimitConfig config) -> config
            .setCapacity(rateLimitProperties.getCapacity())
            .setPeriod(rateLimitProperties.getPeriod())
            .setTimeout(rateLimitProperties.getTimeout())
            .setKeyResolver(keyResolver)
        );
    }

    @Bean
    @Profile({"ci", "no-redis"})
    public HandlerFilterFunction<ServerResponse, ServerResponse> inMemoryRateLimitFilter(
        Function<ServerRequest, String> keyResolver,
        RateLimitProperties rateLimitProperties
    ) {
        Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        return (request, next) -> {
            String key = keyResolver.apply(request);
            Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(limit -> limit
                    .capacity(rateLimitProperties.getCapacity())
                    .refillGreedy(rateLimitProperties.getCapacity(), rateLimitProperties.getPeriod()))
                .build());

            if (bucket.tryConsume(1)) {
                return next.handle(request);
            }
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).build();
        };
    }

    @Bean(destroyMethod = "close")
    @Profile("!ci & !no-redis")
    public StatefulRedisConnection<String, byte[]> bucket4jRedisConnection(
        LettuceConnectionFactory redisConnectionFactory
    ) {
        RedisClient redisClient = (RedisClient) redisConnectionFactory.getNativeClient();
        if (redisClient == null) {
            throw new IllegalStateException("RedisClient is null. Please check your Redis configuration.");
        }
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    @Profile("!ci & !no-redis")
    public AsyncProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> bucket4jRedisConnection) {
        return Bucket4jLettuce.casBasedBuilder(bucket4jRedisConnection)
            .expirationAfterWrite(
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
            .build()
            .asAsync();
    }

}
