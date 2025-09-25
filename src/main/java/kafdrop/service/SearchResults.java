package kafdrop.service;

import jakarta.validation.constraints.NotNull;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Date;
import java.util.List;

public record SearchResults(@NotNull List<ConsumerRecord<String, String>> results,
                            @NotNull kafdrop.service.SearchResults.CompletionReason completionReason,
                            @NotNull Date finalMessageTimestamp,
                            long messagesScannedCount) {

  public enum CompletionReason {
    NO_MORE_MESSAGES_IN_TOPIC,
    FOUND_REQUESTED_NUMBER_OF_RESULTS,
    EXCEEDED_MAX_SCAN_COUNT
  }
}
