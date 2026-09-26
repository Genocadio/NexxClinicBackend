package com.nexxserve.nexxclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexxclinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(NexxclinicApplication.class, args);
	}

}
