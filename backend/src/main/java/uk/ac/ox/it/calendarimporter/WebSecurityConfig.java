package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity(debug = false)
public class WebSecurityConfig {

    @Value("${spring.data.rest.basePath:/}")
    private String apiPath;

    @Bean
    @Order(10)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(request -> {
                request.requestMatchers("/", "/resources/**", "/favicon.ico", "/icon.png", "/public/**").permitAll();
                request.requestMatchers(apiPath + "/**").hasRole("API");
                request.anyRequest().authenticated();
            })
            .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        return http.build();
    }
}
