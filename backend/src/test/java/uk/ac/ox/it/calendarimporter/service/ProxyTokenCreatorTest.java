package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProxyTokenCreatorTest {

    private ProxyTokenCreator factory;

    @BeforeEach
    public void setUp() {
        factory = new ProxyTokenCreator();
    }

    @Test
    public void testExtractSimple() {
        String url = "http://example.com/login/oauth/token";
        String noLocal = factory.removeLocalPart(url);
        assertEquals("http://example.com", noLocal);
    }
}
