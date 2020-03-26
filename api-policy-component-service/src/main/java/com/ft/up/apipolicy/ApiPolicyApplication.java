package com.ft.up.apipolicy;

import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.platform.dropwizard.GoodToGoBundle;
import com.ft.platform.dropwizard.GoodToGoResult;
import com.ft.up.apipolicy.configuration.ApiFilters;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.KnownEndpoint;
import com.ft.up.apipolicy.resources.RequestHandler;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.ft.up.apipolicy.util.FluentLoggingBuilder;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.server.ServerProperties;

import javax.servlet.DispatcherType;
import javax.ws.rs.client.Client;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class ApiPolicyApplication extends Application<ApiPolicyConfiguration> {

    public static void main(final String[] args) throws Exception {
        new ApiPolicyApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<ApiPolicyConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
        bootstrap.addBundle(new GoodToGoBundle(environment -> new GoodToGoResult(true, "")));
    }

    @Override
    public void run(final ApiPolicyConfiguration configuration, final Environment environment) {
        environment.jersey().property(ServerProperties.LOCATION_HEADER_RELATIVE_URI_RESOLUTION_DISABLED, true);
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new ApiPolicyExceptionMapper());
        ApiFilters apiFilters = new ApiFilters();
        apiFilters.initFilters(configuration, environment);

        Set<KnownEndpoint> knownWildcardEndpoints = getKnownWildcardEndpoints(configuration, environment, apiFilters);
        Set<KnownEndpoint> knownWhitelistedPostEndpoints = getKnownWhitelistedPostEndpoints(configuration, environment);

        environment.jersey().register(new WildcardEndpointResource(new RequestHandler(new MutableHttpTranslator(), knownWildcardEndpoints),
                new RequestHandler(new MutableHttpTranslator(), knownWhitelistedPostEndpoints)));

        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        Client healthcheckClient = JerseyClientBuilder.newClient();
        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity", configuration.getVarnish(), healthcheckClient, configuration.isCheckingVulcanHealth()));

        FluentLoggingBuilder.getNewInstance(getClass().getName(), "run")
                .withField(FluentLoggingBuilder.MESSAGE, "Application started")
                .build().logInfo();
    }

    private Set<KnownEndpoint> getKnownWhitelistedPostEndpoints(ApiPolicyConfiguration configuration, Environment environment) {
        Set<KnownEndpoint> knownWhitelistedPostEndpoints = new LinkedHashSet<>();

        // Must specifically list any POST, PUT etc endpoint you want access to
        knownWhitelistedPostEndpoints.add(
                createEndpoint(environment, configuration, "^/suggest", "suggest"));

        return knownWhitelistedPostEndpoints;
    }

    private Set<KnownEndpoint> getKnownWildcardEndpoints(ApiPolicyConfiguration configuration, Environment environment, ApiFilters apiFilters) {
        Set<KnownEndpoint> knownWildcardEndpoints = new LinkedHashSet<>();

        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/things.*", "things"));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/lists/notifications.*", "lists-notifications", apiFilters.notificationsFilter()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/lists.*", "lists", apiFilters.listsFilters()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/internalcontent/.*", "internalcontent", apiFilters.internalContentFilters()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/internalcontent-preview/.*", "internalcontent-preview", apiFilters.internalContentPreviewFilters()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/enrichedcontent/.*", "enrichedcontent", apiFilters.enrichedContentFilters()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/content/notifications.*", "notifications", apiFilters.contentNotificationsFilters()));
        // no filters needed for public-annotations-api
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/content/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/annotations", "public-annotations-api"));
        //identifiersFilter needs to be added before webUrlAdder & canonicalWebUrlAdder in the pipeline since webUrlAdder's & canonicalWebUrlAdder's logic is based on the json property that identifiersFilter might remove
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/content/.*", "content", apiFilters.contentIdentifiersFilters()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/content-preview/.*", "content-preview", apiFilters.contentPreviewFilters()));
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/concordances.*", "concordances"));
        // DEFAULT CASE: Just forward it
        knownWildcardEndpoints.add(
                createEndpoint(environment, configuration, "^/.*", "other"));

        return knownWildcardEndpoints;
    }

    private KnownEndpoint createEndpoint(Environment environment, ApiPolicyConfiguration configuration,
                                         String urlPattern, String instanceName, ApiFilter... filterChain) {
        final Client client = JerseyClientBuilder.newBuilder()
                .property(ClientProperties.FOLLOW_REDIRECTS, false)
                .property(ClientProperties.CONNECT_TIMEOUT, configuration.getVarnish().getConnectTimeoutMillis())
                .property(ClientProperties.READ_TIMEOUT, configuration.getVarnish().getReadTimeoutMillis())
                .build();

        final RequestForwarder requestForwarder = new JerseyRequestForwarder(client, configuration.getVarnish());
        return new KnownEndpoint(urlPattern, new HttpPipeline(requestForwarder, filterChain));
    }
}
