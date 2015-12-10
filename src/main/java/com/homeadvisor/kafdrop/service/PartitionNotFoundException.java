package com.homeadvisor.kafdrop.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PartitionNotFoundException extends RuntimeException
{
   public PartitionNotFoundException()
   {
   }

   public PartitionNotFoundException(String message)
   {
      super(message);
   }
}
