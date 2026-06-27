package org.example.primemobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PrimeMobileApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrimeMobileApplication.class, args);
	}

}
