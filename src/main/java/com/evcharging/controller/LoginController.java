package com.evcharging.controller;

import com.evcharging.model.User;
import com.evcharging.repository.UserRepository;
import com.evcharging.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

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
            if (user.getRole().equals("ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (user.getRole().equals("USER")) {
                return "redirect:/user/home";
            }
        }

        
        return "redirect:/login?error"; // fallback
    }
}
