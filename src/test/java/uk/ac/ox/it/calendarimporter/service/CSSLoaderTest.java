package uk.ac.ox.it.calendarimporter.service;

import org.junit.Test;

public class CSSLoaderTest {

    @Test
    public void testLoadCSS() {
        CSSLoader loader = new CSSLoader();
        loader.loadJSON("https://du11hjcvx0uqb.cloudfront.net/dist/brandable_css/70f360f85859badb1080d11285cd5e50/variables-750d72b9d3e5d522f965bf904110c132.json");
    }

}
