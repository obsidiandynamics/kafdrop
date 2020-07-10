package kafdrop;

/**
 * Use this class for local development.
 * It will run local kafka in docker with test containers.
 */
public class LocalRunner {
    public static void main(String[] args) {
        Kafdrop.createApplicationBuilder()
                .initializers(new AbstractIntegrationTest.Initializer())
                .run(args);
    }
}
