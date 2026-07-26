package com.miguelbf.exchangerateapi.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "spring.application")
@Validated
@Getter
@Setter
public class ApplicationProperties {

	@NotBlank
	@Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Version must match format vX.Y.Z (e.g. 1.2.3)")
	private String version;

}
