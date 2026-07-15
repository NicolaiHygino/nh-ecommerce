package com.nicolai.ecommerce.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            // TODO: remove once JWT is implemented
            .requestMatchers("/products/**").permitAll()
            .anyRequest().authenticated())
        // TODO: remove once JWT is implemented
        .csrf(csrf -> csrf.ignoringRequestMatchers("/products/**"));

    return http.build();
  }

}
