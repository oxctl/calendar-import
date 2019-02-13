package uk.ac.ox.it.calendarimporter.persistence.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByUsernameAndTenant_Name(String username, String tenantName);

    default Optional<User> findByOAuth2AuthenticationToken(OAuth2AuthenticationToken token) {
        return findByUsernameAndTenant_Name(token.getName(), token.getAuthorizedClientRegistrationId());

    }
}
