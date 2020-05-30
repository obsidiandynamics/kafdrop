package kafdrop.service;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import kafdrop.model.MessageVO;

/**
 * MessageFilter that only accepts messages that 'contain' the filter's content.
 */
public class ContainsMessageFilter extends MessageFilter {
	private String key;
	private String message;
	private String headerName;
	private String headerValue;
	
	@Override
	public boolean isConstrained() {
		return (key != null) 
			|| (message != null) 
			|| (headerName != null && headerValue != null);
	}
	
	@Override
	public boolean accepts(MessageVO m) {
		return (message == null || StringUtils.contains(m.getMessage(), message))
			&& containsHeader(m.getHeaders())
			&& (key == null || StringUtils.contains(m.getKey(), key));
	}
	

	private boolean containsHeader(Map<String, String> messageHeaders) {
		if(headerName == null || headerValue == null) {
			return true;
		}
		
		return StringUtils.contains(messageHeaders.get(headerName), headerValue);
	}

	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		if(!StringUtils.isBlank(key)) 
			this.key = key;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String value) {
		if(!StringUtils.isBlank(value)) 
			this.message = value;
	}
	
	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		if(!StringUtils.isBlank(headerName)) 	
			this.headerName = headerName;
	}

	public String getHeaderValue() {
		return headerValue;
	}
	public void setHeaderValue(String headerValue) {
		if(!StringUtils.isBlank(headerValue)) 		
			this.headerValue = headerValue;
	}
	
}
