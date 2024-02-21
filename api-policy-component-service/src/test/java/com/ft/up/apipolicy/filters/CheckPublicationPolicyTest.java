package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckPublicationPolicyTest {

  @Mock private HttpPipelineChain mockChain;

  private final CheckPublicationPolicy checkPublicationPolicy =
      new CheckPublicationPolicy(JsonConverter.testConverter());

  @Test
  public void ShouldReturnOK() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\",\"publication\":[\"http://www.ft.com/thing/8e6c705e-1132-42a2-8db0-c295e29e8658\",\"http://www.ft.com/thing/88fdde6c-2aa4-4f78-af02-9f680097cfd6\"] }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    HashSet<String> p =
        new HashSet<>(
            Arrays.asList("INTERNAL_UNSTABLE", "PBLC_READ_8e6c705e-1132-42a2-8db0-c295e29e8658"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void LegacyAPIKeyWhitespaceShouldReturnOK() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\",\"publication\":[\"http://www.ft.com/thing/8e6c705e-1132-42a2-8db0-c295e29e8658\",\"http://www.ft.com/thing/88fdde6c-2aa4-4f78-af02-9f680097cfd6\"] }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    HashSet<String> p =
        new HashSet<>(
            Arrays.asList(
                "INTERNAL_UNSTABLE", " ", " PBLC_READ_8e6c705e-1132-42a2-8db0-c295e29e8658"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void ShouldReturn403() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\",\"publication\":[\"http://www.ft.com/thing/88fdde6c-2aa4-4f78-af02-9f680097cfd6\"] }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    HashSet<String> p =
        new HashSet<>(
            Arrays.asList("INTERNAL_UNSTABLE", "PBLC_READ_8e6c705e-1132-42a2-8db0-c295e29e8658"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(403));
  }

  @Test
  public void ShouldDeniedAccessNoPublicationFieldFTPink() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\" }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    HashSet<String> p =
        new HashSet<>(
            Arrays.asList("INTERNAL_UNSTABLE", "PBLC_READ_8e6c705e-1132-42a2-8db0-c295e29e8658"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(403));
  }

  @Test
  public void ShouldReturnOKNoPublicationFieldFTPinkReadFTPinkPolicy() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\" }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    HashSet<String> p =
        new HashSet<>(
            Arrays.asList("INTERNAL_UNSTABLE", "PBLC_READ_88fdde6c-2aa4-4f78-af02-9f680097cfd6"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void ShouldReturnOkWithNoPublicationReadPolicyFtPink() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\",\"publication\":[\"http://www.ft.com/thing/88fdde6c-2aa4-4f78-af02-9f680097cfd6\"] }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    validResponse.getHeaders().putSingle("X-Policy", "INTERNAL_UNSTABLE");
    HashSet<String> p = new HashSet<>(Collections.singletonList("INTERNAL_UNSTABLE"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  public void ShouldDeniedAccessNoPublicationPolicyNonPinkFt() {
    final MutableRequest mockedRequest = mock(MutableRequest.class);
    String entity =
        "{ \"type\":\"http://www.ft.com/ontology/content/Article\", \"bodyXML\": \"<body>something here</body>\",\"publication\":[\"http://www.ft.com/thing/8e6c705e-1132-42a2-8db0-c295e29e8658\"] }";
    MutableResponse validResponse =
        new MutableResponse(new MultivaluedHashMap<>(), entity.getBytes());
    validResponse.setStatus(200);
    validResponse.getHeaders().putSingle("Content-Type", "application/json");
    validResponse.getHeaders().putSingle("X-Policy", "INTERNAL_UNSTABLE");
    HashSet<String> p = new HashSet<>(Collections.singletonList("INTERNAL_UNSTABLE"));
    when(mockChain.callNextFilter(mockedRequest)).thenReturn(validResponse);
    when(mockedRequest.getPolicies()).thenReturn(p);

    MutableResponse response = checkPublicationPolicy.processRequest(mockedRequest, mockChain);

    assertThat(response.getStatus(), is(403));
  }
}
