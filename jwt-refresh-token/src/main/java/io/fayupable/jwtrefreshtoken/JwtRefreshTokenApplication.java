package io.fayupable.jwtrefreshtoken;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JwtRefreshTokenApplication {

	public static void main(String[] args) {
		SpringApplication.run(JwtRefreshTokenApplication.class, args);
	}

}
