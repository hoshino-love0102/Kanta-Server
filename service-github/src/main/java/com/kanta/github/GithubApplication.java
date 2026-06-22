package com.kanta.github;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GithubApplication {
    public static void main(String[] args) {
        SpringApplication.run(GithubApplication.class, args);
    }
}
