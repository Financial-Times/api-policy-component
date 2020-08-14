package com.ft.up.apipolicy;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.Before;
import org.junit.ClassRule;

public abstract class AbstractApiComponentTest {
  static final int SOME_PORT;

  static {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      SOME_PORT = socket.getLocalPort();
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  static final String primaryNodes = String.format("localhost:%d:%d", SOME_PORT, SOME_PORT);

  @ClassRule public static WireMockClassRule WIRE_MOCK_1 = new WireMockClassRule(SOME_PORT);

  static String resourceFilePath(String resourceClassPathLocation) {

    File file = null;

    try {

      file = new File(Resources.getResource(resourceClassPathLocation).toURI());
      return file.getAbsolutePath();

    } catch (Exception e) {
      if (file != null) {
        throw new RuntimeException(file.toString(), e);
      }
      throw new RuntimeException(e);
    }
  }

  @Before
  public void setUp() {
    WIRE_MOCK_1.resetMappings();
    WIRE_MOCK_1.resetRequests();
    WIRE_MOCK_1.resetScenarios();
  }
}
