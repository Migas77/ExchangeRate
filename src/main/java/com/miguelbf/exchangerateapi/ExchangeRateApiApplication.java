package com.miguelbf.exchangerateapi;

import com.miguelbf.exchangerateapi.config.properties.ApplicationProperties;
import com.miguelbf.exchangerateapi.config.properties.ExchangeRatesClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class, ExchangeRatesClientProperties.class})
public class ExchangeRateApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeRateApiApplication.class, args);
    }

}
