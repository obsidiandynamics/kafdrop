package kafdrop.controller;

import org.springframework.boot.autoconfigure.web.servlet.error.*;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.*;
import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.*;

import javax.servlet.http.*;
import java.util.*;

@Controller
public final class BasicErrorController extends AbstractErrorController {
  public BasicErrorController(ErrorAttributes errorAttributes) {
    super(errorAttributes);
  }

  @RequestMapping("/error")
  public ModelAndView handleError(HttpServletRequest request) {
    final var error = getErrorAttributes(request, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));
    System.out.println("errorAtts: " + error);
    final var model = Map.of("error", error);
    return new ModelAndView("error", model);
  }

  public String getErrorPath() {
    // not used anymore (see javadoc, remove method when spring boot removes it from ErrorController interface)
    return null;
  }
}