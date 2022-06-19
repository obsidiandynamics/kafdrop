package kafdrop;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.KafkaTemplate;

import com.google.protobuf.Any;

import kafdrop.kafka.KafkaTestConfiguration;
import kafdrop.protos.Person;

@TestComponent
public class SendKafkaProtoPayload implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private KafkaTemplate<String, byte[]> kafkaTemplate;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (!AbstractIntegrationTest.Initializer.kafka.isRunning()) {
			throw new IllegalStateException("Kafka container not started");
		}

		Person person = Person.newBuilder()
				.setName("Max Mustermann")
				.setId(1)
				.setEmail("max.mustermann@example.com")
				.setContact(Person.Contact.MOBILE)
				.addAllData(List.of("This", "is", "a", "test"))
				.build();
		byte[] payload = Any.pack(person).toByteArray();
		kafkaTemplate.send(KafkaTestConfiguration.TEST_TOPIC_NAME, UUID.randomUUID().toString(), payload);
	}
}
