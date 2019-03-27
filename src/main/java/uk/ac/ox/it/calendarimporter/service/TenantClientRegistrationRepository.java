package uk.ac.ox.it.calendarimporter.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Service;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;

/** This loads clients from the Tenant details. */
@Service
public class TenantClientRegistrationRepository implements ClientRegistrationRepository {

  @Autowired private TenantRepository tenantRepository;

  @Override
  public ClientRegistration findByRegistrationId(String registrationId) {
    // TODO We probably want to make this method cachable or
    Optional<Tenant> optionalTenant = tenantRepository.findByName(registrationId);
    if (optionalTenant.isEmpty()) {
      return null;
    }
    Tenant tenant = optionalTenant.get();

    // TODO We should pull this from config
    ClientRegistration registration =
        ClientRegistration.withRegistrationId(tenant.getName())
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri(tenant.getUrl() + "/login/oauth2/auth")
            .clientAuthenticationMethod(ClientAuthenticationMethod.POST)
            .clientId(tenant.getOauth2Id())
            .clientName(tenant.getDisplayName())
            .clientSecret(tenant.getOauth2Secret())
            .redirectUriTemplate("{baseUrl}/login/oauth2/code/{registrationId}")
            .tokenUri(tenant.getUrl() + "/login/oauth2/token")
            .userInfoUri(tenant.getUrl() + "/api/v1/users/self")
            .userNameAttributeName("login_id")
            .build();

    return registration;
  }
}
