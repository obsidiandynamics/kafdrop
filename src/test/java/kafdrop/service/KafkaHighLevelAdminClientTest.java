package kafdrop.service;

import kafdrop.config.KafkaConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.ListGroupsOptions;
import org.apache.kafka.clients.admin.ListGroupsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
