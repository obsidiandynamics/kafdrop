package com.homeadvisor.kafdrop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConsumerNotFoundException extends RuntimeException
{
   public ConsumerNotFoundException(String message)
   {
      super(message);
   }

   public ConsumerNotFoundException()
   {
   }
}
