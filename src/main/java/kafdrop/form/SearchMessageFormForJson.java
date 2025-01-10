package kafdrop.form;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kafdrop.util.MessageFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class SearchMessageFormForJson {
  @Schema(example = "Some search text")
  private String searchText;
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
  @Schema(type = "string", example = "1970-01-01 03:00:00.000")
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  private Date startTimestamp;

  public SearchMessageFormForJson(String searchText, Integer maximumCount, Integer partition, MessageFormat format,
                                  MessageFormat keyFormat, Date startTimestamp) {
    this.searchText = (searchText == null) ? "" : searchText;
    this.maximumCount = (maximumCount == null) ? 1000 : maximumCount;
    this.partition = (partition == null) ? -1 : partition;
    this.format = (format == null) ? MessageFormat.DEFAULT : format;
    this.keyFormat = (keyFormat == null) ? MessageFormat.DEFAULT : keyFormat;
    this.startTimestamp = (startTimestamp == null) ? new Date(0) : startTimestamp;
  }
}
