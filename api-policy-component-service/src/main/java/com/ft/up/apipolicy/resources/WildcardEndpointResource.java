package com.ft.up.apipolicy.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/{path:.*}")
public class WildcardEndpointResource {

  private RequestHandler requestHandler;
  private RequestHandler nonIdempotentRequestHandler;

  public WildcardEndpointResource(
      RequestHandler wildcardRequestHandler, RequestHandler nonIdempotentRequestHandler) {
    this.requestHandler = wildcardRequestHandler;
    this.nonIdempotentRequestHandler = nonIdempotentRequestHandler;
  }

  @GET
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.WILDCARD)
  public Response get(@Context final HttpServletRequest request, @Context final UriInfo uriInfo) {
    return requestHandler.handleRequest(request, uriInfo);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
  public final Response post(
      @Context final HttpServletRequest request, @Context final UriInfo uriInfo) {
    return nonIdempotentRequestHandler.handleRequest(request, uriInfo);
  }
}
