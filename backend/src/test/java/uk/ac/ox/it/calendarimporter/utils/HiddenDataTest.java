package uk.ac.ox.it.calendarimporter.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HiddenDataTest {
    
    @Test
    public void testText() {
        String toHide = "test";
        String description = "Just some text.";
        String withHidden = HiddenData.insertHidden(description, HiddenData.toHidden(toHide));
        String extracted = HiddenData.fromHidden(HiddenData.extractHidden(withHidden));
        assertEquals(toHide, extracted);
        assertEquals(description, HiddenData.removeHidden(withHidden));
    }

    @Test
    public void testQuotes() {
        String toHide = "test\"'";
        String description = "Just some text.";
        String withHidden = HiddenData.insertHidden(description, HiddenData.toHidden(toHide));
        String extracted = HiddenData.fromHidden(HiddenData.extractHidden(withHidden));
        assertEquals(toHide, extracted);
        assertEquals(description, HiddenData.removeHidden(withHidden));
    }

    @Test
    public void testHtml() {
        String toHide = "test";
        String description = "<div>Just some text.</div>";
        String withHidden = HiddenData.insertHidden(description, HiddenData.toHidden(toHide));
        String extracted = HiddenData.fromHidden(HiddenData.extractHidden(withHidden));
        assertEquals(toHide, extracted);
        assertEquals(description, HiddenData.removeHidden(withHidden));
    }

    @Test
    public void testEmpty() {
        String toHide = "";
        String description = "<div>Just some text.</div>";
        String withHidden = HiddenData.insertHidden(description, HiddenData.toHidden(toHide));
        String extracted = HiddenData.fromHidden(HiddenData.extractHidden(withHidden));
        assertEquals(toHide, extracted);
        assertEquals(description, HiddenData.removeHidden(withHidden));
    }

    @Test
    public void testHideHtml() {
        String toHide = "<div>Something</div>";
        String description = "<div>Just some text.</div>";
        String withHidden = HiddenData.insertHidden(description, HiddenData.toHidden(toHide));
        String extracted = HiddenData.fromHidden(HiddenData.extractHidden(withHidden));
        assertEquals(toHide, extracted);
        assertEquals(description, HiddenData.removeHidden(withHidden));
    }

}