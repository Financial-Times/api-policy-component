package com.ft.up.apipolicy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.dropwizard.testing.ConfigOverride.config;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.io.File;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * ApiPolicyComponentUnhappyPathsTest
 *
 * @author sarah.wells
 */
public class ApiPolicyComponentUnhappyPathsTest extends AbstractApiComponentTest {

  private static final String EXAMPLE_PATH = "/example";

  private static final String EXAMPLE_JSON = "{ fieldA: \"A\" , fieldB : \"B\" }";
  private static final String ERROR_JSON = "{\"message\":\"Something went wrong\"}";
  private static final String SERVER_ERROR_JSON = "{\"message\":\"server error\"}";
  private static final String SOCKET_TIMEOUT_JSON =
      "{\"message\":\"java.net.SocketTimeoutException: Read timed out\"}";
  private static final String UNSUPPORTED_REQUEST_EXCEPTION_JSON =
      "{\"message\":\"Unsupported request: path [/example] with method [POST].\"}";

  @Rule public final WireMockClassRule wireMockForVarnish1 = WIRE_MOCK_1;

  @ClassRule
  public static final DropwizardAppRule<ApiPolicyConfiguration> policyComponent =
      new DropwizardAppRule<>(
          ApiPolicyApplication.class,
          resourceFilePath("config-junit.yml"),
          config("varnish.primaryNodes", primaryNodes));

  private final Client client = JerseyClientBuilder.newClient();

  @Test
  public void shouldNotAllowNonWhitelistedPostRequestsThrough() {
    URI uri = fromFacade(EXAMPLE_PATH).build();

    Response response = client.target(uri).request().post(Entity.json("{}"));

    try {
      verify(0, postRequestedFor(urlEqualTo(EXAMPLE_PATH)));

      assertThat(response.getStatus(), is(405));
      assertThat(response.readEntity(String.class), is(UNSUPPORTED_REQUEST_EXCEPTION_JSON));

    } finally {
      response.close();
    }
  }

  // Fails completely
  @Test
  public void shouldFailWhereTargetNodeReturns500() {
    wireMockForVarnish1.stubFor(
        WireMock.get(urlEqualTo(EXAMPLE_PATH))
            .willReturn(
                aResponse()
                    .withBody(ERROR_JSON)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(500)));

    URI uri = fromFacade(EXAMPLE_PATH).build();

    Response response = client.target(uri).request().get();

    try {
      wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

      assertThat(response.getStatus(), is(500)); // TODO should map downstream errors to a 503
      assertThat(response.readEntity(String.class), is(ERROR_JSON));

    } finally {
      response.close();
    }
  }

  @Test
  public void shouldFailWhereTargetNodeReturns503() {
    wireMockForVarnish1.stubFor(
        WireMock.get(urlEqualTo(EXAMPLE_PATH))
            .willReturn(
                aResponse()
                    .withBody(ERROR_JSON)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(503)));

    URI uri = fromFacade(EXAMPLE_PATH).build();

    Response response = client.target(uri).request().get();

    try {
      wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

      assertThat(response.getStatus(), is(503));
      assertThat(response.readEntity(String.class), is(ERROR_JSON));

    } finally {
      response.close();
    }
  }

  @Test
  public void shouldFailWhereTargetNodeReturns503WithoutABody() {
    wireMockForVarnish1.stubFor(
        WireMock.get(urlEqualTo(EXAMPLE_PATH)).willReturn(aResponse().withStatus(503)));

    URI uri = fromFacade(EXAMPLE_PATH).build();

    Response response = client.target(uri).request().get();

    try {
      wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

      assertThat(response.getStatus(), is(503));
      assertThat(response.readEntity(String.class), is(SERVER_ERROR_JSON));

    } finally {
      response.close();
    }
  }

  @Test
  public void shouldReturnErrorIfTimeoutOccurs() {
    int tooLong = policyComponent.getConfiguration().getVarnish().getReadTimeoutMillis() + 500;
    wireMockForVarnish1.stubFor(
        WireMock.get(urlEqualTo(EXAMPLE_PATH))
            .willReturn(
                aResponse()
                    .withBody(EXAMPLE_JSON)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                    .withStatus(200)
                    .withFixedDelay(tooLong)));

    URI uri = fromFacade(EXAMPLE_PATH).build();

    Response response = client.target(uri).request().get();

    try {
      wireMockForVarnish1.verify(getRequestedFor(urlEqualTo(EXAMPLE_PATH)));

      assertThat(response.getStatus(), is(504));
      assertThat(response.readEntity(String.class), is(SOCKET_TIMEOUT_JSON));

    } finally {
      response.close();
    }
  }

  private UriBuilder fromFacade(String path) {
    return UriBuilder.fromPath(path)
        .host("localhost")
        .port(policyComponent.getLocalPort())
        .scheme("http");
  }

  public static String resourceFilePath(String resourceClassPathLocation) {

    File file = null;

    try {

      file = new File(Resources.getResource(resourceClassPathLocation).toURI());
      return file.getAbsolutePath();

    } catch (Exception e) {
      if (file != null) {
        throw new RuntimeException(file.toString(), e);
      }
      throw new RuntimeException(e);
    }
  }
}
