package com.ft.up.apipolicy.health;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ft.platform.dropwizard.AdvancedResult;
import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * ReaderNodesHealthCheckTest
 *
 * @author Simon.Gibbs
 */
public class ReaderNodesHealthCheckTest {

  // used in stacks with vulcan-based routing
  private static final String VULCAN_HC_PATH = "/v2/status";

  // used in stacks with varnish-based routing
  private static final String VARNISH_HC_PATH = "/status";

  @Test
  public void shouldReturnErrorStateIfClientThrowsException() throws Exception {
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(builder.header(any(String.class), any(String.class))).thenReturn(builder);
    WebTarget target = mock(WebTarget.class);
    when(target.request()).thenReturn(builder);

    Client client = mock(Client.class);
    when(client.target(any(URI.class))).thenReturn(target);

    when(builder.get()).thenThrow(new RuntimeException("Synthetic client exception"));

    EndpointConfiguration config = mock(EndpointConfiguration.class);

    ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test", config, client, false);

    assertThat(check.checkAdvanced().status(), is(AdvancedResult.Status.ERROR));
  }

  @Test
  public void shouldReturnErrorStateIfServerGives500() throws Exception {

    EndpointConfiguration config = mock(EndpointConfiguration.class);

    Client client = primeClientToGive(500);

    ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test", config, client, false);

    assertThat(check.checkAdvanced().status(), is(AdvancedResult.Status.ERROR));
  }

  @Test
  public void shouldReturnOKStateIfServerGives200() throws Exception {

    EndpointConfiguration config = mock(EndpointConfiguration.class);

    Client client = primeClientToGive(200);

    ReaderNodesHealthCheck check = new ReaderNodesHealthCheck("test", config, client, false);

    assertThat(check.checkAdvanced().status(), is(AdvancedResult.Status.OK));
  }

  @Test
  public void shouldCheckVarnishIfCheckVulcanHealthFalse() throws Exception {
    testHealthcheckURI(false, VARNISH_HC_PATH);
  }

  @Test
  public void shouldCheckVulcanIfCheckVulcanHealthTrue() throws Exception {
    testHealthcheckURI(true, VULCAN_HC_PATH);
  }

  private void testHealthcheckURI(boolean checkVulcanHealth, String expectedPath) throws Exception {
    EndpointConfiguration config = mock(EndpointConfiguration.class);
    Client client = mock(Client.class);

    ReaderNodesHealthCheck check =
        new ReaderNodesHealthCheck("test", config, client, checkVulcanHealth);
    check.checkAdvanced();

    ArgumentCaptor<URI> argument = ArgumentCaptor.forClass(URI.class);
    verify(client).target(argument.capture());

    assertNotNull(argument.getValue());
    String path = argument.getValue().getPath();
    assertNotNull(path);
    assertThat(path, is(expectedPath));
  }

  private Client primeClientToGive(int status) {
    Invocation.Builder builder = mock(Invocation.Builder.class);
    when(builder.header(any(String.class), any(String.class))).thenReturn(builder);
    WebTarget target = mock(WebTarget.class);
    when(target.request()).thenReturn(builder);

    Client client = mock(Client.class);
    when(client.target(any(URI.class))).thenReturn(target);

    Response mockResponse = mock(Response.class);
    when(mockResponse.getStatus()).thenReturn(status);
    when(builder.get()).thenReturn(mockResponse);

    return client;
  }
}
