package kafdrop.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kafdrop.util.MessageFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class SearchMessageFormForJson {
  @NotBlank
  @Schema(example = "Some search text")
  private String searchText;
  @NotNull
  @Min(1)
  @Max(1000)
  @Schema(example = "1000")
  private Integer maximumCount;
  @Schema(example = "-1")
  private Integer partition;
  @Schema(example = "DEFAULT")
  private MessageFormat format;
  @Schema(example = "DEFAULT")
  private MessageFormat keyFormat;
  @Schema(type = "string", example = "1970-01-01T00:00:00.000Z")
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
  private Date startTimestamp;
  @Schema(description = "Keys to filter messages", example = "[\"key1\", \"key2\"]")
  private String[] keys;

  protected SearchMessageFormForJson() {
    // Default constructor
  }

  protected SearchMessageFormForJson(String searchText, MessageFormat format) {
    this.searchText = searchText;
    this.format = format;
  }

  public SearchMessageFormForJson(String searchText, Integer maximumCount, Integer partition, MessageFormat format,
                                  MessageFormat keyFormat, Date startTimestamp) {
    this.searchText = (searchText == null) ? "" : searchText;
    this.maximumCount = (maximumCount == null) ? 1000 : maximumCount;
    this.partition = (partition == null) ? -1 : partition;
    this.format = (format == null) ? MessageFormat.DEFAULT : format;
    this.keyFormat = (keyFormat == null) ? MessageFormat.DEFAULT : keyFormat;
    this.startTimestamp = (startTimestamp == null) ? new Date(0) : startTimestamp;
  }

  @JsonIgnore
  public boolean isEmpty() {
    return searchText == null || searchText.isEmpty();
  }

  public String getSearchText() {
    return searchText;
  }

  public void setSearchText(String searchText) {
    this.searchText = searchText;
  }

  public Integer getMaximumCount() {
    return maximumCount;
  }

  public void setMaximumCount(Integer maximumCount) {
    this.maximumCount = maximumCount;
  }

  public Integer getPartition() {
    return partition;
  }

  public void setPartition(Integer partition) {
    this.partition = partition;
  }

  public MessageFormat getFormat() {
    return format;
  }

  public void setFormat(MessageFormat format) {
    this.format = format;
  }

  public MessageFormat getKeyFormat() {
    return keyFormat;
  }

  public void setKeyFormat(MessageFormat keyFormat) {
    this.keyFormat = keyFormat;
  }

  public Date getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(Date startTimestamp) {
    this.startTimestamp = startTimestamp;
  }
}
