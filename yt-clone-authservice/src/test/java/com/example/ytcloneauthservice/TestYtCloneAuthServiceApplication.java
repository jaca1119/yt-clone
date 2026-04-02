package com.example.ytcloneauthservice;

import org.springframework.boot.SpringApplication;

public class TestYtCloneAuthServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(YtCloneAuthserviceApplication::main).with(TestContainersConfiguration.class).run(args);
    }
}
