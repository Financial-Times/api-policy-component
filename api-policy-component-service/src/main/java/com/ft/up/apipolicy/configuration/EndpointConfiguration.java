package com.ft.up.apipolicy.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import io.dropwizard.util.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointConfiguration {
  private static final Pattern URL_REGEX =
      Pattern.compile("(https?:\\/\\/)?([^:]+)(:\\d+)?(:\\d+)?");

  private final Optional<String> shortName;
  private final String path;
  private String protocol;
  private String host;
  private int port;
  private int adminPort;
  private Duration connectionTimeout;
  private Duration timeout;

  @JsonProperty private boolean retryNonIdempotentMethods;

  public EndpointConfiguration(
      @JsonProperty("shortName") Optional<String> shortName,
      @JsonProperty("path") Optional<String> path,
      @JsonProperty("primaryNodes") String primaryNodesRaw,
      @JsonProperty("timeout") Duration readTimeout,
      @JsonProperty("connectionTimeout") Duration connectionTimeout) {
    this.shortName = shortName;
    this.path = path.or("/");
    extractNodes(primaryNodesRaw);
    this.connectionTimeout = connectionTimeout;
    this.timeout = readTimeout;
  }

  public void setRetryNonIdempotentMethods(final boolean retryNonIdempotentMethods) {
    this.retryNonIdempotentMethods = retryNonIdempotentMethods;
  }

  public Optional<String> getShortName() {
    return shortName;
  }

  @JsonIgnore
  public String getProtocol() {
    return protocol;
  }

  @JsonIgnore
  public String getHost() {
    return host;
  }

  @JsonIgnore
  public int getAdminPort() {
    return adminPort;
  }

  @JsonIgnore
  public int getPort() {
    return port;
  }

  public String getPath() {
    return path;
  }

  public boolean isRetryNonIdempotentMethods() {
    return retryNonIdempotentMethods;
  }

  protected MoreObjects.ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this)
        .add("shortName", shortName)
        .add("protocol", protocol)
        .add("host", host)
        .add("port", port)
        .add("adminPort", adminPort)
        .add("retryNonIdempotentMethods", retryNonIdempotentMethods);
  }

  @Override
  public String toString() {
    return toStringHelper().toString();
  }

  private void extractNodes(String rawNodes) {
    if (Strings.isNullOrEmpty(rawNodes)) {
      return;
    }

    Matcher matcher = URL_REGEX.matcher(rawNodes.trim());
    if (matcher.matches()) {
      String endpointProtocol = matcher.group(1);
      if (endpointProtocol != null) {
        endpointProtocol =
            endpointProtocol.substring(0, endpointProtocol.length() - 3); // must end with "://"
        protocol = endpointProtocol;
      } else {
        protocol = "http"; // default
      }

      String host = matcher.group(2);
      String endpointAppPort = matcher.group(3);
      int port =
          (endpointAppPort == null)
              ? defaultPortFor(protocol)
              : Integer.parseInt(endpointAppPort.substring(1));

      String endpointAdminPort = matcher.group(4);
      int adminPort =
          (endpointAdminPort == null) ? port : Integer.parseInt(endpointAdminPort.substring(1));

      this.host = host;
      this.port = port;
      this.adminPort = adminPort;
    } else {
      throw new IllegalArgumentException(
          String.format("`%s` is not a valid endpoint value.", rawNodes));
    }
  }

  private int defaultPortFor(String protocol) {
    return "https".equals(protocol) ? 443 : 80;
  }

  public int getReadTimeoutMillis() {
    return (int) timeout.toMilliseconds();
  }

  public int getConnectTimeoutMillis() {
    return (int) connectionTimeout.toMilliseconds();
  }
}
