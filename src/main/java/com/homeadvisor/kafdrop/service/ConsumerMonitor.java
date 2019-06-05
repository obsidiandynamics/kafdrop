package com.homeadvisor.kafdrop.service;

import com.homeadvisor.kafdrop.model.*;

import java.util.*;

public interface ConsumerMonitor {
  List<ConsumerVO> getConsumers(TopicVO topic);
}
