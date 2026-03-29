package com.example.jobtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hovedklasse for oppstart av Job Tracker-applikasjonen.
 * <p>
 * Klassen starter Spring Boot-applikasjonen og aktiverer planlagte jobber,
 * som for eksempel periodisk opprydding av tokens.
 */
@EnableScheduling
@SpringBootApplication
public class JobTrackerApplication {

	/**
	 * Starter Spring Boot-applikasjonen.
	 *
	 * @param args kommandolinjeargumenter
	 */
	public static void main(String[] args) {
		SpringApplication.run(JobTrackerApplication.class, args);
	}
}