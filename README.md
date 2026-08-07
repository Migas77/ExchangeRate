# Exchange Rate Challenge

API that fetches exchange-rates from publicly available API and uses them for conversion calculations using Java and the Spring Framework. [Challenge Script](https://drive.google.com/file/d/16NZ2tXanYFSa2bUTXqOQoUSNKznK2czA/view?usp=sharing). Backend docker image can be found at my [DockerHub](https://hub.docker.com/repository/docker/migas77/exchange-rate-api/general). Sonar build details and quality results can be found in my [SonarCloud](https://sonarcloud.io/project/overview?id=Migas77_ExchangeRate). 



[![semantic-release: conventionalcommits](https://img.shields.io/badge/semantic--release-conventionalcommits-e10079?logo=semantic-release)](https://github.com/semantic-release/semantic-release) [![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-blue?logo=conventionalcommits&logoColor=white)](https://conventionalcommits.org) [![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![SonarCloud](https://img.shields.io/badge/SonarCloud-F3702A?logo=sonarcloud&logoColor=white)](https://sonarcloud.io/)

---

## Setup & Execution

The CI pipeline automatically builds and pushes the spring backend docker image to dockerhub. Therefore, the only thing left to do is to run the docker compose. [.env.example](.env.example) contains a sample .env configuration. You must copy it to .env and at least change the EXCHANGE_RATE_API_ACCESS_KEY value to your [exchangerate.host](https://exchangerate.host/) api key  

```bash
cp .env.example .env
# only if you are ok with access key in command history (else edit file directly using vim)
sed -i 's/^EXCHANGE_RATE_API_ACCESS_KEY=.*/EXCHANGE_RATE_API_ACCESS_KEY=your_api_key_here/' .env
```

Start the application using Docker Compose with the configured environment file.
```bash
docker compose --env-file .env up -d
```

---

## Challenge Tasks

From the tasks defined in the [Challenge Script](https://drive.google.com/file/d/16NZ2tXanYFSa2bUTXqOQoUSNKznK2czA/view?usp=sharing), all mandatory tasks have been completed, along with the following extras:
1. Introduce a mechanism to make as few calls as possible to the external provider,
   while still providing meaningful/valid data. Assume that the clients don’t require
   real-time data and that they are fine to receive data with up to 1 min of delay.
2. Add unit tests to cover your code.
3. Add a rate-limiting mechanism to protect your API from abuse.
4. Provide a Dockerized setup for easy deployment.
5. 
6. Implement authentication/authorization for the API.

---


## Continuous Integration & Code Quality

In order to guarantee/maintain the code quality throughout the challenge and automate the build process a [**CI pipeline with github actions**](.github/workflows) was configured. The following workflows are included:

- [ci.yml](.github/workflows/ci.yml) is the entrypoint on every push to `main`. It calls the tests/analysis workflow and, if it succeeds, the release workflow.
- [tests-and-sonar.yml](.github/workflows/tests-and-sonar.yml) automatically runs non integration tests (`-DskipITs`) and pushes coverage results to SonarCloud on every pull request and on every push to `main`.
- [release.yml](.github/workflows/release.yml) automates the release process using [Semantic Release](https://semantic-release.org/), which derives the next version from the commit messages on `main`, following the [conventionalcommits](https://www.conventionalcommits.org/en/v1.0.0/) convention. The pipeline is configured in [.releaserc.yml](.releaserc.yml) and, on every release, it:
  - determines the version bump based on the default rules (`feat`, `fix`, ...), and `refactor`, `revert` and `build(mvn|docker|helm)` commits;
  - generates the release notes and updates `CHANGELOG.md`, grouping commits into sections (Features, Bug Fixes, Refactors, Build, Tests, Documentation, CI, ...);
  - bumps the version in `pom.xml`, updates the image tag in `docker-compose.yml` and builds the artifacts (JAR and OpenAPI spec);
  - commits those files back to `main` as `chore(release): <version>` and publishes a tagged GitHub Release with the JAR and the OpenAPI specification (JSON and YAML) attached.
- [docker-image-build-and-push.yml](.github/workflows/docker-image-build-and-push.yml) builds the backend docker image. On pull requests the image is only built, as a way to validate the [Dockerfile](Dockerfile), while on every pushed tag (which is what Semantic Release produces) the image is tagged with the released version and pushed to DockerHub..

Both the build performed during the release and the tests run in CI use a dedicated `ci` spring profile ([application-ci.properties](src/main/resources/application-ci.properties)) together with a `ci` maven profile.

---

## Implementation Notes


---

## Test Notes

The tests follow a layered approach (`controller` -> `service` -> `clientservice`, `controller` -> `service` -> `repository`), where each layer is tested in isolation with the layer below being mocked. 

| Layer      | Test                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | Slice                                                                                                                                                                                                                                                                                                             |
|------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Controller | [ExchangeRatesControllerMockServiceTest](src/test/java/com/miguelbf/exchangerateapi/controller/ExchangeRatesControllerMockServiceTest.java), [AuthControllerMockServiceTest](src/test/java/com/miguelbf/exchangerateapi/controller/AuthControllerMockServiceTest.java)                                                                                                                                                                                                                                                                                                                     | `@WebMvcTest` with `MockMvc`, services replaced by `@MockitoBean`. Covers request/response contracts, validation and serialization.                                                                                                                                                                               |
| Service    | [ExchangeRatesServiceMockClientTest](src/test/java/com/miguelbf/exchangerateapi/service/ExchangeRatesServiceMockClientTest.java), [ConversionServiceMockServicesTest](src/test/java/com/miguelbf/exchangerateapi/service/ConversionServiceMockServicesTest.java), [UserServiceMockRepoTest](src/test/java/com/miguelbf/exchangerateapi/service/UserServiceMockRepoTest.java), [AuthServiceMockServicesTest](src/test/java/com/miguelbf/exchangerateapi/service/AuthServiceMockServicesTest.java), [JwtServiceTest](src/test/java/com/miguelbf/exchangerateapi/service/JwtServiceTest.java) | Plain Mockito (no spring context) whenever possible, business rules and conversion math only.                                                                                                                                                                                                                     |
| Client     | [ExchangeRatesClientMockUpstreamAPITest](src/test/java/com/miguelbf/exchangerateapi/client/ExchangeRatesClientMockUpstreamAPITest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                    | `@RestClientTest` with `MockRestServiceServer`, so the upstream [exchangerate.host](https://exchangerate.host/) API is never actually called. Covers the request built by the `RestClient` (path, query params, access key) and how malformed, error and timeout responses are translated into domain exceptions. |
| Repository | [UserRepositoryIT](src/test/java/com/miguelbf/exchangerateapi/repository/UserRepositoryIT.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | `@DataJpaTest` against a PostgreSQL TestContainer                                                                                                                                                                                                                                                                 |

On top of the layered tests, the cross cutting concerns have their own tests:

- **Caching**. [ExchangeRatesServiceCachingTest](src/test/java/com/miguelbf/exchangerateapi/service/ExchangeRatesServiceCachingTest.java) uses a `ConcurrentMapCacheManager` to assert the caching semantics (what gets cached, under which key, and that a second call with the same parameters does not reach the upstream client), while [ExchangeRatesServiceRedisCachingIT](src/test/java/com/miguelbf/exchangerateapi/service/ExchangeRatesServiceRedisCachingIT.java) runs the same behaviour against a real Redis through [Testcontainers](https://testcontainers.com/), also asserting the TTL expiry with [Awaitility](https://github.com/awaitility/awaitility).
- **Gateway (authentication + rate limiting)**. [GatewayRouteAuthRateLimitTest](src/test/java/com/miguelbf/exchangerateapi/gateway/GatewayRouteAuthRateLimitTest.java) boots the application with the `no-redis` profile and in memory buckets, checking routing, JWT validation (expired, tampered, wrong role, missing header) and the `429` responses, whereas [GatewayAuthRateLimitIT](src/test/java/com/miguelbf/exchangerateapi/gateway/GatewayAuthRateLimitIT.java) runs against a Redis container to verify the redis bucket4j buckets are actually created with the expected `b4j::user:` / `b4j::ip:` keys. It also validates the forwarding behavior of the used Spring Cloud Gateway.
- **Exception handling**, in [GlobalExceptionHandlerMockExceptionsTest](src/test/java/com/miguelbf/exchangerateapi/exception/GlobalExceptionHandlerMockExceptionsTest.java) and [UpstreamExceptionHandlerMockExceptionsTest](src/test/java/com/miguelbf/exchangerateapi/exception/UpstreamExceptionHandlerMockExceptionsTest.java), where a stub controller is made to throw each supported exception and the resulting `ProblemDetail` response and the log entries are asserted. Ocasionally, similar tests are also present in the respective controllers.
- **Log masking**, in [MaskingLogTest](src/test/java/com/miguelbf/exchangerateapi/logging/MaskingLogTest.java), which captures the console appender output and asserts that sensitive values, namely the access key never reaches the structured logs.
- **Persistence**, in , a `@DataJpaTest` against a PostgreSQL container instead of an in memory database.

Tests suffixed with `IT` are the ones that need docker (Testcontainers), which is why the CI workflow runs the suite with `-DskipITs`. Coverage is measured with JaCoCo and reported to SonarCloud.

---

Null Safety in the Spring Framework:
- [Spring Framework / Core Technologies / Null-safety](https://docs.spring.io/spring-framework/reference/core/null-safety.html)
- [Null Safety in Spring applications with JSpecify and NullAway](https://spring.io/blog/2025/03/10/null-safety-in-spring-apps-with-jspecify-and-null-away)
