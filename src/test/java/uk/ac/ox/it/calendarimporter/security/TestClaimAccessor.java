package uk.ac.ox.it.calendarimporter.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

import java.util.List;
import java.util.Map;

public class TestClaimAccessor implements JwtClaimAccessor {
    private final String username;
    private List<GrantedAuthority> grantedAuthorities;
    private final Map<String, Object> claims;

    public TestClaimAccessor(String username, List<GrantedAuthority> grantedAuthorities, Map<String, Object> claims) {
        this.username = username;
        this.grantedAuthorities = grantedAuthorities;
        this.claims = claims;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public Map<String, Object> getClaims() {
        return claims;
    }

    public List<GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }
}
