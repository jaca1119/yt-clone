package com.example.ytcloneauthservice.user;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            userService.register(registerRequest.getUsername(), registerRequest.getPassword());
            return "redirect:/login?registered=true";
        } catch (UserAlreadyExistException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
