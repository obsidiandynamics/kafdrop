package kafdrop.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.Date;
import java.util.List;

public final class SearchResults {

  private Date finalMessageTimestamp;

  private long messagesScannedCount;

  private CompletionReason completionReason;

  private List<ConsumerRecord<String, String>> results;

  public enum CompletionReason {
    NO_MORE_MESSAGES_IN_TOPIC,
    FOUND_REQUESTED_NUMBER_OF_RESULTS,
    EXCEEDED_MAX_SCAN_COUNT
  }

  public SearchResults(
    List<ConsumerRecord<String, String>> results,
    CompletionReason completionReason,
    Date finalMessageTimestamp,
    long messagesScannedCount) {

    this.finalMessageTimestamp = finalMessageTimestamp;
    this.messagesScannedCount = messagesScannedCount;
    this.completionReason = completionReason;
    this.results = results;
  }

  public List<ConsumerRecord<String, String>> getResults() {
    return results;
  }

  public CompletionReason getCompletionReason() {
    return completionReason;
  }

  public long getMessagesScannedCount() {
    return messagesScannedCount;
  }

  public Date getFinalMessageTimestamp() {
    return finalMessageTimestamp;
  }
}
