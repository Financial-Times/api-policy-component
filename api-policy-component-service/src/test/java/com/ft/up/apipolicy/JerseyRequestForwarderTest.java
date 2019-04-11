package com.ft.up.apipolicy;

import com.ft.up.apipolicy.configuration.EndpointConfiguration;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.net.URI;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import jersey.repackaged.com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyString;

/**
 *
 * @author peter.clark
 */
@RunWith(MockitoJUnitRunner.class)
public class JerseyRequestForwarderTest {

    private static final Logger LOG = LoggerFactory.getLogger(JerseyRequestForwarderTest.class);

    private static final String FASTFT_BRAND = "http://api.ft.com/things/5c7592a8-1f0c-11e4-b0cb-b2227cce2b54";

    @Mock private Client client;
    @Mock private WebTarget target;
    @Mock private Invocation.Builder builder;
    @Mock private Invocation invocation;
    @Mock private Response response;

    @Mock private EndpointConfiguration endpointConfiguration;

    @Before
    public void before(){
        when(endpointConfiguration.getHost()).thenReturn("hostname");
        when(endpointConfiguration.getPort()).thenReturn(8080);
        when(target.request()).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.build("GET")).thenReturn(invocation);
        when(invocation.invoke()).thenReturn(response);
    }

    @Test
    public void testForwardConceptSearchRequest() {
        JerseyRequestForwarder forwarder = new JerseyRequestForwarder(client, endpointConfiguration);

        MutableRequest request = new MutableRequest(Sets.newHashSet(), "tid_1234");
        request.setAbsolutePath("/concepts");
        request.setHttpMethod("GET");

        MultivaluedMap queryParams = new MultivaluedHashMap();
        queryParams.add("mode", "search");
        queryParams.add("type", "http://www.ft.com/ontology/person/Person");
        queryParams.add("type", "http://www.ft.com/ontology/organisation/Organisation");
        queryParams.add("type", "http://www.ft.com/ontology/Location");
        queryParams.add("type", "http://www.ft.com/ontology/Topic");
        queryParams.add("q", "new york");

        request.setQueryParameters(queryParams);

        MultivaluedMap headers = new MultivaluedHashMap();
        headers.add("X-Test-Header", "EXAMPLE");
        request.setHeaders(headers);

        URI conceptSearch = URI.create("http://hostname:8080/concepts?mode=search&q=new+york&type=http%3A%2F%2Fwww.ft.com%2Fontology%2Fperson%2FPerson&type=http%3A%2F%2Fwww.ft.com%2Fontology%2Forganisation%2FOrganisation&type=http%3A%2F%2Fwww.ft.com%2Fontology%2FLocation&type=http%3A%2F%2Fwww.ft.com%2Fontology%2FTopic");

        when(client.target(conceptSearch)).thenReturn(target);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap());
        when(response.getStatus()).thenReturn(200);

        MutableResponse actualResponse = forwarder.forwardRequest(request);
        assertEquals(HttpServletResponse.SC_OK, actualResponse.getStatus());

        verify(client).target(conceptSearch);
        verifyMocks();
    }

    @Test
    public void testForwardNotificationsRequest() {
        JerseyRequestForwarder forwarder = new JerseyRequestForwarder(client, endpointConfiguration);

        MutableRequest request = new MutableRequest(Sets.newHashSet(), "tid_1234");
        request.setAbsolutePath("/content/notifications");
        request.setHttpMethod("GET");

        MultivaluedMap queryParams = new MultivaluedHashMap();
        queryParams.add("forBrand", FASTFT_BRAND);
        queryParams.add("notForBrand", FASTFT_BRAND);
        queryParams.add("since", "2017-10-17T15:22:49.804Z");

        request.setQueryParameters(queryParams);

        MultivaluedMap headers = new MultivaluedHashMap();
        headers.add("X-Test-Header", "EXAMPLE");
        request.setHeaders(headers);

        URI notifications = URI.create("http://hostname:8080/content/notifications?since=2017-10-17T15%3A22%3A49.804Z");

        when(client.target(notifications)).thenReturn(target);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap());
        when(response.getStatus()).thenReturn(200);

        MutableResponse actualResponse = forwarder.forwardRequest(request);
        assertEquals(HttpServletResponse.SC_OK, actualResponse.getStatus());

        verify(client).target(notifications);
        verifyMocks();
    }

    private void verifyMocks(){
        verify(response).getHeaders();
        verify(response).getStatus();
        verify(target).request();
        verify(invocation).invoke();
        verify(builder).header("X-Test-Header", "EXAMPLE");
        verify(builder).build("GET");
    }
}
