package com.ft.up.apipolicy.health;

import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;
import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.http.HttpStatus;

public class ReaderNodesHealthCheck extends AdvancedHealthCheck {

  private final EndpointConfiguration endpoint;
  private final Client client;
  private boolean checkVulcanHealth = false;

  public ReaderNodesHealthCheck(
      final String name,
      final EndpointConfiguration endpoint,
      final Client client,
      boolean checkVulcanHealth) {
    super(name);
    this.endpoint = endpoint;
    this.client = client;
    this.checkVulcanHealth = checkVulcanHealth;
  }

  @Override
  protected AdvancedResult checkAdvanced() throws Exception {
    URI healthcheckUri;

    if (checkVulcanHealth) {
      healthcheckUri =
          UriBuilder.fromPath("/v2/status")
              .host(endpoint.getHost())
              .port(endpoint.getAdminPort())
              .scheme("http")
              .build();
    } else {
      healthcheckUri =
          UriBuilder.fromPath("/status")
              .host(endpoint.getHost())
              .port(endpoint.getPort())
              .scheme("http")
              .build();
    }

    Response response = null;
    try {
      response = client.target(healthcheckUri).request().header("Cache-Control", "max-age=0").get();

      if (response.getStatus() == HttpStatus.SC_OK) {
        return AdvancedResult.healthy("All is ok");
      }

      return AdvancedResult.error(this, "Unexpected response status: " + response.getStatus());
    } catch (Exception e) {
      return AdvancedResult.error(this, "Could not connect", e);
    } finally {
      if (response != null) {
        response.close();
      }
    }
  }

  @Override
  protected int severity() {
    return 1;
  }

  @Override
  protected String businessImpact() {
    return "Business partners will not be able to access content while the problem continues reducing editorial reach and influence.";
  }

  @Override
  protected String technicalSummary() {
    return "This service facade cannot connect to the services behind it. Requests from partners will fail to be processed.";
  }

  @Override
  protected String panicGuideUrl() {
    return "https://runbooks.in.ft.com/api-policy-component";
  }
}
