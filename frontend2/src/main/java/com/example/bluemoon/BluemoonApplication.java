package com.example.bluemoon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class BluemoonApplication {

	public static void main(String[] args) {
		SpringApplication.run(BluemoonApplication.class, args);
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

		System.out.println("password:" + encoder.encode("123456"));
	}

}
