package uk.ac.ox.it.calendarimporter.termdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;

/**
 * Custom webclient configuration to support OAuth 2.
 */
@Configuration
public class WebClientConfiguration {

	// The is the registration ID to use out of the client repository
	@Value("${client.registrationId:terms}")
	private String registrationId;
	
	@Bean
	@Conditional(ClientsConfiguredCondition.class)
	public WebClient webClient(ReactiveClientRegistrationRepository clientRegistrations) {
		return WebClient.builder()
				.filter(createOAuthFilter(clientRegistrations))
				.build();
	}

	private ServerOAuth2AuthorizedClientExchangeFilterFunction createOAuthFilter(
			ReactiveClientRegistrationRepository clientRegistrations) {
		InMemoryReactiveOAuth2AuthorizedClientService clientService =
				new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations);
		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager =
				new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations,
						clientService);
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
				new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth.setDefaultClientRegistrationId(registrationId);
		return oauth;
	}

	// This normally would be in autoconfiguration, but we need to declare it explicitly
	@Bean
	@Lazy
	InMemoryReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties properties) {
		OAuth2ClientPropertiesMapper mapper = new OAuth2ClientPropertiesMapper(properties);
		return new InMemoryReactiveClientRegistrationRepository(new ArrayList<>(mapper.asClientRegistrations().values()));
	}

	// This normally would be in autoconfiguration, but we need to declare it explicitly
	@Bean
	@Lazy
	ReactiveOAuth2AuthorizedClientService reactiveAuthorizedClientService(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

}
