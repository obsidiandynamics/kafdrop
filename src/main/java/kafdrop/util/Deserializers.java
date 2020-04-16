package kafdrop.util;

public class Deserializers {

    private MessageDeserializer keyDeserializer;
    private MessageDeserializer valueDeserializer;

    public Deserializers(MessageDeserializer keyDeserializer, MessageDeserializer valueDeserializer) {
        this.setKeyDeserializer(keyDeserializer);
        this.setValueDeserializer(valueDeserializer);
    }

    public MessageDeserializer getKeyDeserializer() {
        return keyDeserializer;
    }

    public void setKeyDeserializer(MessageDeserializer keyDeserializer) {
        this.keyDeserializer = keyDeserializer;
    }

    public MessageDeserializer getValueDeserializer() {
        return valueDeserializer;
    }

    public void setValueDeserializer(MessageDeserializer valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
    }
}
