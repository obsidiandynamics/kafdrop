package com.homeadvisor.kafdrop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BrokerNotFoundException extends RuntimeException
{
   public BrokerNotFoundException()
   {
   }

   public BrokerNotFoundException(String message)
   {
      super(message);
   }
}
