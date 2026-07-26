package com.miguelbf.exchangerateapi.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AllArgsConstructor;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@AllArgsConstructor
public class OpenAPIConfig {

	ApplicationProperties applicationProperties;

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
					conversion calculations.
					""")
				.contact(new Contact()
					.name("the developer")
					.email("miguel.belchior@ua.pt"))
				.license(new License()
					.name("MIT License")
					.url("https://opensource.org/licenses/MIT")))
			.externalDocs(new ExternalDocumentation()
				.description("Full Documentation")
				.url("https://github.com/Migas77/ExchangeRate"))
			.servers(List.of(
				new Server().url("http://localhost:8080").description("Development (HTTP)")
			));
	}

	@Bean
	public OperationCustomizer globalErrorResponsesCustomizer() {
		return (operation, handlerMethod) -> {
			ApiResponses responses = operation.getResponses();

			responses.addApiResponse("400", new ApiResponse()
				.description("Bad request — Invalid request parameters.")
				.content(new Content().addMediaType("application/problem+json",
					new MediaType()
						.schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
						.addExamples("BadRequestExample", new Example()
							.value("""
								{
								  "type": "about:blank",
								  "title": "Bad Request",
								  "status": 400,
								  "detail": "Failed to convert 'source' with value: 'null'",
								  "instance": "/api/rates"
								}
								""")
						)
				)));

			responses.addApiResponse("500", new ApiResponse()
				.description("Internal Server Error.")
				.content(new Content().addMediaType("application/problem+json",
					new MediaType()
						.schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
						.addExamples("InternalServerErrorExample", new Example()
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

			return operation;
		};
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
