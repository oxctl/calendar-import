package uk.ac.ox.it.calendarimporter.persistence.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TenantTest {

    /**
     * We want to make sure we aren't ever writing out the secrets if we do a toString()
     */
    @Test
    public void testToString() {
        Tenant tenant = new Tenant();
        tenant.setId(1);
        tenant.setName("name");
        tenant.setDisplayName("Display Name");
        tenant.setUrl("http://example.url/");
        tenant.setLtiSecret("secret");
        tenant.setOauth2Id("oauth-id");
        tenant.setOauth2Secret("secret");

        String s = tenant.toString();
        assertFalse(s.contains("secret"));
    }
}
