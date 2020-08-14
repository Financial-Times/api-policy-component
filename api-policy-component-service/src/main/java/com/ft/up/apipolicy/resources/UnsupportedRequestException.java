package com.ft.up.apipolicy.resources;

public class UnsupportedRequestException extends RuntimeException {

  private String path;
  private String httpMethod;

  public UnsupportedRequestException(String path, String httpMethod) {
    super(String.format("Unsupported request: path [%s] with method [%s].", path, httpMethod));
    this.path = path;
    this.httpMethod = httpMethod;
  }

  public String getPath() {
    return path;
  }

  public String getHttpMethod() {
    return httpMethod;
  }
}
