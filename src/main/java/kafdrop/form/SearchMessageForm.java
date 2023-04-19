package kafdrop.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kafdrop.util.MessageFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class SearchMessageForm {

  @NotBlank
  private String searchText;

  @NotNull
  @Min(1)
  @Max(1000)
  private Integer maximumCount;

  private MessageFormat format;

  private MessageFormat keyFormat;

  private String descFile;

  private String msgTypeName;

  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  private Date startTimestamp;

  public SearchMessageForm(String searchText, MessageFormat format) {
    this.searchText = searchText;
    this.format = format;
  }

  public Date getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(Date startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  public SearchMessageForm(String searchText) {
    this(searchText, MessageFormat.DEFAULT);
  }

  public SearchMessageForm() {
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

  public MessageFormat getKeyFormat() {
    return keyFormat;
  }

  public void setKeyFormat(MessageFormat keyFormat) {
    this.keyFormat = keyFormat;
  }

  public MessageFormat getFormat() {
    return format;
  }

  public void setFormat(MessageFormat format) {
    this.format = format;
  }

  public String getDescFile() {
    return descFile;
  }

  public void setDescFile(String descFile) {
    this.descFile = descFile;
  }

  public String getMsgTypeName() {
    return msgTypeName;
  }

  public void setMsgTypeName(String msgTypeName) {
    this.msgTypeName = msgTypeName;
  }
}
