package uk.ac.ox.it.calendarimporter.service;

import edu.ksu.lti.launch.model.LtiLaunchData;
import edu.ksu.lti.launch.oauth.LtiPrincipal;
import edu.ksu.lti.launch.service.SimpleToolConsumer;
import edu.ksu.lti.launch.service.ToolConsumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.ac.ox.it.calendarimporter.controller.LTILaunchException;
import uk.ac.ox.it.calendarimporter.persistence.model.Tenant;
import uk.ac.ox.it.calendarimporter.persistence.model.User;
import uk.ac.ox.it.calendarimporter.persistence.repo.TenantRepository;
import uk.ac.ox.it.calendarimporter.persistence.repo.UserRepository;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class CalendarLtiLoginServiceTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private LtiPrincipal principal;
    private LtiLaunchData data;
    private ToolConsumer toolConsumer;

    private CalendarLtiLoginService service;

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserRepository userRepository;

    @Before
    public void setUp() {
        service = new CalendarLtiLoginService();
        service.setTenantRepository(tenantRepository);
        service.setUserRepository(userRepository);
        toolConsumer = new SimpleToolConsumer("test", "Test", "http://test");
    }


    @Test(expected = LTILaunchException.class)
    public void testLoginNoTenant() {
        principal = new LtiPrincipal(toolConsumer, "user");
        when(tenantRepository.findByName(anyString())).thenReturn(Optional.empty());
        service.onLogin(principal, data);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test(expected = LTILaunchException.class)
    public void testLoginNoName() {
        principal = new LtiPrincipal(toolConsumer, "user");
        data = new LtiLaunchData();
        Tenant tenant = new Tenant();
        when(tenantRepository.findByName(anyString())).thenReturn(Optional.of(tenant));
        when(userRepository.findByUsernameAndTenant_Name("user", "test")).thenReturn(Optional.empty());
        service.onLogin(principal, data);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLoginNewUser() {
        principal = new LtiPrincipal(toolConsumer, "user");
        data = new LtiLaunchData();
        Tenant tenant = new Tenant();
        when(tenantRepository.findByName(anyString())).thenReturn(Optional.of(tenant));
        when(userRepository.findByUsernameAndTenant_Name("user", "test")).thenReturn(Optional.empty());
        data.setLisPersonNameFull("Test User");
        service.onLogin(principal, data);
        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testLoginExistingUser() {
        principal = new LtiPrincipal(toolConsumer, "user");
        data = new LtiLaunchData();
        Tenant tenant = new Tenant();
        when(tenantRepository.findByName(anyString())).thenReturn(Optional.of(tenant));
        User user = new User(tenant, "test");
        when(userRepository.findByUsernameAndTenant_Name("user", "test")).thenReturn(Optional.of(user));
        data.setLisPersonNameFull("Test User");
        data.setLisPersonContactEmailPrimary("test@test");
        data.setLaunchPresentationLocale("en");
        service.onLogin(principal, data);
        verify(userRepository).save(any(User.class));
        assertEquals("Test User", user.getName());
        assertEquals("test@test", user.getEmail());
        assertEquals("en", user.getLocale());
    }

}
