package kafdrop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KafdropIT extends AbstractIntegrationTest {
    @Test
    void contextTest(){
        assertTrue(Initializer.kafka.isRunning());
    }
}
