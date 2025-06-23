// src/main/java/com/eshop/frontend/config/SecurityConfig.java
package com.eshop.frontend.config;

import com.eshop.frontend.security.AuthServiceAuthenticationProvider;
// import com.eshop.frontend.security.LoginSuccessHandler; // REMOVE THIS IMPORT
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RestTemplate restTemplate;
    // private final LoginSuccessHandler loginSuccessHandler; // REMOVE THIS FIELD

    // Adjust constructor to no longer require LoginSuccessHandler
    public SecurityConfig(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Bean
    public AuthenticationProvider authServiceAuthenticationProvider() {
        return new AuthServiceAuthenticationProvider(restTemplate);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                // .successHandler(loginSuccessHandler) // REMOVE THIS LINE
                .defaultSuccessUrl("/home", true) // Use default success URL
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable()); // Keep as disable for now, enable in production with proper handling

        http.securityContext((securityContext) -> securityContext
            .securityContextRepository(securityContextRepository())
        );

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}