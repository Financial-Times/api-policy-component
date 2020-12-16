package com.ft.up.apipolicy.filters;

import com.ft.up.apipolicy.JsonConverter;
import com.ft.up.apipolicy.pipeline.ApiFilter;
import com.ft.up.apipolicy.pipeline.HttpPipelineChain;
import com.ft.up.apipolicy.pipeline.MutableRequest;
import com.ft.up.apipolicy.pipeline.MutableResponse;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;

/**
 * AddBrandFilterParameters
 *
 * @author Simon.Gibbs
 */
public class AddBrandFilterParameters implements ApiFilter {

  public static final String REQUEST_URL_KEY = "requestUrl";
  private JsonConverter converter;
  private PolicyBrandsResolver policyBrandsResolver;

  public AddBrandFilterParameters(
      JsonConverter converter, PolicyBrandsResolver policyBrandsResolver) {
    this.policyBrandsResolver = policyBrandsResolver;
    this.converter = converter;
  }

  @Override
  public MutableResponse processRequest(MutableRequest request, HttpPipelineChain chain) {

    policyBrandsResolver.applyQueryParams(request);

    MutableResponse response = chain.callNextFilter(request);

    if (response.getStatus() != 200) {
      return response;
    }

    Map<String, Object> content = converter.readEntity(response);

    UriBuilder requestUriBuilder = UriBuilder.fromUri((String) content.get(REQUEST_URL_KEY));
    requestUriBuilder.replaceQueryParam("notForBrand", null);
    requestUriBuilder.replaceQueryParam("forBrand", null);

    content.put(REQUEST_URL_KEY, requestUriBuilder.build());

    converter.replaceEntity(response, content);

    return response;
  }
}
