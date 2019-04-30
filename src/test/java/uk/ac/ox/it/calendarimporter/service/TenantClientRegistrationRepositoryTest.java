package uk.ac.ox.it.calendarimporter.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ox.it.calendarimporter.ClientRegistrationConfiguration;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({TenantClientRegistrationRepository.class, ClientRegistrationConfiguration.class})
public class TenantClientRegistrationRepositoryTest {

    @Autowired
    private TenantClientRegistrationRepository clientRegistrations;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testFindMissingRegistration() {
        ClientRegistration missing = clientRegistrations.findByRegistrationId("does not exist");
        assertNull(missing);
    }

    @Test
    public void testFind() {
        Tenant tenant = new Tenant();
        tenant.setName("test");
        tenant.setUrl("https://example.com");
        tenant.setDisplayName("Test");
        tenant.setOauth2Id("oauth2-id");
        tenant.setOauth2Secret("oauth2-secret");
        entityManager.persist(tenant);

        ClientRegistration registration = clientRegistrations.findByRegistrationId("test");
        assertNotNull(registration);
        assertNotNull(registration.getProviderDetails());

        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());
        assertEquals("https://example.com/login/oauth2/auth", registration.getProviderDetails().getAuthorizationUri());
        assertEquals(ClientAuthenticationMethod.POST, registration.getClientAuthenticationMethod());
        assertEquals("oauth2-id", registration.getClientId());
        assertEquals("Test", registration.getClientName());
        assertEquals("oauth2-secret", registration.getClientSecret());
        assertEquals("{baseUrl}/login/oauth2/code/{registrationId}", registration.getRedirectUriTemplate());
        assertEquals("https://example.com/login/oauth2/token", registration.getProviderDetails().getTokenUri());
        assertEquals("https://example.com/api/v1/users/self", registration.getProviderDetails().getUserInfoEndpoint().getUri());
        assertEquals("login_id", registration.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName());
        assertTrue(registration.getScopes().contains("url:GET|/api/v1/calendar_events"));
        assertTrue(registration.getScopes().contains("url:DELETE|/api/v1/calendar_events/:id"));
        assertTrue(registration.getScopes().contains("url:POST|/api/v1/calendar_events"));
        assertTrue(registration.getScopes().contains("url:GET|/api/v1/courses/:course_id/sections"));
        assertEquals(registration.getScopes().size(), 4);
    }




}
