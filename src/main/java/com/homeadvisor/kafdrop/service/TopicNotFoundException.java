package com.homeadvisor.kafdrop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TopicNotFoundException extends RuntimeException
{
   public TopicNotFoundException()
   {
   }

   public TopicNotFoundException(String message)
   {
      super(message);
   }
}
