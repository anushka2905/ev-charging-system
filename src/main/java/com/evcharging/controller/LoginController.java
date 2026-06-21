package com.evcharging.controller;

import com.evcharging.model.User;
import com.evcharging.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Redirect user after login based on role
    @GetMapping("/home")
    public String redirectBasedOnRole(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            model.addAttribute("user", user);
            String role = user.getRole();
            // role is stored as "ROLE_ADMIN" or "ROLE_USER"
            if ("ROLE_ADMIN".equals(role) || "ADMIN".equals(role)) {
                return "redirect:/admin/dashboard";
            } else if ("ROLE_USER".equals(role) || "USER".equals(role)) {
                return "redirect:/user/home";
            }
        }

        return "redirect:/login?error"; // fallback
    }
}
