package kafdrop.service;

import kafdrop.model.*;

import java.util.*;

public interface ConsumerMonitor {
  List<ConsumerVO> getConsumers(TopicVO topic);
}
