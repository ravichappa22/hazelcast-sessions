package com.hazelcast.guide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.hazelcast.repository.config.EnableHazelcastRepositories;

@SpringBootApplication
@EnableHazelcastRepositories
public class HazelcastSpringSessionApplication {

	public static void main(String[] args) {
		SpringApplication.run(HazelcastSpringSessionApplication.class, args);
	}

}
