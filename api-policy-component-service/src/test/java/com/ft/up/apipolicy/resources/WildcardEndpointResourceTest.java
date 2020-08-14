package com.ft.up.apipolicy.resources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.DummyFilter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.MutableHttpTranslator;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.pipeline.RequestForwarder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WildcardEndpointResourceTest {

  @Rule public org.junit.rules.ExpectedException expectedException = ExpectedException.none();

  private WildcardEndpointResource wildcardEndpointResource;
  private HttpServletRequest request;
  private UriInfo uriInfo;
  private HttpPipeline contentPipeline;
  private HttpPipeline notificationsPipeline;
  private HttpPipeline defaultPipeline;
  private HttpPipeline suggestPipeline;

  private static final URI LOCALHOST = URI.create("http://localhost");

  @Before
  public void setup() throws IOException {
    RequestForwarder requestForwarder = mock(RequestForwarder.class);
    MutableResponse mutableResponse =
        new MutableResponse(new MultivaluedHashMap<>(), "response".getBytes());
    when(requestForwarder.forwardRequest(any(MutableRequest.class))).thenReturn(mutableResponse);

    SortedSet<KnownEndpoint> knownEndpoints = new TreeSet<>();

    ApiFilter mockFilter = new DummyFilter();

    contentPipeline = spy(new HttpPipeline(requestForwarder, mockFilter));
    knownEndpoints.add(new KnownEndpoint("^/content/.*", contentPipeline));

    notificationsPipeline = spy(new HttpPipeline(requestForwarder, mockFilter));
    knownEndpoints.add(new KnownEndpoint("^/content/notifications.*", notificationsPipeline));

    // DEFAULT CASE
    defaultPipeline = spy(new HttpPipeline(requestForwarder, mockFilter));
    knownEndpoints.add(new KnownEndpoint("^/.*", defaultPipeline));

    SortedSet<KnownEndpoint> knownWhitelistedNonIdempotentEndpoints = new TreeSet<>();

    suggestPipeline = spy(new HttpPipeline(requestForwarder, mockFilter));
    knownWhitelistedNonIdempotentEndpoints.add(new KnownEndpoint("^/suggest/.*", suggestPipeline));

    wildcardEndpointResource =
        new WildcardEndpointResource(
            new RequestHandler(new MutableHttpTranslator(), knownEndpoints),
            new RequestHandler(
                new MutableHttpTranslator(), knownWhitelistedNonIdempotentEndpoints));

    request = mock(HttpServletRequest.class);
    when(request.getHeaderNames()).thenReturn(Collections.<String>emptyEnumeration());

    uriInfo = mock(UriInfo.class);
  }

  @Test
  public void shouldUseNotificationsPipelineWhenNotificationsUrlPassed() throws URISyntaxException {
    mockEnvironmentForRequestTo("/content/notifications");
    when(request.getMethod()).thenReturn("GET");

    wildcardEndpointResource.get(request, uriInfo);
    verify(contentPipeline, never()).forwardRequest(any(MutableRequest.class));
    verify(notificationsPipeline, times(1)).forwardRequest(any(MutableRequest.class));
  }

  private StringBuffer absoluteUriFor(URI notificationsPath) {
    return new StringBuffer(LOCALHOST.resolve(notificationsPath).toString());
  }

  @Test
  public void shouldUseContentPipelineWhenContentUrlPassed() throws URISyntaxException {
    mockEnvironmentForRequestTo("/content/54307a12-37fa-11e3-8f44-002128161462");
    when(request.getMethod()).thenReturn("GET");

    wildcardEndpointResource.get(request, uriInfo);

    verify(contentPipeline, times(1)).forwardRequest(any(MutableRequest.class));
    verify(notificationsPipeline, never()).forwardRequest(any(MutableRequest.class));
  }

  private void mockEnvironmentForRequestTo(String path) throws URISyntaxException {
    URI contentPath = new URI(path);
    when(uriInfo.getAbsolutePath()).thenReturn(contentPath);
    when(request.getRequestURL()).thenReturn(absoluteUriFor(contentPath));
    when(uriInfo.getBaseUri()).thenReturn(LOCALHOST);
    when(uriInfo.getPath()).thenReturn(path);
  }

  @Test
  public void shouldUseSuggestPipelineWhenSuggestUrlPassed()
      throws URISyntaxException, IOException {
    mockEnvironmentForRequestTo("/suggest/54307a12-37fa-11e3-8f44-002128161462");
    when(request.getMethod()).thenReturn("POST");
    Object requestEntity = "{\"body\": \"text\"" + "}";
    when(request.getInputStream()).thenReturn(createServletInputStream(requestEntity));

    wildcardEndpointResource.post(request, uriInfo);

    verify(suggestPipeline, times(1)).forwardRequest(any(MutableRequest.class));
  }

  @Test
  public void shouldUseDefautlPipelineWhenUnknownUrlPassedToGet() throws URISyntaxException {
    mockEnvironmentForRequestTo("/you_ready_folks?");
    when(request.getMethod()).thenReturn("GET");

    wildcardEndpointResource.get(request, uriInfo);

    verify(contentPipeline, never()).forwardRequest(any(MutableRequest.class));
    verify(notificationsPipeline, never()).forwardRequest(any(MutableRequest.class));
    verify(defaultPipeline, times(1)).forwardRequest(any(MutableRequest.class));
  }

  @Test
  public void shouldThrowUnsupportedRequestExceptionWhenUnknownUrlPassedToPost()
      throws URISyntaxException {
    mockEnvironmentForRequestTo("/non-matching/54307a12-37fa-11e3-8f44-002128161462");
    when(request.getMethod()).thenReturn("POST");

    Response actual = wildcardEndpointResource.post(request, uriInfo);
    assertThat("status", actual.getStatus(), equalTo(HttpStatus.SC_METHOD_NOT_ALLOWED));

    Object entity = actual.getEntity();
    assertThat(
        "message",
        (String) ((Map) entity).get("message"),
        containsString(
            "Unsupported request: path [/non-matching/54307a12-37fa-11e3-8f44-002128161462] "
                + "with method [POST]."));

    verify(suggestPipeline, never()).forwardRequest(any(MutableRequest.class));
  }

  private ServletInputStream createServletInputStream(Object object) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(object);

    final InputStream byteArrayInputStream =
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

    return new ServletInputStream() {

      @Override
      public int read() throws IOException {
        return byteArrayInputStream.read();
      }

      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return false;
      }

      @Override
      public void setReadListener(ReadListener arg0) {}
    };
  }
}
