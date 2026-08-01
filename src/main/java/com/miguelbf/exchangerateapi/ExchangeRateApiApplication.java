package com.miguelbf.exchangerateapi;

import com.miguelbf.exchangerateapi.config.properties.ApplicationProperties;
import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import com.miguelbf.exchangerateapi.config.properties.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    ApplicationProperties.class,
    ExchangeRatesClientProperties.class,
    JwtProperties.class
})
public class ExchangeRateApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeRateApiApplication.class, args);
    }

}
