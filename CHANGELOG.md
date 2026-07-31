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
