package kafdrop.util;

public class Deserializers {

    private final MessageDeserializer keyDeserializer;
    private final MessageDeserializer valueDeserializer;

    public Deserializers(MessageDeserializer keyDeserializer, MessageDeserializer valueDeserializer) {
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
    }

    public MessageDeserializer getKeyDeserializer() {
        return keyDeserializer;
    }

    public MessageDeserializer getValueDeserializer() {
        return valueDeserializer;
    }

}
