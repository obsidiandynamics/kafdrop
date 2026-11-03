package kafdrop.service;

import kafdrop.config.KafkaConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.ListGroupsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class KafkaHighLevelAdminClientTest {
  private AdminClient adminClient;
  private KafkaHighLevelAdminClient client;

  @BeforeEach
  void setUp() {
    adminClient = mock(AdminClient.class);
    client = new KafkaHighLevelAdminClient(new KafkaConfiguration());
    ReflectionTestUtils.setField(client, "adminClient", adminClient);
  }

  @Test
  void listsGroupsByType() {
    final var byType = result(List.of(group("by-type")), List.of());
    when(adminClient.listGroups(any(ListGroupsOptions.class))).thenReturn(byType);

    assertEquals(Set.of("by-type"), client.listConsumerGroups());
    verify(adminClient, never()).listGroups();
  }

  @Test
  void listsAllGroupsWhenBrokerCannotListByType() {
    final var byType = result(List.of(), List.of(new UnsupportedVersionException("ListGroups v4")));
    final var all = result(List.of(group("a"), group("b")), List.of());
    when(adminClient.listGroups(any(ListGroupsOptions.class))).thenReturn(byType);
    when(adminClient.listGroups()).thenReturn(all);

    assertEquals(Set.of("a", "b"), client.listConsumerGroups());
  }

  @Test
  void keepsValidGroupsWhenOtherErrorsOccur() {
    final var byType = result(List.of(group("ok")), List.of(new RuntimeException("broker down")));
    when(adminClient.listGroups(any(ListGroupsOptions.class))).thenReturn(byType);

    assertEquals(Set.of("ok"), client.listConsumerGroups());
    verify(adminClient, never()).listGroups();
  }

  @Test
  void keepsGroupsFromBrokersThatListByTypeWhenFallingBack() {
    final var byType = result(List.of(group("on-new-broker")),
      List.of(new UnsupportedVersionException("ListGroups v4")));
    final var all = result(List.of(group("on-old-broker")), List.of(new RuntimeException("broker down")));
    when(adminClient.listGroups(any(ListGroupsOptions.class))).thenReturn(byType);
    when(adminClient.listGroups()).thenReturn(all);

    assertEquals(Set.of("on-new-broker", "on-old-broker"), client.listConsumerGroups());
  }

  @Test
  void logsOtherErrorsFromTheFirstAttemptBeforeFallingBack() {
    final var byType = result(List.of(),
      List.of(new UnsupportedVersionException("ListGroups v4"), new RuntimeException("broker timeout")));
    final var all = result(List.of(group("a")), List.of());
    when(adminClient.listGroups(any(ListGroupsOptions.class))).thenReturn(byType);
    when(adminClient.listGroups()).thenReturn(all);

    final var warnings = captureWarnings(() -> assertEquals(Set.of("a"), client.listConsumerGroups()));

    assertTrue(warnings.stream().anyMatch(e -> "broker timeout".equals(e.getThrown().getMessage())),
      "Expected the first attempt's unrelated error to be logged");
    assertTrue(warnings.stream().noneMatch(e -> e.getThrown() instanceof UnsupportedVersionException),
      "Expected the UnsupportedVersionException that triggers the fallback not to be logged as a warning");
  }

  private static List<LogEvent> captureWarnings(Runnable action) {
    final var logger = (Logger) LogManager.getLogger(KafkaHighLevelAdminClient.class);
    final var events = new ArrayList<LogEvent>();
    final var appender = new AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
      @Override
      public void append(LogEvent event) {
        if (event.getLevel() == Level.WARN) {
          events.add(event.toImmutable());
        }
      }
    };
    appender.start();
    logger.addAppender(appender);
    try {
      action.run();
    } finally {
      logger.removeAppender(appender);
      appender.stop();
    }
    return events;
  }

  private static GroupListing group(String groupId) {
    return new GroupListing(groupId, Optional.empty(), "consumer", Optional.empty());
  }

  private static ListGroupsResult result(List<GroupListing> valid, List<Throwable> errors) {
    final var result = mock(ListGroupsResult.class);
    when(result.valid()).thenReturn(KafkaFuture.<Collection<GroupListing>>completedFuture(valid));
    when(result.errors()).thenReturn(KafkaFuture.<Collection<Throwable>>completedFuture(errors));
    return result;
  }
}
