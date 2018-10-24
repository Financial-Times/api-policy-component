package com.ft.up.apipolicy.filters;

import com.ft.api.jaxrs.errors.WebApplicationClientException;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.MultivaluedHashMap;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CanBeSyndicatedAccessFilterTest {

    private CanBeSyndicatedAccessFilter filter;

    @Mock
    private HttpPipelineChain mockChain;

    @Before
    public void setUp() {
        filter = new CanBeSyndicatedAccessFilter(JsonConverter.testConverter(), Policy.RESTRICT_NON_SYNDICATABLE_CONTENT);
    }

    @Test
    public void shouldNotProcessErrorResponse() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.RESTRICT_NON_SYNDICATABLE_CONTENT.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        final String responseBody = "{\"message\":\"TestError\"}";
        MutableResponse response = new MutableResponse(new MultivaluedHashMap<>(), responseBody.getBytes());
        response.setStatus(500);
        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes())));
    }

    @Test
    public void shouldNotModifyResponseWhenNoPolicy() {
        final Set<String> policies = new HashSet<>();
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());

        final String responseBody = "{\"bodyXML\":\"<body>Testing.</body>\",\"canBeSyndicated\":\"no\"}";
        final MutableResponse response = createSuccessfulResponse(responseBody);

        when(mockChain.callNextFilter(request)).thenReturn(response);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldNotModifyResponseWhenPolicyAndCanBeSyndicatedFieldYes() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.RESTRICT_NON_SYNDICATABLE_CONTENT.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        String responseBody = "{\"bodyXML\":\"<body>Testing.</body>\",\"canBeSyndicated\":\"yes\"}";
        MutableResponse chainedResponse = createSuccessfulResponse(responseBody);

        when(mockChain.callNextFilter(request)).thenReturn(chainedResponse);

        MutableResponse filteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(filteredResponse.getEntity()), is(new String(responseBody.getBytes(Charset.forName("UTF-8")))));
    }

    @Test
    public void shouldReturnForbiddenErrorWhenPolicyAndCanBeSyndicatedFieldNotYes() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.RESTRICT_NON_SYNDICATABLE_CONTENT.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        MutableResponse chainedResponse = createSuccessfulResponse("{\"bodyXML\":\"<body>Testing.</body>\",\"canBeSyndicated\":\"verify\"}");

        when(mockChain.callNextFilter(request)).thenReturn(chainedResponse);

        try {
            filter.processRequest(request, mockChain);
            fail("No exception was thrown, but expected one.");

        } catch (WebApplicationClientException e) {
            assertThat(e.getResponse().getStatus(), is(403));
        }
    }

    @Test
    public void shouldStripNestedImageWhenPolicyAndCanBeSyndicatedFieldNotYesForNestedImageContent() {
        final Set<String> policies = new HashSet<>();
        policies.add(Policy.RESTRICT_NON_SYNDICATABLE_CONTENT.getHeaderValue());
        final MutableRequest request = new MutableRequest(policies, getClass().getSimpleName());
        MutableResponse chainedResponse = createSuccessfulResponse("{\"bodyXML\":\"<body>Testing.</body>\",\"canBeSyndicated\":\"yes\",\"mainImage\":{\"id\":\"sampleId\",\"apiUrl\":\"sampleApiUrl\",\"canBeSyndicated\":\"verify\"}}");

        when(mockChain.callNextFilter(request)).thenReturn(chainedResponse);

        final String expectedFilteredResponseBody = "{\"bodyXML\":\"<body>Testing.</body>\",\"canBeSyndicated\":\"yes\"}";

        MutableResponse actualFilteredResponse = filter.processRequest(request, mockChain);

        assertThat(new String(actualFilteredResponse.getEntity()), is(new String(expectedFilteredResponseBody.getBytes(Charset.forName("UTF-8")))));

    }

    private MutableResponse createSuccessfulResponse(String body) {
        MutableResponse response = new MutableResponse(new MultivaluedHashMap<>(), body.getBytes(Charset.forName("UTF-8")));
        response.setStatus(200);
        response.getHeaders().putSingle("Content-Type", "application/json");
        return response;
    }
}
