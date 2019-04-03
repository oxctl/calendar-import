package uk.ac.ox.it.calendarimporter.service;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class OauthTokenFactoryTest {

  private OauthTokenFactory factory;

  @Before
  public void setUp() {
    factory = new OauthTokenFactory();
  }

  @Test
  public void testExtractSimple() {
    String url = "http://example.com/login/oauth/token";
    String noLocal = factory.removeLocalPart(url);
    assertEquals("http://example.com", noLocal);
  }
}
