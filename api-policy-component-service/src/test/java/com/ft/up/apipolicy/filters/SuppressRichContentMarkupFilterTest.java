package com.ft.up.apipolicy.filters;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.configuration.Policy;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SuppressRichContentMarkupFilterTest {
  private SuppressRichContentMarkupFilter filter;
  @Mock private JsonConverter jsonConverter;
  @Mock private BodyProcessingFieldTransformer transformer;
  @Mock private MutableRequest request;
  @Mock private MutableResponse response;
  @Mock private HttpPipelineChain chain;

  @Before
  public void setUp() {
    filter = new SuppressRichContentMarkupFilter(jsonConverter, transformer);
  }

  @Test
  public void thatResponseIsUnchangedWhenRichContentPolicyIsAllowed() {
    when(chain.callNextFilter(request)).thenReturn(response);
    when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(true);

    MutableResponse actual = filter.processRequest(request, chain);

    assertSame(actual, response);
    verifyZeroInteractions(response);
  }

  @Test
  public void thatResponseIsUnchangedWhenNotJSON() {
    when(chain.callNextFilter(request)).thenReturn(response);
    when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(false);

    when(jsonConverter.isJson(response)).thenReturn(false);

    MutableResponse actual = filter.processRequest(request, chain);

    assertSame(actual, response);
    verifyZeroInteractions(response);
  }

  @Test
  public void thatRichContentIsRemovedFromBodyXML() {
    when(chain.callNextFilter(request)).thenReturn(response);
    when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(false);

    String richContent = "rich text";
    String plainContent = "plain text";

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("foo", "bar");
    json.put("bodyXML", richContent);

    Map<String, Object> transformed = new LinkedHashMap<>();
    transformed.put("foo", "bar");
    transformed.put("bodyXML", plainContent);

    when(jsonConverter.isJson(response)).thenReturn(true);
    when(jsonConverter.readEntity(response)).thenReturn(json);

    when(transformer.transform(eq(richContent), anyString())).thenReturn(plainContent);

    MutableResponse actual = filter.processRequest(request, chain);

    assertSame(actual, response);
    verify(jsonConverter).replaceEntity(response, transformed);
  }

  @Test
  public void thatRichContentIsRemovedFromOpeningXML() {
    when(chain.callNextFilter(request)).thenReturn(response);
    when(request.policyIs(Policy.INCLUDE_RICH_CONTENT)).thenReturn(false);

    String richContent = "rich text";
    String plainContent = "plain text";

    Map<String, Object> json = new LinkedHashMap<>();
    json.put("foo", "bar");
    json.put("openingXML", richContent);

    Map<String, Object> transformed = new LinkedHashMap<>();
    transformed.put("foo", "bar");
    transformed.put("openingXML", plainContent);

    when(jsonConverter.isJson(response)).thenReturn(true);
    when(jsonConverter.readEntity(response)).thenReturn(json);

    when(transformer.transform(eq(richContent), anyString())).thenReturn(plainContent);

    MutableResponse actual = filter.processRequest(request, chain);

    assertSame(actual, response);
    verify(jsonConverter).replaceEntity(response, transformed);
  }
}
