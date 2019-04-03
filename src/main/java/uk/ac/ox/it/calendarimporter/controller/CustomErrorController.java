package uk.ac.ox.it.calendarimporter.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

  @Value("${calendar.common.css}")
  private String defaultCommonCss;

  @Value("${calendar.brand.json}")
  private String defaultBrandJson;

  @Value("${spring.application.name}")
  private String applicationName;

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

  @RequestMapping("/error")
  public String handleError() {
    // do something like logging
    return "error";
  }

  @Override
  public String getErrorPath() {
    return "/error";
  }
}
