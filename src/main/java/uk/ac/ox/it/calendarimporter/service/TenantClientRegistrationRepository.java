package uk.ac.ox.it.calendarimporter.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.ClientRegistrationConfiguration;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

/** This loads clients from the Tenant details. */
@Service
public class TenantClientRegistrationRepository implements ClientRegistrationRepository {

  @Autowired private TenantRepository tenantRepository;

  @Autowired private ClientRegistrationConfiguration config;

  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    // The repository should be doing the caching so we don't need to do any caching here.
    Optional<Tenant> optionalTenant = tenantRepository.findByName(registrationId);
    if (optionalTenant.isEmpty()) {
      return null;
    }
    Tenant tenant = optionalTenant.get();

    ClientRegistration registration =
        ClientRegistration.withRegistrationId(tenant.getName())
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri(tenant.getUrl() + config.getAuthorizationUriPath())
            .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
            .clientId(tenant.getOauth2Id())
            .clientName(tenant.getDisplayName())
            .clientSecret(tenant.getOauth2Secret())
            .redirectUriTemplate(config.getRedirectUriTemplate())
            .tokenUri(tenant.getUrl() + config.getTokenUriPath())
            .userInfoUri(tenant.getUrl() + config.getUserInfoUriPath())
            .userNameAttributeName(config.getUserNameAttributeName())
            // You can send scopes and they will be ignored if scoping isn't enabled.
            .scope(config.getScopes())
            .build();

    return registration;
  }
}
