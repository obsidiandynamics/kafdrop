package com.homeadvisor.kafdrop.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public class BrokerVO
{
   private int id;
   private String host;
   private int port;
   private int jmxPort;
   private int version;
   private Date timestamp;

   public int getId()
   {
      return id;
   }

   public void setId(int id)
   {
      this.id = id;
   }

   public String getHost()
   {
      return host;
   }

   public void setHost(String host)
   {
      this.host = host;
   }

   public int getPort()
   {
      return port;
   }

   public void setPort(int port)
   {
      this.port = port;
   }

   public int getJmxPort()
   {
      return jmxPort;
   }

   @JsonProperty("jmx_port")
   public void setJmxPort(int jmxPort)
   {
      this.jmxPort = jmxPort;
   }

   public int getVersion()
   {
      return version;
   }

   public void setVersion(int version)
   {
      this.version = version;
   }

   public Date getTimestamp()
   {
      return timestamp;
   }

   public void setTimestamp(Date timestamp)
   {
      this.timestamp = timestamp;
   }
}
