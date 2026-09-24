package com.seoulaim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SeoulaimApplication {

  public static void main(String[] args) {
    SpringApplication.run(SeoulaimApplication.class, args);
  }
}
