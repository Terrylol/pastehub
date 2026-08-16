package app.pastehub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PastehubApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PastehubApiApplication.class, args);
	}

}
