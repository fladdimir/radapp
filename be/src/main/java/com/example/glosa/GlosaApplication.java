package com.example.glosa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
@EnableCaching
public class GlosaApplication {

	public static void main(String[] args) {
		SpringApplication.run(GlosaApplication.class, args);
	}

}
