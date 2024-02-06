package uk.ac.ox.it.calendarimporter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.Map;

@ConfigurationProperties("calendar.role")
public class RoleMappingConfiguration {

    /**
     * The role mappings from the Canvas role name to the External User Management role.
     */
    private final Map<String, GrantedAuthority> mapping;

    public RoleMappingConfiguration(Map<String, GrantedAuthority> mapping) {
        this.mapping = mapping;
    }

    public Map<String, GrantedAuthority> getMapping() {
        if (mapping != null) return mapping;
        return Collections.EMPTY_MAP;
    }
}
