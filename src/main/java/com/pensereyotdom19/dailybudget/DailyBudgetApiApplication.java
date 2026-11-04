package com.pensereyotdom19.dailybudget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Starts the server. Spring finds every @Component, @Service, @RestController... below this package. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class DailyBudgetApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(DailyBudgetApiApplication.class, args);
    }
}
