package uk.ac.ox.it.calendarimporter.persistence.repo;


import junit.framework.AssertionFailedError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ox.it.calendarimporter.persistence.model.User;

import javax.persistence.PersistenceException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository repository;

    @Test
    public void testFindByTenantNameAndUsername() throws Exception {
        entityManager.persist(new User("tenant", "username"));
        entityManager.flush();
        User user = this.repository.findByTenantNameAndUsername("tenant", "username").orElseThrow(AssertionFailedError::new);
        assertEquals("tenant", user.getTenantName());
        assertEquals("username", user.getUsername());
        assertNotNull(user.getId());

    }

    @Test
    public void testFindByOAuth2AuthenticationToken() throws Exception {
        entityManager.persist(new User("tenant", "username"));
        entityManager.flush();
        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        when(token.getAuthorizedClientRegistrationId()).thenReturn("tenant");
        when(token.getName()).thenReturn("username");
        User user = repository.findByOAuth2AuthenticationToken(token).orElseThrow(AssertionFailedError::new);
        assertEquals("tenant", user.getTenantName());
        assertEquals("username", user.getUsername());
        assertNotNull(user.getId());
    }

    @Test
    public void testNotFound() throws Exception {
        Optional<User> missing = this.repository.findByTenantNameAndUsername("badTenant", "noUser");
        assertFalse(missing.isPresent());
    }

    @Test(expected = PersistenceException.class)
    public void testNoDuplicates() throws Exception {
        // Check index prevents duplicates
        entityManager.persist(new User("tenant", "username"));
        entityManager.persist(new User("tenant", "username"));
        entityManager.flush();
    }


    @Test
    public void testUpdateUser() throws Exception {
        entityManager.persist(new User("tenant", "username"));
        entityManager.flush();
        {
            User user = this.repository.findByTenantNameAndUsername("tenant", "username").orElseThrow(AssertionFailedError::new);
            assertNotNull(user.getId());
            user.setEmail("email@example.com");
            user.setName("Display Name");
            this.repository.save(user);
        }
        {
            User user = this.repository.findByTenantNameAndUsername("tenant", "username").orElseThrow(AssertionFailedError::new);
            assertEquals("email@example.com", user.getEmail());
            assertEquals("Display Name", user.getName());
        }
    }
}
