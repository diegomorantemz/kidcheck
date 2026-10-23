package pe.edu.kidcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KidCheckApplication {

    public static void main(String[] args) {
        SpringApplication.run(KidCheckApplication.class, args);
    }
}
