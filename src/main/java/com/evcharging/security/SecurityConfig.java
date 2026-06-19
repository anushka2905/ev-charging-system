package com.evcharging.security;

import com.evcharging.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Phase 0 enhanced + AI endpoints secured.
 *
 * Security matrix:
 * ┌─────────────────────────────────────┬──────────────────────┐
 * │ Endpoint Pattern                    │ Required Role        │
 * ├─────────────────────────────────────┼──────────────────────┤
 * │ /api/ai/health                      │ ADMIN                │
 * │ /api/ai/analytics/**                │ ADMIN                │
 * │ /api/ai/chat                        │ USER or ADMIN        │
 * │ /api/ai/book                        │ USER or ADMIN        │
 * │ /api/ai/recommend                   │ Any authenticated    │
 * │ /api/ai/rag/**                      │ Any authenticated    │
 * │ /api/ai/ask                         │ Any authenticated    │
 * └─────────────────────────────────────┴──────────────────────┘
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private CustomLoginSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // disabled for REST API; use CSRF token in production
            .authorizeHttpRequests(auth -> auth
                // ── Public access ──────────────────────────────
                .requestMatchers(
                    "/", "/register", "/login",
                    "/stations/list",
                    "/css/**", "/js/**", "/images/**",
                    "/webjars/**", "/actuator/health"
                ).permitAll()

                // ── Admin-only ─────────────────────────────────
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/ai/health").hasRole("ADMIN")
                .requestMatchers("/api/ai/analytics/**").hasRole("ADMIN")

                // ── Any authenticated user ─────────────────────
                .requestMatchers("/api/ai/**").authenticated()
                .requestMatchers("/user/**").authenticated()
                .requestMatchers("/booking/**", "/payment/**").authenticated()

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .authenticationProvider(authenticationProvider());

        return http.build();
    }
}
