package kafdrop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.TRACE;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class KafdropTest extends AbstractIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private Kafdrop kafdrop;

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void contextLoads() throws Exception {
    assertThat(kafdrop).isNotNull();
  }

  @Test
  public void getReturnsExpectedGutHubStarText() throws Exception {
    ResponseEntity<String> responseEntity = restTemplate
            .getForEntity("http://localhost:" + port + "/", String.class);
    assertEquals(OK, responseEntity.getStatusCode());
    assertThat(responseEntity.getBody().contains("Star Kafdrop on GitHub"));
  }

  @Test
  public void traceMethodExpectedDisallowedReturnCode() throws Exception {
    ResponseEntity<String> response = restTemplate
            .exchange("http://localhost:" + port + "/", TRACE, null, String.class);
      assertEquals(METHOD_NOT_ALLOWED, response.getStatusCode());
  }
}
