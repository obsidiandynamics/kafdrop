package com.homeadvisor.kafdrop.controller;

import com.homeadvisor.kafdrop.service.NotInitializedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class KafkaExceptionHandler
{
   @ExceptionHandler(NotInitializedException.class)
   public String notInitialized()
   {
      return "not-initialized";
   }

}
