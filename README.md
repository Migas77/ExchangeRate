# Exchange Rate Challenge

API that fetches exchange rates from a publicly available API and uses them for conversion calculations, using Java and the Spring Framework. [Challenge Script](https://drive.google.com/file/d/16NZ2tXanYFSa2bUTXqOQoUSNKznK2czA/view?usp=sharing). The backend Docker image can be found at my [DockerHub](https://hub.docker.com/repository/docker/migas77/exchange-rate-api/general). Sonar build details and quality results can be found in my [SonarCloud](https://sonarcloud.io/project/overview?id=Migas77_ExchangeRate).



[![semantic-release: conventionalcommits](https://img.shields.io/badge/semantic--release-conventionalcommits-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release) [![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-blue?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org) [![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-F3702A?logo=sonarcloud&logoColor=white)](https://sonarcloud.io/)

---

## Setup & Execution

The CI pipeline automatically builds and pushes the Spring backend Docker image to DockerHub. Therefore, the only thing left to do is to run Docker Compose. [.env.example](.env.example) contains a sample .env configuration. You must copy it to .env and at least change the EXCHANGE_RATE_API_ACCESS_KEY value to your [exchangerate.host](https://exchangerate.host/) API key.

```bash
cp .env.example .env
# only if you are ok with access key in command history (else edit file directly using vim)
sed -i 's/^EXCHANGE_RATE_API_ACCESS_KEY=.*/EXCHANGE_RATE_API_ACCESS_KEY=your_api_key_here/' .env
```

Start the application using Docker Compose with the configured environment file.
```bash
docker compose --env-file .env up -d
```

After the application is up and running, you can access the API documentation at [http://localhost:8080/gw/swagger-ui/index.html](http://localhost:8080/gw/swagger-ui/index.html) or [http://localhost:8080/gw/v3/api-docs](http://localhost:8080/gw/v3/api-docs). Be aware that all endpoints at the documentation are prefixed with `/gw` (e.g. `/gw/api/auth/signup`, `/gw/api/auth/login`, `/gw/api/auth/refresh`, `/gw/api/rates`, `/gw/api/conversions`).

---

## Challenge Tasks

From the tasks defined in the [Challenge Script](https://drive.google.com/file/d/16NZ2tXanYFSa2bUTXqOQoUSNKznK2czA/view?usp=sharing), all mandatory tasks have been completed, along with the following extras:
1. Introduce a mechanism to make as few calls as possible to the external provider,
   while still providing meaningful/valid data. Assume that the clients don’t require
   real-time data and that they are fine to receive data with up to 1 min of delay.
2. Add unit tests to cover your code.
3. Add a rate-limiting mechanism to protect your API from abuse.
4. Provide a Dockerized setup for easy deployment.
6. Implement authentication/authorization for the API.

---


## Continuous Integration & Code Quality

In order to guarantee/maintain the code quality throughout the challenge and automate the build process, a [**CI pipeline with GitHub Actions**](.github/workflows) was configured. The following workflows are included:

- [ci.yml](.github/workflows/ci.yml) is the entrypoint on every push to `main`. It calls the tests/analysis workflow and, if it succeeds, the release workflow.
- [tests-and-sonar.yml](.github/workflows/tests-and-sonar.yml) automatically runs non-integration tests (`-DskipITs`) and pushes coverage results to SonarCloud on every pull request and on every push to `main`.
- [release.yml](.github/workflows/release.yml) automates the release process using [Semantic Release](https://semantic-release.org/), which derives the next version from the commit messages on `main`, following the [conventionalcommits](https://www.conventionalcommits.org/en/v1.0.0/) convention. The pipeline is configured in [.releaserc.yml](.releaserc.yml) and, on every release, it:
  - determines the version bump based on the default rules (`feat`, `fix`, ...), and `refactor`, `revert` and `build(mvn|docker|helm)` commits;
  - generates the release notes and updates `CHANGELOG.md`, grouping commits into sections (Features, Bug Fixes, Refactors, Build, Tests, Documentation, CI, ...);
  - bumps the version in `pom.xml`, updates the image tag in `docker-compose.yml` and builds the artifacts (JAR and OpenAPI spec);
  - commits those files back to `main` as `chore(release): <version>` and publishes a tagged GitHub Release with the JAR and the OpenAPI specification (JSON and YAML) attached.
- [docker-image-build-and-push.yml](.github/workflows/docker-image-build-and-push.yml) builds the backend Docker image. On pull requests the image is only built, as a way to validate the [Dockerfile](Dockerfile), while on every pushed tag (which is what Semantic Release produces) the image is tagged with the released version and pushed to DockerHub.

Both the build performed during the release and the tests run in CI use a dedicated `ci` Spring profile ([application-ci.properties](src/main/resources/application-ci.properties)) together with a `ci` Maven profile.

---

## Implementation Notes

The whole API is documented under [http://localhost:8080/gw/swagger-ui/index.html](http://localhost:8080/gw/swagger-ui/index.html). Regarding the implementation, the following details are of note.

### Authentication and Authorization

To implement **authentication and authorization mechanisms** for the API, JWT has been used to control access to protected API routes. Endpoints `POST /api/auth/signup`, `POST /api/auth/login` and `POST /api/auth/refresh` were created for the purpose of registering new users, authenticating existing users, and renewing JWT access tokens. The API is responsible for the generation of valid JWTs and the persistence of users in a Postgres database. However, validation of JWTs and creation of the appropriate HTTP responses was delegated to the spring-boot-starter-oauth2-resource-server dependency, which describes authentication/authorization failures in the WWW-Authenticate response header as per the [RFC 6750 Section 3 WWW-Authenticate response headers](https://datatracker.ietf.org/doc/html/rfc6750#section-3). The use of this dependency also means that the application could be changed to be secured through an authorization server (e.g. [Keycloak](https://www.keycloak.org/)) with minor modifications, instead of issuing its own JWTs.

Two different tokens are issued: an access token, which is the one accepted on the protected routes, and a refresh token, which is only accepted by `POST /api/auth/refresh`. Both are signed with the same key, so they are told apart by a `type` claim that is checked in the two `JwtDecoder` beans of [JwtDecoderConfig](src/main/java/com/miguelbf/exchangerateapi/config/security/JwtDecoderConfig.java). Passwords are never stored in plain text; they are hashed with BCrypt before being persisted. The auth endpoints themselves, the Swagger UI and the OpenAPI documents are the only routes left open; everything else requires a valid access token.

**Authorization** follows role-based access control. Each user has a role (`FREE_TIER`, `PREMIUM_TIER` or `MAX_TIER`), which is written into the token as a `role` claim and converted back into a `ROLE_<name>` granted authority when the token is validated, so that method security annotations such as `@Secured("ROLE_PREMIUM_TIER")` work as expected. No endpoint of this API is restricted to a specific role at the moment, since the challenge doesn't ask for tiers, but the mechanism is in place and is tested: the tests register a stub controller with a premium-only endpoint and assert that a free-tier user gets a `403` (with the RFC 6750 `WWW-Authenticate` header) while a premium user goes through. The natural use for this would be to have different rate limits per tier.

### Spring Cloud Gateway

The API leverages [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway) to serve as the single entrypoint to the application, routing incoming requests to the appropriate downstream controllers. In addition to routing, it provides support for a set of cross-cutting concerns, such as security, circuit breakers, rate limiting. In this project, these correspond to authentication and authorization (including JWT validation) and rate limiting. Although not implemented, a circuit breaker would also have been particularly valuable here given the dependence of the application on an external API. If such an API became unavailable or started failing, the circuit breaker would prevent the service from receiving requests and continuing to hammer the external API. Spring Cloud Gateway would facilitate this integration as well.

In this implementation, the gateway runs inside the same Spring application, with every route being exposed under a `/gw` prefix ([GatewayConfig](src/main/java/com/miguelbf/exchangerateapi/config/GatewayConfig.java)). The gateway matches `/gw/**`, applies the filters (e.g. [SecurityConfig](src/main/java/com/miguelbf/exchangerateapi/config/security/SecurityConfig.java)) and forwards the request internally to the actual controller path. The implementation could also be extracted into a standalone API Gateway (actually the Spring Cloud Gateway use case) service in front of multiple Spring backend instances without changing much. Centralizing these concerns in a single component would ensure that authentication, authorization and rate limiting are consistently enforced at a single entry point, rather than being reimplemented by each backend service.

### Rate Limiting

**Rate limiting** is enforced on the `/gw/**` route using Spring Cloud Gateway's support for [**Bucket4j**](https://github.com/bucket4j/bucket4j), following the **Token Bucket Algorithm** and using a **Redis Cache**. By default, each bucket has a capacity of 10 requests, refilled over a 1-minute window. Rate-limiting is applied based on the bucket key determined by the request’s authentication status. Authenticated requests are rate-limited per user using the JWT subject as the key (`b4j::user:<sub>`), while unauthenticated requests are rate-limited per IP address using the caller’s IP as the key (`b4j::ip:<address>`).

Every response carries an `X-RateLimit-Remaining` header with the number of requests left in the current window, and once the bucket is empty the gateway answers `429 Too Many Requests` (with `X-RateLimit-Remaining: 0`), without it being forwarded to the controller. The buckets are stored in Redis, so the limit is shared across all backend instances (if there were multiple) and survives restarts.


### Observability

Regarding **observability**, structured logging was used following the ecs format. The application also includes the oTel dependency which results in the generation of traceId and spanId and its inclusion in the JSON logs, although no open telemetry exporter was configured or included in the docker compose. A masking encoder is also included in order to not leak through the logs the access key.


### Caching

For the main API operation, the [exchangerate.host](https://docs.apilayer.com/exchangerate/docs/api-documentation) API (`/live` endpoint) was used to fetch the currency exchange rates. These rates were **cached** using the **`@Cacheable`** annotation with Redis to promote horizontal scalability and reduce upstream API calls over multiple Spring backend instances. By default, the cache key is built from the method arguments, which here are the source currency and the (optional, i.e. ALL) target currency, so it ends up being `USD:EUR` when a single target is asked for and just `USD` when all the rates are asked for. One consequence worth pointing out is that a request asking for the exchange rate from `USD` to `EUR` will not hit the global cache (`USD` with all entries for all currencies). Conversions behave the same way, since they are served from these same rates: a `1:1` conversion asks for the single quote, while a `1:N` (or all currencies) conversion fetches all the rates at once and filters the requested ones locally. This was kept this way to stay closer to the endpoint described in the challenge, but it would arguably be better to always fetch all the rates for a source currency and always filter them, as one cached entry per source would then serve every request and save a good number of upstream calls. That would only become a problem if the upstream response time grew significantly with the number of returned quotes, which could not be verified in practice given the limited number of upstream calls available, under my access key. 

#### Cache Stampede

However, caching using the default spring-data-redis with lettuce driver introduces the problem of cache stampede, where upon cache expiration, multiple concurrent requests simultaneously miss the cache and flood the upstream API with redundant calls. For those particular cases, it's possible to use the `sync` attribute to instruct the underlying cache provider to lock the cache entry while the value is being computed, as mentioned in [Synchronized Caching](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html#cache-annotations-cacheable-synchronized). As a result, only one thread is busy computing the value, while the others are blocked until the entry is updated in the cache. However, this is an optional feature left to the implementation of the cache manager provider and the default caching implementation of spring-data-redis with lettuce driver present in the implementation provided at the [main branch of this repository](https://github.com/Migas77/ExchangeRate/tree/main) does not provide such support, as specified [here](https://github.com/spring-projects/spring-data-redis/issues/1253). Instead, it requires enabling `RedisCacheWriter.lockingRedisCacheWriter` which, although it achieves the single upstream call it locks the entire cache (for reads and writes) for all entries in the database (in this case all currencies under the key `liveRates*`). Therefore, this wasn't included in the solution and two alternatives were provided at the **[redisson](https://github.com/Migas77/ExchangeRate/tree/redisson) and [caffeine](https://github.com/Migas77/ExchangeRate/tree/caffeine) branches** which have their own tradeoffs. Both implementation allow keeping the same `@Cacheable(sync = true)` annotation. **Redisson** introduces itw own Cache Manager, supporting `sync = true` with a per-key distributed lock (`Rlock`) rather than a single lock over the whole cache. However, the keys now correspond to entries within a Redis hash map. Unlike standalone Redis keys, hash map entries do not support a default per-entry TTL, so expiration must be handled explicitly (actively by redisson) rather than relying on Redis's native per-key TTL. The **caffeine** approach switches the redis instance by a local (per spring instance) cache. This is reasonably faster, as it avoids both the network round-trip to Redis and the serialization/deserialization overhead, storing the POJOs directly on the Java heap instead of as a serialized byte representation. However, this would imply that different spring instances would return different (although valid) values for the same requests as they do not share a cache. Perhaps a better alternative would be to combine the two approaches into a two-tiered cache: a local Caffeine cache as a fast first layer in front of a shared Redis cache as the second layer. This would require an invalidation strategy between the two caches to keep them consistent with one another.

With that said, for this particular problem the cache is only ever populated **reactively**. If we knew the actual client profile and a list of most queried currencies (or even if this was for internal use) maybe it would make more sense to fill the cache **preemptively**, refreshing the rates on a background thread (a scheduled task) or on a dedicated sidecar/worker that owns the upstream calls and writes the entries to Redis.

### Error Handling with Problem Details

**Error responses** follow [RFC 9457 (Problem Details for HTTP APIs)](https://datatracker.ietf.org/doc/html/rfc9457) through Spring's `ProblemDetail`, built in a single place ([ProblemDetailsFactory](src/main/java/com/miguelbf/exchangerateapi/exception/ProblemDetailsFactory.java)) so that every handler produces the same `type`/`title`/`detail`/`instance` shape with the `application/problem+json` content type. The handlers are split by concern: [ValidationExceptionHandler](src/main/java/com/miguelbf/exchangerateapi/exception/handler/ValidationExceptionHandler.java) for the request contract (unknown or repeated currencies, malformed amounts), [UpstreamExceptionHandler](src/main/java/com/miguelbf/exchangerateapi/exception/handler/UpstreamExceptionHandler.java) for upstream failures and inconsistent upstream payloads, and [GlobalExceptionHandler](src/main/java/com/miguelbf/exchangerateapi/exception/handler/GlobalExceptionHandler.java) to catch all unhandled (possibly missed) exceptions, logging them instead. [AuthExceptionHandler](src/main/java/com/miguelbf/exchangerateapi/exception/handler/AuthExceptionHandler.java) delegates the exceptions raised by the manual refresh-token validation in `POST /api/auth/refresh` to the very same `AuthenticationEntryPoint` used by Spring Security's filter chain, expresing authentication and authorization failures as the RFC 6750 `WWW-Authenticate` headers.


### Monetary Precision with BigDecimal

All monetary values (amounts, rates and conversion results) are handled as **`BigDecimal`** (never `double` or `float`), so that no binary floating point representation error is introduced into the conversion math. The multiplication in [ConversionService](src/main/java/com/miguelbf/exchangerateapi/service/impl/ConversionService.java) is done with `MathContext.DECIMAL128` (34 significant digits) and the result is then rounded to 12 decimal places with `RoundingMode.HALF_EVEN` (banker's rounding). These values were chosen based on the precision and decimal part observed for the rates results returned from the upstream API (max 16 digits). Amounts are bounded at the API boundary and validated again in the service (16 significant digits, 10 integer and 6 decimal). Combined, conversion multiplications are probably lossless with the 34 digit precision offered by `DECIMAL128`.

Regarding the **serialization of `BigDecimal`**, JSON has no decimal type, only `number`. I'm aware that some JSON clients (such as javascript) parse these number into a floating point value, which can silently lose the precision that `BigDecimal` was meant to preserve. A solution to this would be to to serialize these values as **strings** and let the client parse them into its own decimal type. This constitutes a topic of discussion in online forums (string vs number) and it wasn't done here to mimic the upstream API which also doesn't serialize them as string.

---

## Test Notes

The tests follow a layered approach (`controller` -> `service` -> `clientservice`, `controller` -> `service` -> `repository`), where each layer is tested in isolation with the layer below being mocked. 

- **Controller**
  - [ExchangeRatesControllerMockServiceTest](src/test/java/com/miguelbf/exchangerateapi/controller/ExchangeRatesControllerMockServiceTest.java), with `IExchangeRatesService` and `IConversionService` mocked, covers the `/rates` and `/conversion` request/response contracts: query parameter validation (unknown, repeated or missing currencies, invalid amounts), the returned JSON shape and the number formatting (no scientific notation), plus the `ProblemDetail` produced for failures.
  - [AuthControllerMockServiceTest](src/test/java/com/miguelbf/exchangerateapi/controller/AuthControllerMockServiceTest.java), with `IAuthService` mocked, covers the signup/login/refresh contracts: body validation, the token payload returned on success and the `401` responses (existing email, unknown user, wrong password, tampered/expired refresh token) together with their `WWW-Authenticate` headers characteristic of [RFC 6750 Section 3 WWW-Authenticate response headers](https://datatracker.ietf.org/doc/html/rfc6750#section-3).
- **Service**
  - [ExchangeRatesServiceMockClientTest](src/test/java/com/miguelbf/exchangerateapi/service/ExchangeRatesServiceMockClientTest.java), with `IExchangeRatesClientService` mocked, covers the mapping of the upstream payload into `RatesResponse` and the validation of that payload (source mismatch, unexpected quote count, missing requested target).
  - [ConversionServiceMockServicesTest](src/test/java/com/miguelbf/exchangerateapi/service/ConversionServiceMockServicesTest.java), with `IExchangeRatesService` mocked, covers the BigDecimal conversion math, the filtering of the requested targets (from global cache entry), the rejection of invalid amounts and the logging of invalid upstream rates.
  - [UserServiceMockRepoTest](src/test/java/com/miguelbf/exchangerateapi/service/UserServiceMockRepoTest.java), with `UserRepository` mocked, covers user lookup/creation and the `UserDetailsService` contract used by Spring Security.
  - [AuthServiceMockServicesTest](src/test/java/com/miguelbf/exchangerateapi/service/AuthServiceMockServicesTest.java), with `IUserService`, `IJwtService` and Spring Security's `AuthenticationManager` mocked, covers the signup/login/refresh orchestration: duplicated emails, bad credentials, and refresh token validation (a refresh token but not an access token).
  - [JwtServiceTest](src/test/java/com/miguelbf/exchangerateapi/service/JwtServiceTest.java) covers the token generation itself (claims, roles, token type and expiry) with no collaborators mocked.
- **Client**. [ExchangeRatesClientMockUpstreamAPITest](src/test/java/com/miguelbf/exchangerateapi/client/ExchangeRatesClientMockUpstreamAPITest.java) uses `@RestClientTest` with `MockRestServiceServer`, mocking the upstream API. Covers the request built by the `RestClient` (path, query params, access key) and how malformed, error and timeout responses are translated into domain exceptions.
- **Repository**. [UserRepositoryIT](src/test/java/com/miguelbf/exchangerateapi/repository/UserRepositoryIT.java) uses `@DataJpaTest` against a PostgreSQL TestContainer.


On top of these, there are the following tests:

- **Caching**. [ExchangeRatesServiceCachingTest](src/test/java/com/miguelbf/exchangerateapi/service/ExchangeRatesServiceCachingTest.java) uses a `ConcurrentMapCacheManager` to assert the caching semantics (what gets cached, under which key, and that a second call with the same parameters does not reach the upstream client), while [ExchangeRatesServiceRedisCachingIT](src/test/java/com/miguelbf/exchangerateapi/service/ExchangeRatesServiceRedisCachingIT.java) runs the same behaviour against a real Redis through [Testcontainers](https://testcontainers.com/), also asserting the TTL expiry with [Awaitility](https://github.com/awaitility/awaitility).
- **Gateway (authentication + rate limiting)**. [GatewayRouteAuthRateLimitTest](src/test/java/com/miguelbf/exchangerateapi/gateway/GatewayRouteAuthRateLimitTest.java) boots the application with the `no-redis` profile and in memory buckets, checking routing, JWT validation (expired, tampered, wrong role, missing header) and the `429` responses, whereas [GatewayAuthRateLimitIT](src/test/java/com/miguelbf/exchangerateapi/gateway/GatewayAuthRateLimitIT.java) runs against a Redis container to verify the redis bucket4j buckets are actually created with the expected `b4j::user:` / `b4j::ip:` keys. It also validates the forwarding behavior of the used Spring Cloud Gateway.
- **Exception handling**, in [GlobalExceptionHandlerMockExceptionsTest](src/test/java/com/miguelbf/exchangerateapi/exception/GlobalExceptionHandlerMockExceptionsTest.java) and [UpstreamExceptionHandlerMockExceptionsTest](src/test/java/com/miguelbf/exchangerateapi/exception/UpstreamExceptionHandlerMockExceptionsTest.java), where a stub controller is made to throw each supported exception and the resulting `ProblemDetail` response and the log entries are asserted. Ocasionally, similar tests are also present in the respective controllers.
- **Log masking**, in [MaskingLogTest](src/test/java/com/miguelbf/exchangerateapi/logging/MaskingLogTest.java), which captures the console appender output and asserts that sensitive values, namely the access key never reaches the structured logs.

Tests suffixed with `IT` are the ones that need docker (Testcontainers), which is why the CI workflow runs the suite with `-DskipITs`. Coverage is measured with JaCoCo and reported to SonarCloud.

---

## References

Null Safety in the Spring Framework:
- [Spring Framework / Core Technologies / Null-safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html)
- [Null Safety in Spring applications with JSpecify and NullAway](https://spring.io/blog/2025/03/10/null-safety-in-spring-apps-with-jspecify-and-null-away)
