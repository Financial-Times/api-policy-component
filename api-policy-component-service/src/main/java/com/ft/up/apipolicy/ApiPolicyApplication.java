package com.ft.up.apipolicy;

import com.ft.up.apipolicy.filters.*;
import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.util.EnumSet;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.DispatcherType;

import com.ft.api.jaxrs.errors.RuntimeExceptionMapper;
import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.KnownEndpoint;
import com.ft.up.apipolicy.resources.RequestHandler;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformerFactory;
import com.sun.jersey.api.client.Client;

public class ApiPolicyApplication extends Application<ApiPolicyConfiguration> {

    private static final String MAIN_IMAGE_JSON_PROPERTY = "mainImage";
    private static final String IDENTIFIERS_JSON_PROPERTY = "identifiers";
    private static final String COMMENTS_JSON_PROPERTY = "comments";
    private static final String PROVENANCE_JSON_PROPERTY = "publishReference";
    private static final String LAST_MODIFIED_JSON_PROPERTY = "lastModified";

    private ApiFilter mainImageFilter;
    private ApiFilter identifiersFilter;
    private ApiFilter commentsFilterForEnrichedContentEndpoint;
    private ApiFilter commentsFilterForContentEndpoint;
    private ApiFilter stripProvenance;
    private ApiFilter suppressMarkup;
    private ApiFilter webUrlAdder;
    private ApiFilter brandFilter;
    private ApiFilter stripNestedProvenance;
    private ApiFilter stripLastModifiedDate;

    public static void main(final String[] args) throws Exception {
        new ApiPolicyApplication().run(args);
    }

    @Override
    public void initialize(final Bootstrap<ApiPolicyConfiguration> bootstrap) {
        bootstrap.addBundle(new AdvancedHealthCheckBundle());
    }

    @Override
    public void run(final ApiPolicyConfiguration configuration, final Environment environment) throws Exception {
        environment.jersey().register(new BuildInfoResource());
        environment.jersey().register(new RuntimeExceptionMapper());
        setFilters(configuration, environment);

        SortedSet<KnownEndpoint> knownWildcardEndpoints = new TreeSet<>();
        SortedSet<KnownEndpoint> knownWhitelistedPostEndpoints = new TreeSet<>();

        //identifiersFilter needs to be added before webUrlAdder in the pipeline since webUrlAdder's logic is based on the json property that identifiersFilter might remove
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/.*", "content",
                identifiersFilter, webUrlAdder, suppressMarkup, mainImageFilter, commentsFilterForContentEndpoint, stripProvenance, stripLastModifiedDate));
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/notifications.*", "notifications", brandFilter, stripNestedProvenance));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/enrichedcontent/.*", "enrichedcontent",
                identifiersFilter, webUrlAdder, suppressMarkup, mainImageFilter, commentsFilterForEnrichedContentEndpoint, stripProvenance, stripLastModifiedDate));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists/.*", "lists", stripProvenance, stripLastModifiedDate));

        // DEFAULT CASE: Just forward it
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/.*", "other", new ApiFilter[]{}));
        
        // Must specifically list any POST, PUT etc endpoint you want access to
        knownWhitelistedPostEndpoints.add(createEndpoint(environment, configuration, "^/suggest", "suggest", new ApiFilter[]{}));
        
        environment.jersey().register(new WildcardEndpointResource(new RequestHandler(new MutableHttpTranslator(), knownWildcardEndpoints),
                new RequestHandler(new MutableHttpTranslator(), knownWhitelistedPostEndpoints)));
        
        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        Client client = ResilientClientBuilder.in(environment).usingDNS()
                .named("vulcan-client")
                .build();
        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity ", configuration.getVarnish(), client, configuration.isCheckingVulcanHealth()));
    }

    private BodyProcessingFieldTransformer getBodyProcessingFieldTransformer() {
        return (BodyProcessingFieldTransformer) (new BodyProcessingFieldTransformerFactory()).newInstance();
    }

    private void setFilters(ApiPolicyConfiguration configuration, Environment environment) {
        JsonConverter jsonTweaker = new JsonConverter(environment.getObjectMapper());
        PolicyBrandsResolver resolver = configuration.getPolicyBrandsResolver();

        mainImageFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, MAIN_IMAGE_JSON_PROPERTY, Policy.INCLUDE_RICH_CONTENT);
        identifiersFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, IDENTIFIERS_JSON_PROPERTY, Policy.INCLUDE_IDENTIFIERS);
        commentsFilterForEnrichedContentEndpoint = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, COMMENTS_JSON_PROPERTY, Policy.INCLUDE_COMMENTS);
        commentsFilterForContentEndpoint = new SuppressJsonPropertyFilter(jsonTweaker, COMMENTS_JSON_PROPERTY);
        stripProvenance = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, PROVENANCE_JSON_PROPERTY, Policy.INCLUDE_PROVENANCE);
        stripNestedProvenance = new NotificationsProvenanceFilter(jsonTweaker, Policy.INCLUDE_PROVENANCE);
        stripLastModifiedDate =  new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, LAST_MODIFIED_JSON_PROPERTY, Policy.INCLUDE_LAST_MODIFIED_DATE);
        suppressMarkup = new SuppressRichContentMarkupFilter(jsonTweaker, getBodyProcessingFieldTransformer());
        webUrlAdder = new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(),
                jsonTweaker);
        brandFilter = new AddBrandFilterParameters(jsonTweaker, resolver);
    }

    private KnownEndpoint createEndpoint(Environment environment, ApiPolicyConfiguration configuration,
                                         String urlPattern, String instanceName, ApiFilter... filterChain) {
        Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).named(instanceName).build();
        RequestForwarder requestForwarder = new JerseyRequestForwarder(client,configuration.getVarnish());
        KnownEndpoint endpoint = new KnownEndpoint(urlPattern,
                new HttpPipeline(requestForwarder, filterChain));
        return endpoint;
    }
}
