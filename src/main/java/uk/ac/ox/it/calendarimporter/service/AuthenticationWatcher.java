package uk.ac.ox.it.calendarimporter.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * This could allow us to update the database table when a user authenticates.
 */
@Component()
public class AuthenticationWatcher implements ApplicationListener<AuthenticationSuccessEvent>{

    @Autowired
    private UserRepository userRepository;

    @PostConstruct
    public void init() {
    }

    @Override
    @Transactional
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication instanceof  OAuth2LoginAuthenticationToken) {
            OAuth2LoginAuthenticationToken oauthAuthentication = (OAuth2LoginAuthenticationToken) authentication;
            // This is our internal name for the remote server
            String tenantName = oauthAuthentication.getClientRegistration().getRegistrationId();
            String username = oauthAuthentication.getPrincipal().getName();
            String token = oauthAuthentication.getAccessToken().getTokenValue();

            User user = userRepository.findByTenantNameAndUsername(tenantName, username)
                    .orElse(new User(tenantName, username));
            user.setToken(token);
            new User();
            userRepository.save(user);
        }
    }

}
