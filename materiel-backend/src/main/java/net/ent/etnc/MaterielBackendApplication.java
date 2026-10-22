package net.ent.etnc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API de gestion des matériels.
 */
@SpringBootApplication
public class MaterielBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaterielBackendApplication.class, args);
    }
}