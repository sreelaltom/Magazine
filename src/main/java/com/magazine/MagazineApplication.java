package com.magazine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MagazineApplication {

    public static void main(String[] args) {
        SpringApplication.run(MagazineApplication.class, args);
        System.out.println("==================================================================");
        System.out.println(">>> Magazine Portal Application is RUNNING!");
        System.out.println(">>> Home Page (Modern Cards): http://localhost:8080");
        System.out.println(">>> Admin Login Page:        http://localhost:8080/admin/login");
        System.out.println(">>> Admin Credentials:       admin / password123");
        System.out.println(">>> H2 Database Console:     http://localhost:8080/h2-console");
        System.out.println("==================================================================");
    }
}
