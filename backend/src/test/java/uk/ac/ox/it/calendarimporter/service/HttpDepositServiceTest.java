
package uk.ac.ox.it.calendarimporter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpDepositServiceTest {
    
    private HttpDepositService depositService;
    
    @BeforeEach
    public void setUp() {
        depositService = new HttpDepositService();
    }

    @Test
    public void testCanHandleGood() {
        assertTrue(depositService.canHandle("http://host.test/"));
        assertTrue(depositService.canHandle("https://host.test/"));
    }
    
    @Test
    public void testCanHandleOld() {
        assertTrue(depositService.canHandle("  https://host.test/  "));
        assertTrue(depositService.canHandle("https://host.test/  "));
        assertTrue(depositService.canHandle("  https://host.test/"));
    }

    @Test
    public void testCanHandlePlaceholder() {
        assertFalse(depositService.canHandle("protocol://host.test/${placeholder}"));
        assertTrue(depositService.canHandle("http://host.test/${placeholder}"));
        assertTrue(depositService.canHandle("https://host.test/${placeholder}"));
    }

    @Test
    public void testCanHandleNotNotOwned() {
        assertFalse(depositService.canHandle("other://host"));
    }
}
