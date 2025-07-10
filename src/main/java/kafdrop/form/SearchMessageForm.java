package kafdrop.form;

import kafdrop.util.MessageFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class SearchMessageForm extends SearchMessageFormForJson {

  private String descFile;
  private String msgTypeName;
  private final SimpleDateFormat UI_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  public SearchMessageForm(String searchText, MessageFormat format) {
    super(searchText, format);
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

  public String getStartTimestampUi() {
    if (super.getStartTimestamp() == null) {
      return "";
    }
    return UI_DATE_FORMAT.format(super.getStartTimestamp());
  }

  public void setStartTimestampUi(String value) throws ParseException {
    if (value == null || value.trim().isEmpty()) {
      super.setStartTimestamp(null);
    } else {
      super.setStartTimestamp(UI_DATE_FORMAT.parse(value));
    }
  }

}
