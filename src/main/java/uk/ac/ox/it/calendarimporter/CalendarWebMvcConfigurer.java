package uk.ac.ox.it.calendarimporter;

import com.samskivert.mustache.Mustache;
import edu.ksu.lti.launch.service.LtiLoginService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.mustache.MustacheEnvironmentCollector;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;
import uk.ac.ox.it.calendarimporter.controller.CustomErrorController;
import uk.ac.ox.it.calendarimporter.security.oauth2.client.web.method.annotation.OAuth2AuthorizedClientArgumentResolver;
import uk.ac.ox.it.calendarimporter.support.LtiSessionArgumentResolver;

@Configuration
public class CalendarWebMvcConfigurer implements WebMvcConfigurer {

  @Autowired private ClientRegistrationRepository clientRegistrationRepository;

  @Autowired private OAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

  @Autowired private LtiLoginService ltiLoginService;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    // This is because we copied a whole load of spring stuff and so we need do our own resolving.
    argumentResolvers.add(
        new OAuth2AuthorizedClientArgumentResolver(
            clientRegistrationRepository, oAuth2AuthorizedClientRepository));
    // Allow LTI Session to be resolved.
    argumentResolvers.add(new LtiSessionArgumentResolver(ltiLoginService));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // Tell clients to cache all the CSS we serve up for 1 hour.
    // This is because CSS output by a controller is marked as uncachable otherwise.
    WebContentInterceptor interceptor = new WebContentInterceptor();
    interceptor.addCacheMapping(CacheControl.maxAge(1, TimeUnit.HOURS), "/**/*.css");
    interceptor.addCacheMapping(CacheControl.maxAge(1, TimeUnit.HOURS), "/**/*.js");
    registry.addInterceptor(interceptor);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // We currently have jquery being used from a webjar.
    registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/");
  }

  @Bean()
  @Lazy
  public Mustache.Compiler mustacheCompiler(
      Mustache.TemplateLoader templateLoader, Environment environment) {

    MustacheEnvironmentCollector collector = new MustacheEnvironmentCollector();
    collector.setEnvironment(environment);

    return Mustache.compiler()
        .defaultValue("Some Default Value")
        .withLoader(templateLoader)
        .withCollector(collector);
  }

  // This is conditional so that when using @WebMvcTest we don't try to setup error handling and
  // fail.
  @Configuration
  @ConditionalOnBean(DispatcherServlet.class)
  class ErrorConfiguration {

    @Autowired private ServerProperties serverProperties;

    @Autowired private List<ErrorViewResolver> errorViewResolvers;

    @Bean
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes) {
      return new CustomErrorController(
          errorAttributes, this.serverProperties.getError(), this.errorViewResolvers);
    }

    @Bean
    public CustomErrorAttributes errorAttributes() {
      return new CustomErrorAttributes(this.serverProperties.getError().isIncludeException());
    }
  }
}
