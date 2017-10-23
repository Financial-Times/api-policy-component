package com.ft.up.apipolicy;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.ClassRule;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public abstract class AbstractApiComponentTest {
    static final int SOME_PORT_1;
    static final int SOME_PORT_2;
    
    static {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            SOME_PORT_1 = socket.getLocalPort();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            SOME_PORT_2 = socket.getLocalPort();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    static final String primaryNodes = String.format("localhost:%d:%d, localhost:%d:%d",
            SOME_PORT_1, SOME_PORT_1,
            SOME_PORT_2, SOME_PORT_2);

    @ClassRule
    public static WireMockClassRule WIRE_MOCK_1 = new WireMockClassRule(SOME_PORT_1);

    @ClassRule
    public static WireMockClassRule WIRE_MOCK_2 = new WireMockClassRule(SOME_PORT_2);

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
        WIRE_MOCK_2.resetMappings();
        WIRE_MOCK_2.resetRequests();
        WIRE_MOCK_2.resetScenarios();
    }
}
