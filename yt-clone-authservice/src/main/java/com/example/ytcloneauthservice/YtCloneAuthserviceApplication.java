package com.example.ytcloneauthservice;

import com.example.ytcloneauthservice.user.AppUser;
import com.example.ytcloneauthservice.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class YtCloneAuthserviceApplication implements CommandLineRunner {

    private final UserRepository userRepository;

    public YtCloneAuthserviceApplication(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(YtCloneAuthserviceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String defaultUser = "user";
        if (userRepository.findByUsername(defaultUser).isPresent()) {
            return;
        }

        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setUsername(defaultUser);
        user.setPassword("$2a$10$mMyb3K3euxMAvyQb9RZ6V./aPbIXHdxd4FGWa.kX7C.Y7Kjh1x.9a"); // "password"
        userRepository.save(user);
    }
}
