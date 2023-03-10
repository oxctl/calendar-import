package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

@Order(10)
@Configuration
@EnableWebSecurity(debug = false)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${spring.data.rest.basePath:/}")
    private String apiPath;

    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        super.configure(webSecurity);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure(HttpSecurity http) throws Exception {

        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPointMap = new LinkedHashMap<>();
        BasicAuthenticationEntryPoint basicAuthenticationEntryPoint =
                new BasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("API");
        entryPointMap.put(new AntPathRequestMatcher(apiPath + "/**"), basicAuthenticationEntryPoint);

        http.authorizeRequests()
                .antMatchers("/", "/resources/**", "/favicon.ico", "/icon.png", "/public/**")
                .permitAll()
                .and()
                // TODO Should prevent LTI from working here so that even if a user comes across with this
                // role they can't access the APM
                .authorizeRequests()
                .antMatchers(apiPath + "/**")
                .hasRole("API")
                .and()
                .authorizeRequests()
                .anyRequest()
                .authenticated()
                .and()
                // TODO Make better
                .headers()
                .frameOptions()
                .disable();
    }

    /**
     * This just calls the autoconfigurer as it's skipped because we have OAuth configured. This sets
     * up a user and if the password isn't specified creates one and writes it to the logs.
     */
    @Bean
    @Lazy
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(
            SecurityProperties properties, ObjectProvider<PasswordEncoder> passwordEncoder) {
        return new UserDetailsServiceAutoConfiguration()
                .inMemoryUserDetailsManager(properties, passwordEncoder);
    }
}
