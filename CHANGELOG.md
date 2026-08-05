## [2.0.0](https://github.com/Migas77/ExchangeRate/compare/v1.2.0...v2.0.0) (2026-08-05)

### ⚠ BREAKING CHANGES

* **gateway:** changed API contract. All API endpoints are now prefixed with /gw. This is also true for documentation (swagger) endpoints.

Rate limiting:
  - Rate limit key "b4j::user:<sub>" or "b4j::ip:<remoteAddr>" for authenticated and anonymous requests, respectively
  - Capacity, period and timeout defaulted to 10, 1m and 500ms, repsectively
  - Rate limit uses redis in production and ConcurrentHashMap for ci and no-redis profiles

Configuration refactors:
  - SecurityConfig contains now two filter chains: 1. for oauth resource server and jwt auth; 2. for deny all
  - AuthExceptionHandler and GlobalExceptionHandler no longer rethrow exceptions and calls AuthenticationEntryPoint/AccessDeniedHandler directly to produce the RFC 6750 401/403 responses

At this commit, tests are broken and the swagger docs outdated, i.e. not updated for /gw prefix

### Features

* **gateway:** spring cloud gateway with rate limiting (TOKEN BUCKET alg with bucket4j and redis for anonymous (ip) and auth users) routes traffic from /gw prefix to apis (direct access to controllers blocked) ([34d34e0](https://github.com/Migas77/ExchangeRate/commit/34d34e035c7082de3a76fc02f7d23727e7476ef8))
* **rate-limiter:** simple bucket4j rate limitter setup with redis cache using spring cloud gateway ([9ac0de1](https://github.com/Migas77/ExchangeRate/commit/9ac0de1345b6d7e075f3d0ae4cfb959792ec4a9a))

### Build

* **mvn:** add dependencies spring-boot-webtestclient and new ignores for jacoco/sonar ([7499129](https://github.com/Migas77/ExchangeRate/commit/74991298024421b28f8eb6b4e1b1720cf8f620cf))
* **mvn:** add dependencies spring-cloud-starter-gateway-server-webmvc bucket4j_jdk17-core bucket4j_jdk17-redis-common and BOM spring-cloud-dependencies ([b466b23](https://github.com/Migas77/ExchangeRate/commit/b466b237a6d3740e03accc9f38989597e4c29ca5))

### Tests

* adapt tests to new bean config ([7caf51c](https://github.com/Migas77/ExchangeRate/commit/7caf51c6a93ec9b61c00be885da1b937b9201b60))
* **gateway:** integration testing gateway auth and rate limiting against a redis testcontainer ([b3b0120](https://github.com/Migas77/ExchangeRate/commit/b3b01206ff9faae861e152d1c88cf33cc03ba78a))
* **gateway:** testing gateway routing, auth and rate limiting against the /gw prefixed routes, replacing AuthExceptionHandlerTest ([d363b86](https://github.com/Migas77/ExchangeRate/commit/d363b8669c227443b97ef76de6e425e05c6bb10b))

### Documentation

* **swagger:** change swagger to cater to new /gw gateway prefix ([2209a19](https://github.com/Migas77/ExchangeRate/commit/2209a194f1eaf9daeca21b8dbdae0f98aa7a624a))

## [1.2.0](https://github.com/Migas77/ExchangeRate/compare/v1.1.0...v1.2.0) (2026-08-03)

### Features

* **auth:** JWT authentication with signup endpoint and RFC 6750 Section 3 WWW-Authenticate response headers (https://datatracker.ietf.org/doc/html/rfc6750\[#section](https://github.com/Migas77/ExchangeRate/issues/section)-3\). JwtService is reduced to token issuing, with extractSubject, isTokenValid and isRefreshTokenValid being removed. ([e963426](https://github.com/Migas77/ExchangeRate/commit/e963426d7d7daac74e848ec42c2d423112a85472))
* **auth:** JWT service and properties for generation and validation of token and refresh token JWTs ([f1bbd80](https://github.com/Migas77/ExchangeRate/commit/f1bbd808f88f756a3a873d8cba0af1b721a08176))
* **auth:** login endpoint ([8cf4ad9](https://github.com/Migas77/ExchangeRate/commit/8cf4ad9be519b3c05ef4b4861e4c6b2a0f174365))
* **auth:** refresh endpoint. Delegate validation and handling of exceptions to oauth2-resource-server dependency ([770a113](https://github.com/Migas77/ExchangeRate/commit/770a113b77db350b4c31018ebb26b7f146d198e5))
* **database:** integrate Flyway migrations with Spring Boot ([58e1ee2](https://github.com/Migas77/ExchangeRate/commit/58e1ee2ad11886cf38de417fec0050e99f3fce13))

### Refactors

* **structure:** new service.impl package for implementations (separate from interfaces) ([b00ee8f](https://github.com/Migas77/ExchangeRate/commit/b00ee8fef91bbba574ce69901c03e0cd8452304d))

### Build

* **mvn:** add depedencies spring-boot-starter-data-jpa,spring-boot-starter-security,postgresql,jjwt-api/impl/jackson,spring-boot-starter-flyway,flyway-database-postgresql ([7190b59](https://github.com/Migas77/ExchangeRate/commit/7190b5971532c1e6f7c73c4cddd9acbddcd47e4d))
* **mvn:** add dependencies spring-boot-starter-data-jpa-test & testcontainers-postgresql ([b377f1a](https://github.com/Migas77/ExchangeRate/commit/b377f1afd1d8b6bb5ef38ef8bbd7bd52b63c6819))
* **mvn:** add dependency spring-boot-starter-oauth2-resource-server ([ce11465](https://github.com/Migas77/ExchangeRate/commit/ce11465eb8a3a8635037da2b53744b9fa3ce02b8))

### Tests

* **auth-controller:** testing auth controller and exception handling (including WWW Authenticate headers) with mock auth service ([66994ba](https://github.com/Migas77/ExchangeRate/commit/66994ba6e8ed34156e040359655d6e013389a30f))
* **auth-exception-handler:** premium only stub endpoint to test default 403 insufficient_scope response for a free tier user (and 200 for premium) ([41aaedf](https://github.com/Migas77/ExchangeRate/commit/41aaedf02ffebab355ec0ec1750c58cbbeac515e))
* **auth-exception-handler:** testing default oauth2ResourceServer 401 handling and RFC 6750 Section 3 WWW-Authenticate response headers against a stub secured endpoint ([6028454](https://github.com/Migas77/ExchangeRate/commit/6028454e6bfcbd4dd3b5e5bae116972a2d7e7d6e))
* **auth-service:** testing auth service (signup, login and refresh) with mocked user/jwt services and authentication manager (also tested actual configured jwt decoder) ([0e72a26](https://github.com/Migas77/ExchangeRate/commit/0e72a26d3d8c808c004914f9c05ef49866d8626e))
* **jwt-service:** testing jwt service token issuing and expiration extraction against the configured signing key ([b681bc4](https://github.com/Migas77/ExchangeRate/commit/b681bc4ffbd41209652fea2e0f4231810cd72870))
* **user-repository:** integration testing user repository findByEmail against a postgres testcontainer ([8d0747f](https://github.com/Migas77/ExchangeRate/commit/8d0747fdc84f7026ef538fea367e5c6609d0bb66))
* **user-service:** testing user service lookup, creation and UserDetailsService with a mocked user repository ([4b3bcbc](https://github.com/Migas77/ExchangeRate/commit/4b3bcbce40607687ec4908bca1fb065cc5064a42))

### Documentation

* **swagger:** swagger for authentication bearer and corresponding /api/auth endpoints ([fe62fdd](https://github.com/Migas77/ExchangeRate/commit/fe62fdde8011c6df9d795f514b39e68d4341d5c1))
* **swagger:** swagger schema examples for auth ([6a5fcdc](https://github.com/Migas77/ExchangeRate/commit/6a5fcdceca93eb70c11f9da12308c85d53ce0f28))

### Deployment

* **compose:** add missing env variables to backend container ([8ddc476](https://github.com/Migas77/ExchangeRate/commit/8ddc47655f2df9db20e798e73af3614c6ec7c986))
* **compose:** add postgres container and segregated networks (one for spring-cache connection, another for spring-db connection) ([e56f29c](https://github.com/Migas77/ExchangeRate/commit/e56f29c3051e2dd98a2202557aa293d23f5d0e7e))
* missing migas77 repository (image had only tag) ([b04a6a7](https://github.com/Migas77/ExchangeRate/commit/b04a6a796b3a2679442e843eee7b9dd36f1fd7ad))

### CI

* **release:** create ci profile (spring and maven) for ci actions with default env values ([64aab3a](https://github.com/Migas77/ExchangeRate/commit/64aab3ac0f92aec4a26be3877246ec4f9b5b29ae))

## [1.1.0](https://github.com/Migas77/ExchangeRate/compare/v1.0.0...v1.1.0) (2026-07-31)

### Features

* **caching:** add redis cache with default 1m TTL for caching RatesResponses ([13fa7a9](https://github.com/Migas77/ExchangeRate/commit/13fa7a9279dfb76929387f19d8236cae2afc906e))
* **caching:** correct naming for caching ttl to work with application.properties ([847b436](https://github.com/Migas77/ExchangeRate/commit/847b436c8ec3d20b96eeba78dff1812b3b2e2aa3))
* **caching:** removed lettuce factory overhead caught with intellij profiler (eager init + ping) ([ab4c166](https://github.com/Migas77/ExchangeRate/commit/ab4c16693728c8fbe4c4840f066ad9de9e75b93e))

### Build

* **mvn:** add actuator dependency (for health endpoint) ([1aaee33](https://github.com/Migas77/ExchangeRate/commit/1aaee331f34e1d0228cc3de6d45881a2c71cf3ec))
* **mvn:** add spring-boot-starter-cache and spring-boot-starter-data-redis to dependencies ([5587c8e](https://github.com/Migas77/ExchangeRate/commit/5587c8e970a273d1cbb35be198fac75ac7381f1b))
* **mvn:** add test containers (redis and junit) dependencies ([42f3d6b](https://github.com/Migas77/ExchangeRate/commit/42f3d6be8ba94817905fbc1fcebd99aa27013279))

### Tests

* **caching:** test `@Cacheable` with ConcurrentMapCacheManager and mock upstream client ([91fc4ea](https://github.com/Migas77/ExchangeRate/commit/91fc4ea9621e77cb987a49ab55656eae0174c4c0))
* **caching:** test `@Cacheable` with Redis TestContainer and actual RedisConfig (integration test) ([3568e0b](https://github.com/Migas77/ExchangeRate/commit/3568e0bd0c2b81208af6e187dd03c44e11c2d5e5))

### Deployment

* docker compose ([572bd2c](https://github.com/Migas77/ExchangeRate/commit/572bd2c34ed9c24a5fdddbd5e9e2b771f6a33c90))

### CI

* **actions:** skip integration tests on sonar build ([723f034](https://github.com/Migas77/ExchangeRate/commit/723f03407af5004813c3b346b03f95157da8c590))
* **release:** add tests to release messages ([5074b5e](https://github.com/Migas77/ExchangeRate/commit/5074b5e1be866513c0ccf2b331d62d20532a0e9b))
* **release:** change docker compose image version to next release tag ([444ca2b](https://github.com/Migas77/ExchangeRate/commit/444ca2bbddae50db11062480f4b757d4d4c8f37c))
* **release:** change release description to include more conventional commits types ([76a0b58](https://github.com/Migas77/ExchangeRate/commit/76a0b582d636b42df55ad80f18d785bab4566480))

## 1.0.0 (2026-07-28)

### Features

* **config:** properties config; jackson config; rest client config with default access key and apache request factory ([7de4847](https://github.com/Migas77/ExchangeRate/commit/7de4847fccc26a458eab71520fbddf48c4780184))
* **config:** update application.properties to enable Problem Details and surpress errors in response ([a2abe73](https://github.com/Migas77/ExchangeRate/commit/a2abe73196432373fb7607bb07296b55cdaabf5c))
* **exceptions:** add exception handler for upstream exceptions with structured logging ([a1d17b9](https://github.com/Migas77/ExchangeRate/commit/a1d17b955d99fea91f06280cb4cfc9025fe37550))
* **exceptions:** add GlobalExceptionHandler to handle Generic Exception with ProblemDetails (RFC 9457) and log them using fluent logging API (added structured logs) ([732fc66](https://github.com/Migas77/ExchangeRate/commit/732fc66e396b6a8c0a9053adf45a8517ff2dca26))
* **exceptions:** add read and connect timeout to rest client and corresponding exception handler ([a4712c4](https://github.com/Migas77/ExchangeRate/commit/a4712c4f9c76643ed796837a819dc2af2c12ca36))
* init project ([1316aed](https://github.com/Migas77/ExchangeRate/commit/1316aedab1b3e63a683806dd9dd39d4538b1495e))
* **logging:** customized structured logging with Custom Exception fields by default ([0e6da60](https://github.com/Migas77/ExchangeRate/commit/0e6da600757bda160c18324113de1e861feec103))
* **masking:** added masking structured encoder and logging config to mask access key in logs ([11dbc1c](https://github.com/Migas77/ExchangeRate/commit/11dbc1cbca03cf91a446a48d8e8ab89dc3d75622))
* **rates-api:** controller, repository, client and required payloads and exceptions for getting exchange rate from curr A (to ALL or curr B) ([e5d7299](https://github.com/Migas77/ExchangeRate/commit/e5d729928e9df56f97445ea6d3f20965ee07eaf8))
* **swagger:** swagger docs based on OpenAPI ([5e2a482](https://github.com/Migas77/ExchangeRate/commit/5e2a4822b179e887fc32386826da080a4b3c14ed))

### Bug Fixes

* **timeout:** fix rest client timeout and rest client config injection ([d47d953](https://github.com/Migas77/ExchangeRate/commit/d47d9530cf7ff943581b9daa78c0723eedff8959))
