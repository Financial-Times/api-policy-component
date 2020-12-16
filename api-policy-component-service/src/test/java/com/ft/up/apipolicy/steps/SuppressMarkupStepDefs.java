package com.ft.up.apipolicy.steps;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.filters.SuppressRichContentMarkupFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformer;
import com.ft.up.apipolicy.transformer.BodyProcessingFieldTransformerFactory;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import java.util.HashMap;
import javax.ws.rs.core.MultivaluedHashMap;

public class SuppressMarkupStepDefs {

  private HttpPipelineChain mockChain;

  private MutableRequest mockRequest;

  private String unprocessedMarkup;

  private String processedMarkup;
  private JsonConverter jsonConverter;

  @Before
  public void setup() {
    mockRequest = mock(MutableRequest.class);
    mockChain = mock(HttpPipelineChain.class);

    jsonConverter = JsonConverter.testConverter();
  }

  @Given("^the unprocessed markup (.+)$")
  public void the_unprocessed_markup_(String unprocessedMarkup) throws Throwable {
    this.unprocessedMarkup = unprocessedMarkup;
  }

  @When("^it is transformed$")
  public void it_is_transformed() throws Throwable {

    HashMap<String, Object> jsonEntity = new HashMap<>(1);
    jsonEntity.put("bodyXML", (Object) wrap(unprocessedMarkup));

    MutableResponse expectedResponse = defaultResponse();

    jsonConverter.replaceEntity(expectedResponse, jsonEntity);

    when(mockChain.callNextFilter(any(MutableRequest.class))).thenReturn(expectedResponse);

    SuppressRichContentMarkupFilter filter =
        new SuppressRichContentMarkupFilter(jsonConverter, getBodyProcessingFieldTransformer());

    MutableResponse rawResponse = filter.processRequest(mockRequest, mockChain);

    processedMarkup = (String) jsonConverter.readEntity(rawResponse).get("bodyXML");
  }

  private MutableResponse defaultResponse() {
    MutableResponse expectedResponse = new MutableResponse();
    MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
    headers.putSingle("Content-Type", "application/json");
    expectedResponse.setHeaders(headers);
    return expectedResponse;
  }

  @Then("^the mark up is removed$")
  public void the_mark_up_is_removed() throws Throwable {
    assertThat(processedMarkup, is(""));
  }

  private BodyProcessingFieldTransformer getBodyProcessingFieldTransformer() {
    return (BodyProcessingFieldTransformer)
        (new BodyProcessingFieldTransformerFactory()).newInstance();
  }

  private String wrap(String html) {
    return "<body>" + html + "</body>";
  }
}
