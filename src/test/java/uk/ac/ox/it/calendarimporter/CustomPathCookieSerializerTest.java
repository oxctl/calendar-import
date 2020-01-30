package uk.ac.ox.it.calendarimporter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer.CookieValue;

import javax.servlet.http.Cookie;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class CustomPathCookieSerializerTest {

    private CustomPathCookieSerializer cookieSerializer;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private CookieValue cookieValue;

    @Before
    public void setUp() {
        this.cookieSerializer = new CustomPathCookieSerializer();
        cookieSerializer.setUseBase64Encoding(false);
        request = new MockHttpServletRequest();
        request.setServerName("domain.test");
        response = new MockHttpServletResponse();
        cookieValue = new CookieValue(request, response, "1234");
    }

    @Test
    public void testWriteNormalCookie() {
        cookieSerializer.writeCookieValue(cookieValue);
        List<String> headers = response.getHeaders("Set-Cookie");
        assertFalse(headers.isEmpty());
        assertThat(headers, hasItem(containsString("SESSION=1234")));
        assertThat(headers, not(hasItem(containsString(cookieSerializer.getLegacySuffix()))));
    }

    @Test
    public void testWriteSameSiteNoneCookie() {
        cookieSerializer.setSameSite("None");
        cookieSerializer.writeCookieValue(cookieValue);
        List<String> headers = response.getHeaders("Set-Cookie");
        assertFalse(headers.isEmpty());
        assertThat(headers, hasItem(containsString("SESSION=1234")));
        assertThat(headers, hasItem(containsString(cookieSerializer.getLegacySuffix())));
    }

    @Test
    public void testWriteSameSiteNoneDisabledCookie() {
        cookieSerializer.setSameSite("None");
        cookieSerializer.setSameSiteWorkaround(false);
        cookieSerializer.writeCookieValue(cookieValue);
        List<String> headers = response.getHeaders("Set-Cookie");
        assertFalse(headers.isEmpty());
        assertThat(headers, hasItem(containsString("SESSION=1234")));
        assertThat(headers, not(hasItem(containsString(cookieSerializer.getLegacySuffix()))));
    }

    @Test
    public void testReadNormalCookie() {
        Cookie cookie = new Cookie("SESSION", "1234");
        request.setCookies(cookie);
        List<String> strings = cookieSerializer.readCookieValues(request);
        assertThat(strings, hasItem("1234"));
    }

    @Test
    public void testReadBothCookie() {
        cookieSerializer.setSameSite("None");
        request.setCookies(new Cookie("SESSION", "1234"), new Cookie("SESSION"+cookieSerializer.getLegacySuffix(), "1234"));
        List<String> strings = cookieSerializer.readCookieValues(request);
        assertThat(strings, hasItem("1234"));
        assertThat(strings, hasSize(2));
    }

    @Test
    public void testReadSameSiteCookie() {
        cookieSerializer.setSameSite("None");
        Cookie cookie = new Cookie("SESSION"+ cookieSerializer.getLegacySuffix(), "1234");
        request.setCookies(cookie);
        List<String> strings = cookieSerializer.readCookieValues(request);
        assertThat(strings, hasItem("1234"));
        assertThat(strings, hasSize(1));
    }

}
