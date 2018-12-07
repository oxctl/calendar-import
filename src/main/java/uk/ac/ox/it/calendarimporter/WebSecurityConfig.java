package uk.ac.ox.it.calendarimporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.ProviderManagerBuilder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;

@Configuration
//@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;


    @Override
    public void configure(WebSecurity webSecurity) throws Exception {
        super.configure(webSecurity);

    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationEventPublisher(new DefaultAuthenticationEventPublisher(applicationEventPublisher));
        http
                .authorizeRequests()
                .anyRequest().authenticated()
                .and().oauth2Login();

    }


    @Override
    protected  void configure(AuthenticationManagerBuilder authenticationManager) throws Exception {
        // This is called before the configure(HttpSecurity)
        // This doesn't work because we don't have our own authentication manager and so it doesn't create it.
        // authenticationManager.authenticationProvider()
        super.configure(authenticationManager);
        authenticationManager.authenticationEventPublisher(new DefaultAuthenticationEventPublisher(applicationEventPublisher));
    }

    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    // This doesn't work because the authentication manager isn't built by the configurer.
    @Bean
    public GlobalAuthenticationConfigurerAdapter globalAuthenticationConfigurerAdapter() {
        return new GlobalAuthenticationConfigurerAdapter(){

            public void configure(AuthenticationManagerBuilder builder) {
                builder.authenticationEventPublisher(new DefaultAuthenticationEventPublisher(applicationEventPublisher));
            }

        };
    }

}
