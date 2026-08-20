package com.picpaysimplificado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PicpaySimplificadoApplication {

	/**
	 * [PT-BR] Ponto de entrada principal da aplicação Spring Boot. Inicializa o contexto Spring, conexão com PostgreSQL e RabbitMQ.
	 * [EN]    Main entry point for the Spring Boot application. Initializes Spring context, PostgreSQL connection, and RabbitMQ.
	 *
	 * @param args Argumentos de linha de comando / Command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PicpaySimplificadoApplication.class, args);
	}
}
