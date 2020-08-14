package com.ft.up.apipolicy.filters;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.HttpPipeline;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Arrays;
import java.util.Collections;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * AddBrandFilterParametersTest
 *
 * @author Simon.Gibbs
 */
@RunWith(MockitoJUnitRunner.class)
public class AddBrandFilterParametersTest {

  public static final String ERROR_RESPONSE = "{ \"message\" : \"Error\" }";

  public static final String MINIMAL_EXAMPLE_RESPONSE =
      "{ \"requestUrl\": \"http://example.org/content/100?forBrand=ONE&notForBrand=TWO\" }";

  @Mock private HttpPipelineChain mockChain;

  @Mock private PolicyBrandsResolver policyBrandsResolver;

  private MutableRequest exampleRequest =
      new MutableRequest(Collections.singleton("TEST"), getClass().getSimpleName());

  private MutableResponse exampleErrorResponse;
  private MutableResponse minimalExampleResponse;

  @Before
  public void setUpExamples() {

    MultivaluedMap<String, Object> allHeaders = new MultivaluedHashMap<>();
    allHeaders.put(
        HttpPipeline.POLICY_HEADER_NAME,
        Arrays.asList("FASTFT_CONTENT_ONLY", "EXCLUDE_FASTFT_CONTENT"));

    exampleErrorResponse = new MutableResponse(allHeaders, ERROR_RESPONSE.getBytes());
    exampleErrorResponse.setStatus(500);

    minimalExampleResponse = new MutableResponse(allHeaders, MINIMAL_EXAMPLE_RESPONSE.getBytes());
    minimalExampleResponse.setStatus(200);
  }

  @Test
  public void shouldNotProcessErrorResponse() {

    when(mockChain.callNextFilter(exampleRequest)).thenReturn(exampleErrorResponse);

    AddBrandFilterParameters filter =
        new AddBrandFilterParameters(JsonConverter.testConverter(), policyBrandsResolver);

    MutableResponse response = filter.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntity(), is(ERROR_RESPONSE.getBytes()));
  }

  @Test
  public void shouldNotAddQueryParamsToResponse() {

    when(mockChain.callNextFilter(exampleRequest)).thenReturn(minimalExampleResponse);

    AddBrandFilterParameters filter =
        new AddBrandFilterParameters(JsonConverter.testConverter(), policyBrandsResolver);

    MutableResponse response = filter.processRequest(exampleRequest, mockChain);

    assertThat(response.getEntityAsString(), not(containsString("forBrand")));
    assertThat(response.getEntityAsString(), not(containsString("notForBrand")));
  }
}
