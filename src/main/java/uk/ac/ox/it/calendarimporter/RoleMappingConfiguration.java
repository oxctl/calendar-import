package uk.ac.ox.it.calendarimporter;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.GrantedAuthority;

@ConfigurationProperties("calendar.role")
public class RoleMappingConfiguration {

  /** The role mappings from the Canvas role name to the External User Management role. */
  private Map<String, GrantedAuthority> mapping;

  public RoleMappingConfiguration(Map<String, GrantedAuthority> mapping) {
    this.mapping = mapping;
  }

  public Map<String, GrantedAuthority> getMapping() {
    return mapping;
  }
}
