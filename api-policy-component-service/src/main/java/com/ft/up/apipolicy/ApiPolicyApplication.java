package com.ft.up.apipolicy;

import com.ft.api.jaxrs.errors.RuntimeExceptionMapper;
import com.ft.api.util.buildinfo.BuildInfoResource;
import com.ft.api.util.transactionid.TransactionIdFilter;
import com.ft.jerseyhttpwrapper.ResilientClientBuilder;
import com.ft.platform.dropwizard.AdvancedHealthCheckBundle;
import com.ft.up.apipolicy.configuration.ApiPolicyConfiguration;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.filters.AddBrandFilterParameters;
import com.ft.up.apipolicy.filters.AddSyndication;
import com.ft.up.apipolicy.filters.LinkedContentValidationFilter;
import com.ft.up.apipolicy.filters.MediaResourceNotificationsFilter;
import com.ft.up.apipolicy.filters.PolicyBasedJsonFilter;
import com.ft.up.apipolicy.filters.PolicyBrandsResolver;
import com.ft.up.apipolicy.filters.RemoveJsonPropertyUnlessPolicyPresentFilter;
import com.ft.up.apipolicy.filters.SuppressJsonPropertyFilter;
import com.ft.up.apipolicy.filters.SuppressRichContentMarkupFilter;
import com.ft.up.apipolicy.filters.WebUrlCalculator;
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

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.DispatcherType;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ApiPolicyApplication extends Application<ApiPolicyConfiguration> {

    private static final String MAIN_IMAGE_JSON_PROPERTY = "mainImage";
    private static final String IDENTIFIERS_JSON_PROPERTY = "identifiers";
    private static final String ALT_TITLES_JSON_PROPERTY = "alternativeTitles";
    private static final String ALT_IMAGES_JSON_PROPERTY = "alternativeImages";
    private static final String ALT_STANDFIRST_JSON_PROPERTY = "alternativeStandfirsts";
    private static final String COMMENTS_JSON_PROPERTY = "comments";
    private static final String PROVENANCE_JSON_PROPERTY = "publishReference";
    private static final String LAST_MODIFIED_JSON_PROPERTY = "lastModified";
    private static final String OPENING_XML_JSON_PROPERTY = "openingXML";

    private ApiFilter mainImageFilter;
    private ApiFilter identifiersFilter;
    private ApiFilter alternativeTitlesFilter;
    private ApiFilter alternativeImagesFilter;
    private ApiFilter alternativeStandfirstsFilter;
    private ApiFilter stripCommentsFields;
    private ApiFilter removeCommentsFieldRegardlessOfPolicy;
    private ApiFilter stripProvenance;
    private ApiFilter suppressMarkup;
    private ApiFilter webUrlAdder;
    private ApiFilter addSyndication;
    private ApiFilter brandFilter;
    private ApiFilter stripLastModifiedDate;
    private ApiFilter _unstable_stripOpeningXml;
    private ApiFilter linkValidationFilter;
    private ApiFilter mediaResourceNotificationsFilter;

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
                identifiersFilter, webUrlAdder, addSyndication, linkValidationFilter, suppressMarkup, mainImageFilter, alternativeTitlesFilter, alternativeImagesFilter, alternativeStandfirstsFilter, removeCommentsFieldRegardlessOfPolicy, stripProvenance, stripLastModifiedDate, _unstable_stripOpeningXml));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content-preview/.*", "content-preview",
                identifiersFilter, webUrlAdder, addSyndication, suppressMarkup, mainImageFilter, alternativeTitlesFilter, alternativeImagesFilter, alternativeStandfirstsFilter, stripCommentsFields, stripProvenance, stripLastModifiedDate, _unstable_stripOpeningXml));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/content/notifications.*", "notifications", mediaResourceNotificationsFilter, brandFilter, notificationsFilter()));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/enrichedcontent/.*", "enrichedcontent",
                identifiersFilter, webUrlAdder, addSyndication, linkValidationFilter, suppressMarkup, mainImageFilter, alternativeTitlesFilter, alternativeImagesFilter, alternativeStandfirstsFilter, stripCommentsFields, stripProvenance, stripLastModifiedDate, _unstable_stripOpeningXml));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists.*", "lists", stripProvenance, stripLastModifiedDate));
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/lists/notifications.*", "lists-notifications", notificationsFilter()));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/concordances.*", "concordances", new ApiFilter[]{}));

        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/things.*", "things", new ApiFilter[]{}));

        // DEFAULT CASE: Just forward it
        knownWildcardEndpoints.add(createEndpoint(environment, configuration, "^/.*", "other", new ApiFilter[]{}));

        // Must specifically list any POST, PUT etc endpoint you want access to
        knownWhitelistedPostEndpoints.add(createEndpoint(environment, configuration, "^/suggest", "suggest", new ApiFilter[]{}));

        environment.jersey().register(new WildcardEndpointResource(new RequestHandler(new MutableHttpTranslator(), knownWildcardEndpoints),
                new RequestHandler(new MutableHttpTranslator(), knownWhitelistedPostEndpoints)));

        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");

        Client healthcheckClient;
        if (configuration.isCheckingVulcanHealth()) {
            healthcheckClient = ResilientClientBuilder.in(environment).usingDNS().named("healthcheck-client").build();
        } else {
            healthcheckClient = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).named("healthcheck-client").build();
        }
        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity ", configuration.getVarnish(), healthcheckClient, configuration.isCheckingVulcanHealth()));
    }

    private BodyProcessingFieldTransformer getBodyProcessingFieldTransformer() {
        return (BodyProcessingFieldTransformer) (new BodyProcessingFieldTransformerFactory()).newInstance();
    }

    private void setFilters(ApiPolicyConfiguration configuration, Environment environment) {
        JsonConverter jsonTweaker = new JsonConverter(environment.getObjectMapper());
        PolicyBrandsResolver resolver = configuration.getPolicyBrandsResolver();

        mainImageFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, MAIN_IMAGE_JSON_PROPERTY, Policy.INCLUDE_RICH_CONTENT);
        identifiersFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, IDENTIFIERS_JSON_PROPERTY, Policy.INCLUDE_IDENTIFIERS);
        alternativeTitlesFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, ALT_TITLES_JSON_PROPERTY, Policy.INTERNAL_UNSTABLE);
        alternativeImagesFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, ALT_IMAGES_JSON_PROPERTY, Policy.INTERNAL_UNSTABLE);
        alternativeStandfirstsFilter = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, ALT_STANDFIRST_JSON_PROPERTY, Policy.INTERNAL_UNSTABLE);
        stripCommentsFields = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, COMMENTS_JSON_PROPERTY, Policy.INCLUDE_COMMENTS);
        removeCommentsFieldRegardlessOfPolicy = new SuppressJsonPropertyFilter(jsonTweaker, COMMENTS_JSON_PROPERTY);
        stripProvenance = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, PROVENANCE_JSON_PROPERTY, Policy.INCLUDE_PROVENANCE);
        stripLastModifiedDate =  new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, LAST_MODIFIED_JSON_PROPERTY, Policy.INCLUDE_LAST_MODIFIED_DATE);
        _unstable_stripOpeningXml = new RemoveJsonPropertyUnlessPolicyPresentFilter(jsonTweaker, OPENING_XML_JSON_PROPERTY, Policy.INTERNAL_UNSTABLE);
        suppressMarkup = new SuppressRichContentMarkupFilter(jsonTweaker, getBodyProcessingFieldTransformer());
        webUrlAdder = new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(), jsonTweaker);
        addSyndication = new AddSyndication(jsonTweaker, Policy.INTERNAL_UNSTABLE);
        brandFilter = new AddBrandFilterParameters(jsonTweaker, resolver);
        linkValidationFilter = new LinkedContentValidationFilter();
        mediaResourceNotificationsFilter = new MediaResourceNotificationsFilter(jsonTweaker);
    }
    
    private ApiFilter notificationsFilter() {
      Map<String,Policy> notificationsJsonFilters = new HashMap<>();
      // whitelisted (no policy required)
      notificationsJsonFilters.put("$.requestUrl", null);
      notificationsJsonFilters.put("$.links[*].*", null);
      notificationsJsonFilters.put("$.notifications[*].id", null);
      notificationsJsonFilters.put("$.notifications[*].type", null);
      notificationsJsonFilters.put("$.notifications[*].apiUrl", null);
      // restricted (policy required)
      notificationsJsonFilters.put("$.notifications[*].lastModified", Policy.INCLUDE_LAST_MODIFIED_DATE);
      notificationsJsonFilters.put("$.notifications[*].notificationDate", Policy.INCLUDE_LAST_MODIFIED_DATE);
      notificationsJsonFilters.put("$.notifications[*].publishReference", Policy.INCLUDE_PROVENANCE);

      return new PolicyBasedJsonFilter(notificationsJsonFilters);
    }
    
    private KnownEndpoint createEndpoint(Environment environment, ApiPolicyConfiguration configuration,
                                         String urlPattern, String instanceName, ApiFilter... filterChain) {
        final Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).named(instanceName).build();

        final RequestForwarder requestForwarder = new JerseyRequestForwarder(client, configuration.getVarnish());
        final KnownEndpoint endpoint = new KnownEndpoint(urlPattern,
                new HttpPipeline(requestForwarder, filterChain));
        return endpoint;
    }
}
