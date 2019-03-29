package uk.ac.ox.it.calendarimporter;

import edu.ksu.lti.launch.service.LtiLoginService;
import edu.ksu.lti.launch.service.ToolConsumerService;
import edu.ksu.lti.launch.spring.config.LtiConfigurer;
import edu.ksu.lti.launch.spring.config.LtiLaunchCsrfMatcher;
import java.util.Arrays;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.web.client.RestTemplate;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.endpoint.CanvasOAuth2AuthorizationCodeGrantRequestEntityConverter;
import uk.ac.ox.it.calendarimporter.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;

@Configuration
// The alternative way to debug is to do WebSecurity.debug(true)
@EnableWebSecurity(debug = false)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  @Autowired private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

  @Autowired private LtiLoginService ltiLoginService;

  @Autowired private ToolConsumerService toolConsumerService;

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

    // This is so that we can tell the exception handler that LtiAuthentication is considered to be
    // not full
    // authentication. The AuthenticationTrustResolver is used all over the place, but this is the
    // only one we
    // really care about fixing.
    //        http.getConfigurer(ExceptionHandlingConfigurer.class).withObjectPostProcessor(new
    // ObjectPostProcessor<ExceptionTranslationFilter>() {
    //            @Override
    //            public <O extends ExceptionTranslationFilter> O postProcess(O elt) {
    //                AuthenticationTrustResolverImpl authenticationTrustResolver = new
    // AuthenticationTrustResolverImpl();
    //                authenticationTrustResolver.setRememberMeClass(LtiAuthenticationToken.class);
    //                elt.setAuthenticationTrustResolver(authenticationTrustResolver);
    //                return elt;
    //            }
    //        });
    //        OAuth2LoginConfigurer oauth2 = new OAuth2LoginConfigurer();
    //
    //        // Have to do this with a post processor as we want to get the existing authentication
    // entry point
    //        http.getConfigurer(ExceptionHandlingConfigurer.class).withObjectPostProcessor(new
    // ObjectPostProcessor<ExceptionTranslationFilter>() {
    //            @Override
    //            public <O extends ExceptionTranslationFilter> O postProcess(O elt) {
    //                AuthenticationEntryPoint authenticationEntryPoint =
    // elt.getAuthenticationEntryPoint();
    //                AccessDeniedHandler handler = new
    // SecondChanceAccessDeniedHandler(authenticationEntryPoint, new AccessDeniedHandlerImpl());
    //                elt.setAccessDeniedHandler(handler);
    //                return elt;
    //            }
    //        });

    http.setSharedObject(RequestCache.class, new HttpSessionRequestCache());
    http.setSharedObject(LtiLoginService.class, ltiLoginService);
    LtiConfigurer ltiConfigurer = new LtiConfigurer(toolConsumerService, "/launch", true);
    http.apply(ltiConfigurer);
    http.csrf().requireCsrfProtectionMatcher(new LtiLaunchCsrfMatcher("/launch"));

    http.authorizeRequests()
        .antMatchers("/resources/**")
        .permitAll()
        .and()
        .authorizeRequests()
        .antMatchers("/config.xml")
        .permitAll()
        .and()
        .authorizeRequests()
        .antMatchers("/favicon.ico", "/icon.png")
        .permitAll()
        .and()
        .authorizeRequests()
        .antMatchers("/webjars/**")
        .permitAll()
        .and()
        // TODO This should be authenticated with a different method
        .authorizeRequests()
        .antMatchers("/api/**")
        .permitAll()
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
    //
    // .apply(oauth2).tokenEndpoint().accessTokenResponseClient(accessTokenResposeClient()).and().authorizedClientRepository(oAuth2AuthorizedClientRepository);
    ;
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
  protected void configure(AuthenticationManagerBuilder authenticationManager) throws Exception {
    // This is called before the configure(HttpSecurity)
    // This doesn't work because we don't have our own authentication manager and so it doesn't
    // create it.
    // authenticationManager.authenticationProvider()
    super.configure(authenticationManager);
    authenticationManager.authenticationEventPublisher(
        new DefaultAuthenticationEventPublisher(applicationEventPublisher));
  }

  protected AuthenticationManager authenticationManager() throws Exception {
    return super.authenticationManager();
  }

  public GlobalAuthenticationConfigurerAdapter globalAuthenticationConfigurerAdapter() {
    return new GlobalAuthenticationConfigurerAdapter() {

      public void configure(AuthenticationManagerBuilder builder) {
        builder.authenticationEventPublisher(
            new DefaultAuthenticationEventPublisher(applicationEventPublisher));
      }
    };
  }
}
