package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;

import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class AddSyndicationTest {

    private final HttpPipelineChain mockChain = mock(HttpPipelineChain.class);
    private AddSyndication filter = new AddSyndication(JsonConverter.testConverter());
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static byte[] readFileBytes(final String path) {
        try {
            return Files.readAllBytes(Paths.get(AddSyndicationTest.class.getClassLoader().getResource(path).toURI()));
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void shouldNotProcessErrorResponse() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{ \"message\" : \"TestError\" }";
        MutableResponse response = new MutableResponse(new MultivaluedHashMap<>(), responseBody.getBytes());
        response.setStatus(500);
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes())));
    }

    @Test
    public void shouldNotProcessErrorResponseWhenNoPolicy() {
        final Set<String> policies = new HashSet<>();
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "";
        MutableResponse response = new MutableResponse(new MultivaluedHashMap<>(), responseBody.getBytes());
        response.setStatus(404);
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes())));
    }

    @Test
    public void shouldNotProcessNotJSON() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{ \"bodyXML\": \"<body>Testing.</body>\" }";
        final MutableResponse response = new MutableResponse(new MultivaluedHashMap<>(), responseBody.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/ld-json");
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldHaveOriginalFieldIfPresentAndPolicy() throws IOException {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.INTERNAL_UNSTABLE.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final MutableResponse initialResponse = new MutableResponse(new MultivaluedHashMap<>(), readFileBytes("sample-article-with-image.json"));
        initialResponse.setStatus(200);
        initialResponse.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(initialResponse);

        MutableResponse actualResponse = filter.processRequest(request, mockChain);

        final JsonNode actualTree = MAPPER.readTree(actualResponse.getEntityAsString());
        final JsonNode expectedTree = MAPPER.readTree(initialResponse.getEntityAsString());

        assertThat(actualTree, equalTo(expectedTree));
    }

    @Test
    public void shouldShowOriginalFieldIfPresentRegardlessPolicy() throws IOException {
        final MutableRequest request = new MutableRequest(new HashSet<>(), getClass().getSimpleName());
        final MutableResponse initialResponse = new MutableResponse(new MultivaluedHashMap<>(), readFileBytes("sample-article-with-image.json"));
        final MutableResponse expectedResponse = new MutableResponse(new MultivaluedHashMap<>(), readFileBytes("sample-article-with-image.json"));
        initialResponse.setStatus(200);
        initialResponse.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(initialResponse);

        MutableResponse actualResponse = filter.processRequest(request, mockChain);
        final JsonNode actualTree = MAPPER.readTree(actualResponse.getEntityAsString());
        final JsonNode expectedTree = MAPPER.readTree(expectedResponse.getEntityAsString());

        assertThat(actualTree, equalTo(expectedTree));
    }

    @Test
    public void shouldHaveFieldAddedIfMissingRegardlessPolicy() throws IOException {
        final MutableRequest request = new MutableRequest(new HashSet<>(), getClass().getSimpleName());
        final MutableResponse initialResponse = new MutableResponse(new MultivaluedHashMap<>(), readFileBytes("sample-article-no-canBeSyndicated.json"));
        final MutableResponse expectedResponse = new MutableResponse(new MultivaluedHashMap<>(), readFileBytes("sample-article-with-image.json"));
        initialResponse.setStatus(200);
        initialResponse.getHeaders().putSingle("Content-Type", "application/json");
        when(mockChain.callNextFilter(request)).thenReturn(initialResponse);

        MutableResponse actualResponse = filter.processRequest(request, mockChain);
        final JsonNode actualTree = MAPPER.readTree(actualResponse.getEntityAsString());
        final JsonNode expectedTree = MAPPER.readTree(expectedResponse.getEntityAsString());

        assertThat(actualTree, equalTo(expectedTree));
    }
}
