package com.ft.up.apipolicy.filters;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PolicyBasedJsonFilterTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final MutableRequest request = mock(MutableRequest.class);
  private final HttpPipelineChain chain = mock(HttpPipelineChain.class);
  private final MutableResponse response = new MutableResponse();

  @Before
  public void setUp() {
    response.setStatus(SC_OK);
  }

  @Test
  public void thatPathMappedToNullPolicyIsWhitelisted() throws Exception {
    Map<String, Object> object = singletonMap("foo", "bar");
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(singletonMap("$.foo", null));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }

  @Test
  public void thatPathMappedToPresentPolicyIsPreserved() throws Exception {
    Map<String, Object> object = singletonMap("foo", "bar");
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(singletonMap("$.foo", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }

  @Test
  public void thatPathMappedToAbsentPolicyIsRemoved() throws Exception {
    Map<String, Object> object = new HashMap<>();
    object.put("foo", "bar");
    object.put("fish", "wibble");

    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);
    int expectedStatus = response.getStatus();

    when(chain.callNextFilter(request)).thenReturn(response);

    Map<String, List<Policy>> policies = new HashMap<>();
    policies.put("$.foo", null);
    policies.put("$.fish", singletonList(Policy.INTERNAL_UNSTABLE));
    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(policies);

    MutableResponse actualResponse = f.processRequest(request, chain);

    assertThat(actualResponse.getStatus(), equalTo(expectedStatus));

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    Map<String, Object> expected = singletonMap("foo", "bar");
    assertThat(actual, equalTo(expected));
  }

  @Test
  public void that2XXStatusIsMapped() throws Exception {
    response.setStatus(SC_CREATED);
    thatPathMappedToAbsentPolicyIsRemoved();
  }

  @Test
  public void that204StatusIsHandled() {
    response.setStatus(SC_NO_CONTENT);

    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f = new PolicyBasedJsonFilter(singletonMap("$.foo", null));

    MutableResponse actualResponse = f.processRequest(request, chain);

    assertThat(actualResponse.getStatus(), equalTo(SC_NO_CONTENT));
    // accept null or an empty array as the response entity
    assertArrayEquals(
        Optional.ofNullable(actualResponse.getEntity()).orElse(new byte[0]), new byte[0]);
  }

  @Test
  public void thatNoAllowedPathsReturnsEmptyMap() throws Exception {
    Map<String, Object> object = singletonMap("foo", "bar");
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(singletonMap("$.foo", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(0));
  }

  @Test
  public void thatNestedPathsAreTraversed() throws Exception {
    Map<String, Object> inner = singletonMap("bar", "baz");
    Map<String, Object> object = singletonMap("foo", inner);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(
            singletonMap("$.foo.bar", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }

  @Test
  public void thatPathDoesNotDescendIntoNestedObject() throws Exception {
    Map<String, Object> inner = singletonMap("bar", "baz");
    Map<String, Object> object = singletonMap("foo", inner);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(singletonMap("$.foo", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(1));

    Map<String, Object> foo = (Map) actual.get("foo");
    assertThat(foo.size(), equalTo(0));
  }

  @Test
  public void thatWildcardPathMappedToPresentPolicyIsPreserved() throws Exception {
    Map<String, Object> inner = new HashMap<>();
    inner.put("red", "elephant");
    inner.put("blue", "mouse");
    Map<String, Object> middle = singletonMap("bar", inner);
    Map<String, Object> object = singletonMap("foo", middle);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(
            singletonMap("$.foo.*.red", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    Map<String, Object> expected =
        singletonMap("foo", singletonMap("bar", singletonMap("red", "elephant")));
    assertThat(actual, equalTo(expected));
  }

  @Test
  public void thatWildcardTerminalPreservesDeepObjects() throws Exception {
    Map<String, Object> inner = singletonMap("baz", "elephant");
    Map<String, Object> middle = singletonMap("bar", inner);
    Map<String, Object> object = singletonMap("foo", middle);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(singletonMap("$.foo.*", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual, equalTo(object));
  }

  @Test
  public void thatArraysAreTraversed() throws Exception {
    Map<String, Object> first = singletonMap("bar", "baz");
    Map<String, Object> second = singletonMap("bar", "wibble");
    List<Object> list = Arrays.asList(first, second);
    Map<String, Object> object = singletonMap("foo", list);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(
            singletonMap("$.foo[1].bar", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(1));
    List<Object> foo = (List) actual.get("foo");
    assertThat(foo.size(), equalTo(2));

    Map<String, Object> actualFirst = (Map) foo.get(0);
    assertThat(actualFirst.size(), equalTo(0));
    Map<String, Object> actualSecond = (Map) foo.get(1);
    assertThat(actualSecond, equalTo(second));
  }

  @Test
  public void thatWildcardIndexesArePreserved() throws Exception {
    Map<String, Object> first = new HashMap<>();
    first.put("bar", "baz");
    first.put("fish", "wibble");

    Map<String, Object> second = new HashMap<>();
    second.put("bar", "red");
    second.put("fish", "blue");

    List<Object> list = Arrays.asList(first, second);
    Map<String, Object> object = singletonMap("foo", list);
    byte[] entity = MAPPER.writer().writeValueAsBytes(object);
    response.setEntity(entity);

    when(request.getPolicies())
        .thenReturn(Collections.singleton(Policy.INTERNAL_UNSTABLE.toString()));
    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(
            Collections.singletonMap("$.foo[*].bar", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    Map<String, Object> actual =
        MAPPER.readValue(actualResponse.getEntity(), JsonConverter.JSON_MAP_TYPE);
    assertThat(actual.size(), equalTo(1));
    List<Object> foo = (List) actual.get("foo");
    assertThat(foo.size(), equalTo(2));

    Map<String, Object> actualFirst = (Map) foo.get(0);
    Map<String, Object> expectedFirst = singletonMap("bar", "baz");
    assertThat(actualFirst, equalTo(expectedFirst));

    Map<String, Object> actualSecond = (Map) foo.get(1);
    Map<String, Object> expectedSecond = singletonMap("bar", "red");
    assertThat(actualSecond, equalTo(expectedSecond));
  }

  @Test
  public void thatFilterIsTransparentForErrorResponse() {
    String raw = "foobar";
    response.setEntity(raw.getBytes());
    response.setStatus(SC_BAD_REQUEST);

    when(chain.callNextFilter(request)).thenReturn(response);

    PolicyBasedJsonFilter f =
        new PolicyBasedJsonFilter(singletonMap("$.foo", singletonList(Policy.INTERNAL_UNSTABLE)));

    MutableResponse actualResponse = f.processRequest(request, chain);

    assertThat(actualResponse.getStatus(), equalTo(SC_BAD_REQUEST));

    String actual = new String(actualResponse.getEntity());
    assertThat(actual, equalTo(raw));
  }
}
