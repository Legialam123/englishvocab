package com.englishvocab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.englishvocab.config.properties")
public class EnglishvocabApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnglishvocabApplication.class, args);
	}

}
