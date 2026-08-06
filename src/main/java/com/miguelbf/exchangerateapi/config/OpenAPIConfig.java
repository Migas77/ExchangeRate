package com.miguelbf.exchangerateapi.config;

import com.miguelbf.exchangerateapi.config.properties.ApplicationProperties;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AllArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
@AllArgsConstructor
public class OpenAPIConfig {

    private static final String BEARER_AUTH_SCHEME = "bearerAuth";
    private static final String AUTH_PATH_PREFIX = "/api/auth";
    private static final String GATEWAY_BASE_URL = "http://localhost:8080/gw";

    ApplicationProperties applicationProperties;

    static {
        SpringDocUtils.getConfig().replaceWithSchema(
            BigDecimal.class,
            new Schema<BigDecimal>().type("number").format("decimal"));
    }

    @Bean
    public OpenAPI exchangeRatesAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Exchange Rates API")
                .version(applicationProperties.getVersion())
                .description("""
                    This is the swagger documentation for my Exchange Rates API based on the Open API spec. \
                    The API fetches currency exchange-rates from a publicly available API provided by \
                    [exchangerate.host](https://exchangerate.host/documentation), and also supports currency \
                    conversion calculations. Standard API errors are expressed through the `Problem Details` data type \
                    according to [RFC 9457 Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc9457).
                    
                    ## Base Path & Rate Limiting
                    
                    All endpoints are served under the `/gw` base path, which strips the `/gw` prefix and \
                    forwards the remaining path internally, while applying a per-caller rate limit \
                    of 10 requests per minute, keyed by user for authenticated requests, and by IP otherwise. \
                    Exceeding it returns `429 Too Many Requests`. The paths below are relative to that base \
                    path, so the operation documented as `/api/rates` is called as `GET /gw/api/rates`.
                    
                    ## Authentication & Authorization
                    
                    Every endpoint is protected and requires a JWT bearer token, except \
                    `/gw/api/auth`, `.well-known/oauth-protected-resource`, `/swagger-ui/**`, `/v3/api-docs/**` and \
                    `/actuator/health`.
                    
                    1. Create an account with `POST /gw/api/auth/signup`, or authenticate an existing one with \
                    `POST /gw/api/auth/login`. Both return an `accessToken`, a `refreshToken` and `expiresIn`.
                    
                    2. Send the access token on every subsequent request in the `Authorization` header:
                    `Authorization: Bearer <accessToken>`.
                    
                    3. When the access token expires, exchange the refresh token for a new access token \
                    with `POST /gw/api/auth/refresh`.
                    
                    In the **Swagger UI**, click **Authorize** and paste the raw `accessToken` (without the \
                    `Bearer ` prefix) to have it sent automatically. Passing refresh token here will be accepted by \
                    swagger but rejected at the API level with `401`. Authentication and authorization failures \
                    follow [RFC 6750 Section 3 WWW-Authenticate response headers](https://datatracker.ietf.org/doc/html/rfc6750#section-3), \
                    describing the failure in the `WWW-Authenticate` response header.
                    """)
                .contact(new Contact()
                    .name("the developer")
                    .email("miguel.belchior@ua.pt"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .externalDocs(new ExternalDocumentation()
                .description("Full Documentation (Github)")
                .url("https://github.com/Migas77/ExchangeRate"))
            .servers(List.of(
                new Server().url(GATEWAY_BASE_URL).description("Development (HTTP)")
            ))
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .in(SecurityScheme.In.HEADER)
                    .name("Authorization")
                    .description("""
                        HMAC-signed JWT access token sent as `Authorization: Bearer <accessToken>` and obtained from:
                        - `POST /gw/api/auth/signup`
                        - `POST /gw/api/auth/login`
                        - `POST /gw/api/auth/refresh`
                        
                        Refresh tokens with claim `"type": "refresh"` will be explicitly rejected by the api \
                        being only accepted in the body of the refresh token endpoint: `POST /gw/api/auth/refresh`.""")))
            // opt out with @SecurityRequirements({})
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }

    @Bean
    public OperationCustomizer globalErrorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();

            if (hasEndpointPrefix(handlerMethod, AUTH_PATH_PREFIX)) {
                // Gateway errors only when depending on upstream API
                responses.remove("502");
                responses.remove("504");
            }

            responses.addApiResponse("500", new ApiResponse()
                .description("Internal Server Error.")
                .content(new Content().addMediaType("application/problem+json",
                    new MediaType()
                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                        .addExamples("Internal Server Error", new Example()
                            .value("""
                                {
                                  "type": "about:blank",
                                  "title": "Internal Server Error",
                                  "status": 500,
                                  "detail": "An unexpected error occurred. Please try again later.",
                                  "instance": "/api/rates"
                                }
                                """)
                        )
                )));

            // if this endpoint did not opt out of the global bearer requirement with @SecurityRequirements({})
            if (handlerMethod.getMethodAnnotation(SecurityRequirements.class) == null
                && handlerMethod.getBeanType().getAnnotation(SecurityRequirements.class) == null) {

                responses.addApiResponse("401", new ApiResponse()
                    .description("""
                        Unauthorized — the `Authorization` header is missing, or the bearer token is \
                        malformed, expired or a refresh token. The body is empty; the reason is carried \
                        by the `WWW-Authenticate` header \
                        ([RFC 6750 Section 3 WWW-Authenticate response headers]\
                        (https://datatracker.ietf.org/doc/html/rfc6750#section-3)).""")
                    .addHeaderObject("WWW-Authenticate", bearerChallengeHeader(
                        "Bearer error=\"invalid_token\", "
                            + "error_description=\"An error occurred while attempting to decode the Jwt: "
                            + "Signed JWT rejected: Invalid signature\", "
                            + "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\", "
                            + "resource_metadata=\"http://localhost:8080/.well-known/oauth-protected-resource\"")));

                responses.addApiResponse("403", new ApiResponse()
                    .description("""
                        Forbidden — the bearer token is valid, but the authenticated user's role does not \
                        grant access to this resource. The body is empty; the reason is carried by the \
                        `WWW-Authenticate` header ([RFC 6750 Section 3 WWW-Authenticate response headers]\
                        (https://datatracker.ietf.org/doc/html/rfc6750#section-3)).""")
                    .addHeaderObject("WWW-Authenticate", bearerChallengeHeader(
                        "Bearer error=\"insufficient_scope\", "
                            + "error_description=\"The request requires higher privileges than provided by "
                            + "the access token.\", "
                            + "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"")));
            }

            return operation;
        };
    }

    private boolean hasEndpointPrefix(HandlerMethod handlerMethod, String pathPrefix) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(
            handlerMethod.getBeanType(), RequestMapping.class);
        if (requestMapping == null) {
            return false;
        }
        return Stream.concat(Arrays.stream(requestMapping.value()), Arrays.stream(requestMapping.path()))
            .anyMatch(path -> path.startsWith(pathPrefix));
    }

    private Header bearerChallengeHeader(String example) {
        return new Header()
            .description("""
                Bearer challenge as defined by [RFC 6750 Sec. 3]\
                (https://datatracker.ietf.org/doc/html/rfc6750#section-3)""")
            .schema(new StringSchema().example(example));
    }

    @Bean
    public OpenApiCustomizer problemDetailSchemaCustomizer() {
        return openApi -> {
            Schema<?> schema = openApi.getComponents().getSchemas().get("ProblemDetail");
            if (schema != null) {
                schema.setDescription("""
                    [RFC 9457](https://datatracker.ietf.org/doc/html/rfc9457) problem details object returned for\
                     all error responses.""");
                if (schema.getProperties() != null) {
                    Map<String, Schema> props = schema.getProperties();

                    setExample(props, "type", "about:blank");
                    setExample(props, "title", "Internal Server Error");
                    setExample(props, "status", 500);
                    setExample(props, "detail", "An unexpected error occurred. Please try again later.");
                    setExample(props, "instance", "/api/rates");
                }
            }
        };
    }

    private void setExample(Map<String, Schema> props, String field, Object example) {
        Schema<?> propSchema = props.get(field);
        if (propSchema != null) {
            propSchema.setExample(example);
        }
    }

}
