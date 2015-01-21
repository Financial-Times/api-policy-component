package com.ft.up.apipolicy;

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
import com.ft.up.apipolicy.filters.AddBrandFilterParameters;
import com.ft.up.apipolicy.filters.MainImageFilter;
import com.ft.up.apipolicy.filters.WebUrlCalculator;
import com.ft.up.apipolicy.health.ReaderNodesHealthCheck;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import com.ft.up.apipolicy.resources.KnownEndpoint;
import com.ft.up.apipolicy.resources.WildcardEndpointResource;
import com.sun.jersey.api.client.Client;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ApiPolicyApplication extends Application<ApiPolicyConfiguration> {

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
        Client client = ResilientClientBuilder.in(environment).using(configuration.getVarnish()).build();


		RequestForwarder requestForwarder = new JerseyRequestForwarder(client,configuration.getVarnish());

        JsonConverter jsonTweaker = new JsonConverter(environment.getObjectMapper());


        final ApiFilter webUrlAdder = new WebUrlCalculator(configuration.getPipelineConfiguration().getWebUrlTemplates(),jsonTweaker);
        final ApiFilter mainImageFilter = new MainImageFilter(jsonTweaker);

        SortedSet<KnownEndpoint> knownEndpoints = new TreeSet<>();
		knownEndpoints.add(new KnownEndpoint("^/content/.*",
				new HttpPipeline(requestForwarder, webUrlAdder, mainImageFilter)));

        knownEndpoints.add(new KnownEndpoint("^/content/notifications.*",
                new HttpPipeline(requestForwarder, new AddBrandFilterParameters(jsonTweaker))));

        knownEndpoints.add(new KnownEndpoint("^/enrichedcontent/.*",
                new HttpPipeline(requestForwarder, webUrlAdder, mainImageFilter)));

        // DEFAULT CASE: Just forward it
        knownEndpoints.add(new KnownEndpoint("^/.*", new HttpPipeline(requestForwarder)));


        environment.jersey().register(new WildcardEndpointResource(new MutableHttpTranslator(), knownEndpoints));

        environment.healthChecks()
                .register("Reader API Connectivity",
                        new ReaderNodesHealthCheck("Reader API Connectivity", configuration.getVarnish(), client));

        environment.servlets().addFilter("Transaction ID Filter",
                new TransactionIdFilter()).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/content/*");
    }

}
