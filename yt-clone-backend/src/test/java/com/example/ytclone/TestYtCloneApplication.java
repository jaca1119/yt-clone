package com.example.ytclone;

import org.springframework.boot.SpringApplication;

public class TestYtCloneApplication {

  public static void main(String[] args) {
    SpringApplication.from(YtCloneApplication::main).with(TestcontainersConfiguration.class).run(args);
  }

}

