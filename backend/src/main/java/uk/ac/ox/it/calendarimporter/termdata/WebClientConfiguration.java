package uk.ac.ox.it.calendarimporter.termdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom webclient configuration to support OAuth 2.
 */
@Configuration
public class WebClientConfiguration {

	// The is the registration ID to use out of the client repository
	@Value("${client.registrationId:terms}")
	private String registrationId;
	
	@Bean
	public WebClient webClient(ReactiveClientRegistrationRepository clientRegistrations) {
		// This custom webclient is so that we use Azure AD OAuth when making requests.
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
				new ServerOAuth2AuthorizedClientExchangeFilterFunction(
						clientRegistrations,
						new UnAuthenticatedServerOAuth2AuthorizedClientRepository());

		oauth.setDefaultClientRegistrationId(registrationId);
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(
						HttpClient.create().wiretap(true)
				))
				.filter(oauth)
				.build();
	}

	// This normally would be in autoconfiguration, but we need to declare it explicitly
	@Bean
	InMemoryReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties properties) {
		List<ClientRegistration> registrations = new ArrayList<>(
				OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties).values());
		return new InMemoryReactiveClientRegistrationRepository(registrations);
	}

	// This normally would be in autoconfiguration, but we need to declare it explicitly
	@Bean
	ReactiveOAuth2AuthorizedClientService reactiveAuthorizedClientService(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

}
