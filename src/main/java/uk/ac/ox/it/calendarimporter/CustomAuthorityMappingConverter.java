package uk.ac.ox.it.calendarimporter;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * This extracts roles out of a LTI 1.3 JWT that are passed through in the custom claims. Mapping
 * claims to roles makes it easier to test the code.
 */
class CustomAuthorityMappingConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  // The default field in the custom claims to look for the roles in.
  private static final String DEFAULT_ROLES_CLAIM_FIELD = "canvas_membership_roles";

  private final Map<String, GrantedAuthority> mappings;
  private String rolesClaimField = DEFAULT_ROLES_CLAIM_FIELD;

  public CustomAuthorityMappingConverter(Map<String, GrantedAuthority> mappings) {
    Objects.requireNonNull(mappings, "You must supply some mappings");
    this.mappings = new HashMap<>();
    this.mappings.putAll(mappings);
  }

  public void setRolesClaimField(String rolesClaimField) {
    this.rolesClaimField = rolesClaimField;
  }

  @Override
  public Collection<GrantedAuthority> convert(Jwt source) {
    Map<String, Object> customClaims =
        source.getClaimAsMap("https://purl.imsglobal.org/spec/lti/claim/custom");
    Collection<GrantedAuthority> authorities = new HashSet<>();
    if (customClaims != null) {
      convertRoles(customClaims, authorities);
    }
    return authorities;
  }

  private void convertRoles(
      Map<String, Object> customClaims, Collection<GrantedAuthority> authorities) {
    Object claim = customClaims.get(rolesClaimField);
    if (claim instanceof String) {
      String[] splitRoles = ((String) claim).split(",");
      for (String role : splitRoles) {
        GrantedAuthority authority = mappings.get(role);
        if (authority != null) {
          authorities.add(authority);
        }
      }
    }
  }
}
