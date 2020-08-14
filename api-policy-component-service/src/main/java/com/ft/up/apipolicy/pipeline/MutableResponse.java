package com.ft.up.apipolicy.pipeline;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * MutableResponse
 *
 * @author Simon.Gibbs
 */
public class MutableResponse {

  public static final String VARY_HEADER = "Vary";

  @SuppressWarnings("EI_EXPOSE_REP2")
  private byte[] entity;

  private MultivaluedMap<String, Object> headers;
  private int status;

  public MutableResponse() {
    headers = new MultivaluedHashMap<>();
  }

  public MutableResponse(MultivaluedMap<String, Object> headers, byte[] entity) {
    this.headers = new MultivaluedHashMap(headers);
    this.entity = entity;
  }

  public byte[] getEntity() {
    return entity;
  }

  public String getEntityAsString() {
    return new String(entity);
  }

  public MultivaluedMap<String, Object> getHeaders() {
    return headers;
  }

  public void setEntity(byte[] bytes) {
    this.entity = bytes;
  }

  public void setHeaders(MultivaluedMap<String, Object> headers) {
    this.headers = headers;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getContentType() {
    return String.valueOf(headers.getFirst("Content-Type"));
  }

  public Set<String> getHeadersInVaryList() {
    Set<String> varyByHeadersSet = new LinkedHashSet<>();
    if (headers.containsKey(VARY_HEADER)) {
      List<Object> headerListsFromVaryHeaders = headers.get(VARY_HEADER);
      for (Object headerListFromAVaryHeader : headerListsFromVaryHeaders) {
        String[] varyByHeaders = String.valueOf(headerListFromAVaryHeader).split("[, ]");
        varyByHeadersSet.addAll(Arrays.asList(varyByHeaders));
      }
    }
    return varyByHeadersSet;
  }
}
