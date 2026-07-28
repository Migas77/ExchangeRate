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
