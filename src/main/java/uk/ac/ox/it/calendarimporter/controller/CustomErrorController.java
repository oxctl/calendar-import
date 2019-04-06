package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Just like the standard one, but adds our properties to that we get our CSS. */
public class CustomErrorController extends BasicErrorController {

  @Value("${calendar.common.css}")
  private String defaultCommonCss;

  @Value("${calendar.brand.json}")
  private String defaultBrandJson;

  @Value("${spring.application.name}")
  private String applicationName;

  public CustomErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties, List<ErrorViewResolver> errorViewResolvers) {
    super(errorAttributes, errorProperties, errorViewResolvers);
  }

  @ModelAttribute("canvasCommonCss")
  public String canvasCommonCss() {
    return defaultCommonCss;
  }

  @ModelAttribute("canvasBrandCss")
  public String canvasBrandCss() {
    String canvasCss = "";
    if (defaultBrandJson.endsWith(".json")) {
      canvasCss =
          defaultBrandJson.substring(0, defaultBrandJson.length() - ".json".length()) + ".css";
    }
    return canvasCss;
  }

  @ModelAttribute("applicationName")
  public String applicationName() {
    return applicationName;
  }

}
