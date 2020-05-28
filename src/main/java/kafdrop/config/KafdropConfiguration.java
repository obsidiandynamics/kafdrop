package kafdrop.config;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class KafdropConfiguration {
	  @Component
	  @ConfigurationProperties(prefix = "kafdrop")
	  public static final class KafdropProperties {
		/** 
		 * Reduces the topic info that is shown by default in the app, to improve app performance on clusters 
		 * with many topics & consumers. 
		 * 
		 * E.g., doesn't show consumer info or detailed topic (partition) info unless absolutely required.
		 */
	    private Boolean reducedTopicInfo;
	    
	    /**
	     * Hides the 'Delete Topic' button in the UI.
	     */
	    private Boolean hideDeleteTopic;

		@PostConstruct
	    public void init() {
	      // Set defaults if not configured.
	      if (reducedTopicInfo == null) {
	    	  reducedTopicInfo = false;
	      }
	      if (hideDeleteTopic == null) {
	    	  hideDeleteTopic = false;
	      }
	    }

	    public Boolean getReducedTopicInfo() {
			return reducedTopicInfo;
		}

		public void setReducedTopicInfo(Boolean reducedTopicInfo) {
			this.reducedTopicInfo = reducedTopicInfo;
		}

		public Boolean getHideDeleteTopic() {
			return hideDeleteTopic;
		}

		public void setHideDeleteTopic(Boolean hideDeleteTopic) {
			this.hideDeleteTopic = hideDeleteTopic;
		}
	  }
}
