package kafdrop.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kafdrop.util.MessageFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class SearchMessageForm extends SearchMessageFormForJson {

  private String descFile;

  private String msgTypeName;

  public SearchMessageForm(String searchText, MessageFormat format) {
    super (searchText, format);
  }

  public SearchMessageForm(String searchText) {
    this(searchText, MessageFormat.DEFAULT);
  }

  public SearchMessageForm() {
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
