package com.example.videobrowsing.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.videobrowsing.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

        @Bean
        @SuppressWarnings("deprecation")
    public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authz -> authz
            // Allow public pages and static assets
            .requestMatchers(
                "/", "/index", "/login", "/register",
                "/video-show", "/video-show/**",
                "/video-detail", "/video-detail/**",
                "/search", "/search/**",
                "/help-center", "/help-center.html",
                "/privacy", "/privacy.html",
                "/report-issue", "/report-issue.html",
                "/css/**", "/js/**", "/images/**",
                "/uploads/thumbnails/**", "/uploads/images/**"
            ).permitAll()
            // Require authentication to watch videos or access raw media files
            .requestMatchers(
                "/video/**", "/uploads/videos/**"
            ).authenticated()
            // Allow API endpoints that handle their own session checks
            .requestMatchers("/api/**").permitAll()
            // All other requests require authentication
            .anyRequest().authenticated()
        )
                .formLogin(form -> form
                        .loginPage("/login")
            .successHandler(authenticationSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }
}