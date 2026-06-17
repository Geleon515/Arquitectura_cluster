package arqui.grupo5.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VendiaWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendiaWebApplication.class, args);
        System.out.println("=== SERVIDOR WEB INICIADO EN http://localhost:8080 ===");
    }
}