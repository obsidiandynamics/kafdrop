package kafdrop.service;

import kafdrop.model.MessageVO;

public abstract class MessageFilter {
	/** @return boolean indicating whether the given message passes the given filter */ 
	public abstract boolean accepts(MessageVO m);
	
	/** @return boolean indicating whether the filter is constrained and will not accept any message */
	public abstract boolean isConstrained();
	
	/** Default filter accepting any message. */
	public static MessageFilter DEFAULT = new MessageFilter() {
		
		@Override
		public boolean isConstrained() {
			return false;
		}
		
		@Override
		public boolean accepts(MessageVO m) {
			return true;
		}
	};
}
