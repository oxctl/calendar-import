package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Value("${spring.data.rest.basePath:/}")
    private String apiPath;

    @Bean
    @Order(10)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/",
                        "/error",
                        "/index.html",
                        "/resources/**",
                        "/favicon.ico",
                        "/icon.png",
                        "/public/**",
                        "/actuator/health"
                ).permitAll());
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll());

        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
    
    @Bean
    @Order(3)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher(apiPath + "/**")
                .authorizeHttpRequests(request -> request.anyRequest().hasRole("API"))
                .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }
}
