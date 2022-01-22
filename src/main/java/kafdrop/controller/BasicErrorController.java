package kafdrop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger LOG = LoggerFactory.getLogger(BasicErrorController.class);

  public BasicErrorController(ErrorAttributes errorAttributes) {
    super(errorAttributes);
  }

  @RequestMapping("/error")
  public ModelAndView handleError(HttpServletRequest request) {
    final var errorAttributeOptions = ErrorAttributeOptions.of(
      ErrorAttributeOptions.Include.STACK_TRACE,
      ErrorAttributeOptions.Include.MESSAGE);

    final var error = getErrorAttributes(request, errorAttributeOptions);
    LOG.info("errorAtts: {}", error);
    
    error.putIfAbsent("message", "");

    final var model = Map.of("error", error);
    return new ModelAndView("error", model);
  }
}