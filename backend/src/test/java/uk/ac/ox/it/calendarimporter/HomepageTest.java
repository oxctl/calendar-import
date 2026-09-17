package uk.ac.ox.it.calendarimporter;

import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.htmlunit.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application.properties", "classpath:application-test.properties"})
public class HomepageTest {

    private WebClient webClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        webClient = new WebClient();
    }

    @Test
    void frontPageWorks() throws IOException {
        // Check that everything starts up and we respond to a request for the homepage (doesn't need authentication)
        HtmlPage page = webClient.getPage("http://localhost:" + port + "/");
        assertEquals("Calendar Import", page.getTitleText());
    }

}