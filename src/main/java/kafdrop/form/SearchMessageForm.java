package kafdrop.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import kafdrop.util.MessageFormat;

import javax.validation.constraints.NotBlank;

public class SearchMessageForm {

    @NotBlank
    private String searchText;

    private MessageFormat format;

    private MessageFormat keyFormat;

    private String descFile;

    private String msgTypeName;

    public SearchMessageForm(String searchText, MessageFormat format) {
        this.searchText = searchText;
        this.format = format;
    }

    public SearchMessageForm(String searchText) {
        this(searchText, MessageFormat.DEFAULT);
    }

    public SearchMessageForm() {}

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

