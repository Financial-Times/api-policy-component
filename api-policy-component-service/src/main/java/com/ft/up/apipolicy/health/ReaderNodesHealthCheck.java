package com.ft.up.apipolicy.health;

import com.ft.jerseyhttpwrapper.config.EndpointConfiguration;
import com.ft.platform.dropwizard.AdvancedHealthCheck;
import com.ft.platform.dropwizard.AdvancedResult;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.http.HttpStatus;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class ReaderNodesHealthCheck extends AdvancedHealthCheck {

    private final EndpointConfiguration endpoint;
    private final Client client;
    private boolean checkVulcanHealth = false;

    public ReaderNodesHealthCheck(final String name, final EndpointConfiguration endpoint, final Client client, boolean checkVulcanHealth) {
        super(name);
        this.endpoint = endpoint;
        this.client = client;
        this.checkVulcanHealth = checkVulcanHealth;
    }

    @Override
    protected AdvancedResult checkAdvanced() throws Exception {

        URI healthcheckUri;

        if (checkVulcanHealth) {
            healthcheckUri = UriBuilder
                    .fromPath("/v1/status")
                    .host(endpoint.getHost())
                    .port(endpoint.getAdminPort())
                    .scheme("http")
                    .build();
        } else {
            healthcheckUri = UriBuilder
                    .fromPath("/build-info")
                    .host(endpoint.getHost())
                    .port(endpoint.getPort())
                    .scheme("http")
                    .build();
        }

        System.out.println("healthcheckUri: "+healthcheckUri.toString());

        ClientResponse response = null;
        try {
            // Resilient Client provides functionality to try each node until at least one reports 200 OK.
            response = client.resource(healthcheckUri).header("Cache-Control", "max-age=0").get(ClientResponse.class);

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
        return "https://sites.google.com/a/ft.com/dynamic-publishing-team/api-policy-component";
    }

}