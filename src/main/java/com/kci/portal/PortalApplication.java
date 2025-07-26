package com.kci.portal;

import com.kci.portal.model.StudentResult;
import com.kci.portal.model.User;
import com.kci.portal.repository.UserRepository;
import com.kci.portal.service.StudentResultService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDate;

@SpringBootApplication
@EnableScheduling
public class PortalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortalApplication.class, args);
	}

	@Bean
	public CommandLineRunner demoData(UserRepository userRepository, StudentResultService resultService) {
		return args -> {
			// Make sure this student exists in the DB
			User student = userRepository.findByEmail("student@gmail.com").orElse(null);
			if (student == null) {
				System.out.println("Test student not found. Skipping result seeding.");
				return;
			}

			StudentResult r1 = new StudentResult();
			r1.setUser(student);
			r1.setModuleTitle("Math");
			r1.setTestName("Algebra Basics");
			r1.setScore(78);
			r1.setPassed(true);
			r1.setDateTaken(LocalDate.now().minusDays(3));

			StudentResult r2 = new StudentResult();
			r2.setUser(student);
			r2.setModuleTitle("Science");
			r2.setTestName("Physics Test");
			r2.setScore(42);
			r2.setPassed(false);
			r2.setDateTaken(LocalDate.now().minusDays(1));

			resultService.saveResult(r1);
			resultService.saveResult(r2);
		};
	}
}
