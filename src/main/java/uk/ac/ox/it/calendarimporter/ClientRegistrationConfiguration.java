package uk.ac.ox.it.calendarimporter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * We're storing most details of the client in the DB and these are just the common across all of them.
 */
@Configuration
@ConfigurationProperties(prefix = "canvas.client")
public class ClientRegistrationConfiguration {

    private String authorizationUriPath;
    private String redirectUriTemplate;
    private String tokenUriPath;
    private String userInfoUriPath;
    private String userNameAttributeName;
    private List<String> scopes;

    public String getAuthorizationUriPath() {
        return authorizationUriPath;
    }

    public void setAuthorizationUriPath(String authorizationUriPath) {
        this.authorizationUriPath = authorizationUriPath;
    }

    public String getRedirectUriTemplate() {
        return redirectUriTemplate;
    }

    public void setRedirectUriTemplate(String redirectUriTemplate) {
        this.redirectUriTemplate = redirectUriTemplate;
    }

    public String getTokenUriPath() {
        return tokenUriPath;
    }

    public void setTokenUriPath(String tokenUriPath) {
        this.tokenUriPath = tokenUriPath;
    }

    public String getUserInfoUriPath() {
        return userInfoUriPath;
    }

    public void setUserInfoUriPath(String userInfoUriPath) {
        this.userInfoUriPath = userInfoUriPath;
    }

    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }

    public void setUserNameAttributeName(String userNameAttributeName) {
        this.userNameAttributeName = userNameAttributeName;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
