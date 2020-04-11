package uk.ac.ox.it.calendarimporter;

import edu.ksu.lti.launch.service.LtiLoginService;
import edu.ksu.lti.launch.service.ToolConsumerService;
import edu.ksu.lti.launch.spring.config.LtiConfigurer;
import edu.ksu.lti.launch.spring.config.LtiLaunchCsrfMatcher;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter;
import uk.ac.ox.it.calendarimporter.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

@Configuration
@EnableWebSecurity(debug = false)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  @Autowired private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

  @Autowired private LtiLoginService ltiLoginService;

  @Autowired private ToolConsumerService toolConsumerService;

  @Value("${spring.lti.launch.path:/launch}")
  private String ltiLaunchPath;

  @Value("${spring.data.rest.basePath:/}")
  private String apiPath;

  @Override
  public void configure(WebSecurity webSecurity) throws Exception {
    super.configure(webSecurity);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void configure(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder =
        http.getSharedObject(AuthenticationManagerBuilder.class);
    authenticationManagerBuilder.authenticationEventPublisher(
        new DefaultAuthenticationEventPublisher(applicationEventPublisher));

    http.setSharedObject(RequestCache.class, new HttpSessionRequestCache());
    http.setSharedObject(LtiLoginService.class, ltiLoginService);
    LtiConfigurer ltiConfigurer =
        new LtiConfigurer(toolConsumerService, ltiLaunchPath, true, "/error");
    http.apply(ltiConfigurer);
    http.csrf()
        .requireCsrfProtectionMatcher(
            new AndRequestMatcher(
                new LtiLaunchCsrfMatcher(ltiLaunchPath),
                new NegatedRequestMatcher(new AntPathRequestMatcher(apiPath + "/**"))));

    LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPointMap = new LinkedHashMap<>();
    BasicAuthenticationEntryPoint basicAuthenticationEntryPoint =
        new BasicAuthenticationEntryPoint();
    basicAuthenticationEntryPoint.setRealmName("API");
    entryPointMap.put(new AntPathRequestMatcher(apiPath + "/**"), basicAuthenticationEntryPoint);

    DelegatingAuthenticationEntryPoint authenticationEntryPoint =
        new DelegatingAuthenticationEntryPoint(entryPointMap);
    LtiEntryPointImpl ltiEntryPoint = new LtiEntryPointImpl();
    ltiEntryPoint.setErrorPage("/error");
    authenticationEntryPoint.setDefaultEntryPoint(ltiEntryPoint);
    http.authorizeRequests()
        .antMatchers(
            "/", "/resources/**", "/config.xml", "/favicon.ico", "/icon.png", "/webjars/**")
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
        .disable()
        .and()
        .oauth2Client()
        .authorizedClientRepository(oAuth2AuthorizedClientRepository)
        .authorizationCodeGrant()
        .accessTokenResponseClient(accessTokenResposeClient())
        .and()
        // We only want to prompt for authentication on some URLs.
        .and()
        .httpBasic()
        .authenticationEntryPoint(authenticationEntryPoint);
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

  // This is so we can remove old tokens.
  private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      accessTokenResposeClient() {
    DefaultAuthorizationCodeTokenResponseClient client =
        new DefaultAuthorizationCodeTokenResponseClient();
    client.setRequestEntityConverter(
        new CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter());
    RestTemplate restTemplate =
        new RestTemplate(
            Arrays.asList(
                new FormHttpMessageConverter(),
                new OAuth2AccessTokenResponseHttpMessageConverter()));
    // Switch to Apache HTTP Components;
    HttpClient requestFactory = HttpClientBuilder.create().disableContentCompression().build();
    restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(requestFactory));
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    client.setRestOperations(restTemplate);
    return client;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder builder) throws Exception {
    // This is called before the configure(HttpSecurity)
    // This doesn't work because we don't have our own authentication manager and so it doesn't
    // create it.
    // builder.authenticationProvider()
    super.configure(builder);
    builder
        .inMemoryAuthentication()
        .and()
        .authenticationEventPublisher(
            new DefaultAuthenticationEventPublisher(applicationEventPublisher));
  }
}
