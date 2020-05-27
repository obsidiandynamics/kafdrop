package kafdrop.service;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import kafdrop.model.MessageVO;

public class MessageFilter {
	private static enum Type {CONTAINS};
	private Type type; 
	
	private String key;
	private String message;
	private String headerName;
	private String headerValue;
	
	
	public MessageFilter() {
		type = Type.CONTAINS;
	}
	
	/** @return boolean indicating whether the given message passes the given filter */ 
	public boolean accepts(MessageVO m) {
		switch(type) {
			case CONTAINS:
				return (message == null || StringUtils.contains(m.getMessage(), message))
					&& containsHeader(m.getHeaders())
					&& (key == null || StringUtils.contains(m.getKey(), key));
			default:
					throw new UnsupportedOperationException(String.format("unsupported filter type: %s", type));
		}
	}
	

	private boolean containsHeader(Map<String, String> messageHeaders) {
		if(headerName == null || headerValue == null) {
			return true;
		}
		
		return StringUtils.contains(messageHeaders.get(headerName), headerValue);
	}

	public boolean initialized() {
		return (key != null) 
			|| (message != null) 
			|| (headerName != null && headerValue != null);
	}
	
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
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

	public static boolean accepts(MessageFilter filter, MessageVO messageVo) {
		if(filter == null) {
			return true;
		}
		
		return filter.accepts(messageVo);
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
