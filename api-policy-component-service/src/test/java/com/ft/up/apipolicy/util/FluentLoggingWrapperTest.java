package com.ft.up.apipolicy.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Arrays;

import static com.ft.up.apipolicy.util.FluentLoggingWrapper.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class FluentLoggingWrapperTest {
    private FluentLoggingWrapper log;

    @Before
    public void setup() {
        log = new FluentLoggingWrapper();
        log.withClassName(this.getClass().toString())
                .withMethodName("testOperation");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void logVerifyCommonFieldsArePresent() {
        final Appender mockAppender = getAppender();

        log.withTransactionId("tid_test1")
                .withField(MESSAGE, "Message test 1")
                .withField(METHOD, "GET")
                .withField(UUID, "7398d82a-6e76-11dd-a80a-0000779fd18c")
                .build()
                .logDebug();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                LoggingEvent loggingEvent = (LoggingEvent) argument;
                String content = loggingEvent.getFormattedMessage();
                return containsBasicJSONFields(content)
                        && containsFieldInJSON("\"uuid\":\"7398d82a-6e76-11dd-a80a-0000779fd18c\"", content)
                        && containsFieldInJSON("\"transaction_id\":\"tid_test1\"", content)
                        && containsFieldInJSON("\"logLevel\":\"DEBUG\"", content)
                        && containsFieldInJSON("\"method\":\"GET\"", content)
                        && containsFieldInJSON("\"msg\":\"Message test 1\"", content);
            }
        }));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void logVerifyAllOutboundFieldsArePresent() throws Exception {
        final Appender mockAppender = getAppender();

        MultivaluedMap<String, Object> headerMap = new MultivaluedHashMap<>();
        headerMap.put("content-type", Arrays.asList("application/json", "application/xml"));

        Response resp = mock(Response.class);
        when(resp.getStatus()).thenReturn(200);
        when(resp.getHeaders()).thenReturn(headerMap);

        java.net.URI fakeURI = new URI("http://localhost:8080/content");

        log.withResponse(resp)
                .withUri(fakeURI)
                .build()
                .logDebug();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                LoggingEvent loggingEvent = (LoggingEvent) argument;
                String content = loggingEvent.getFormattedMessage();
                return containsBasicJSONFields(content)
                        && containsFieldInJSON("\"uri\":\"http://localhost:8080/content\"", content)
                        && containsFieldInJSON("\"path\":\"/content\"", content)
                        && containsFieldInJSON("\"logLevel\":\"DEBUG\"", content)
                        && containsFieldInJSON("\"userAgent\":\"" + APPLICATION_NAME + " (+" + RUNBOOK_URI + ")\"", content)
                        && containsFieldInJSON("\"accept\":\"application/json\"", content)
                        && containsFieldInJSON("\"content-type\":\"application/json;application/xml\"", content)
                        && containsFieldInJSON("\"status\":" + "\"200\"", content);
            }
        }));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void logVerifyAllInboundFieldsArePresent() throws Exception {
        final Appender mockAppender = getAppender();

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getProtocol()).thenReturn("HTTP/1.1");
        when(req.getLocalAddr()).thenReturn("test host");
        when(req.getMethod()).thenReturn("GET");
        when(req.getRemoteAddr()).thenReturn("test client");

        java.net.URI fakeURI = new URI("http://localhost:8080/content");
        UriInfo mockUI = mock(UriInfo.class);
        when(mockUI.getAbsolutePath()).thenReturn(fakeURI);
        when(mockUI.getPath()).thenReturn(fakeURI.getPath());

        HttpHeaders mockHttpHeaders = mock(HttpHeaders.class);
        when(mockHttpHeaders.getHeaderString(mockHttpHeaders.ACCEPT)).thenReturn(APPLICATION_JSON_TYPE.toString());
        when(mockHttpHeaders.getHeaderString(mockHttpHeaders.USER_AGENT)).thenReturn("test user-agent");

        log.withRequest(req)
                .withUriInfo(mockUI)
                .withInboundHeaders(mockHttpHeaders)
                .build()
                .logDebug();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                LoggingEvent loggingEvent = (LoggingEvent) argument;
                String content = loggingEvent.getFormattedMessage();
                return containsBasicJSONFields(content)
                        && containsFieldInJSON("\"uri\":\"http://localhost:8080/content\"", content)
                        && containsFieldInJSON("\"path\":\"/content\"", content)
                        && containsFieldInJSON("\"logLevel\":\"DEBUG\"", content)
                        && containsFieldInJSON("\"userAgent\":\"test user-agent\"", content)
                        && containsFieldInJSON("\"client\":\"test client\"", content)
                        && containsFieldInJSON("\"host\":\"test host\"", content)
                        && containsFieldInJSON("\"protocol\":\"HTTP/1.1\"", content)
                        && containsFieldInJSON("\"method\":\"GET\"", content)
                        && containsFieldInJSON("\"accept\":\"application/json\"", content);
            }
        }));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void logVerifyAllExceptionFieldsArePresent() {
        final Appender mockAppender = getAppender();

        Exception ex = new NullPointerException("null pointer");

        log.withException(ex)
                .build()
                .logDebug();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                LoggingEvent loggingEvent = (LoggingEvent) argument;
                String content = loggingEvent.getFormattedMessage();
                return containsBasicJSONFields(content)
                        && containsFieldInJSON("\"exception_message\"", content)
                        && containsFieldInJSON("\"stacktrace_log\"", content);
            }
        }));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void logVerifyUnwantedFieldsAreNotPresent() {
        final Appender mockAppender = getAppender();

        log.withTransactionId(null)
                .withField(MESSAGE, "")
                .withField(UUID, null)
                .withException(null)
                .build().logDebug();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                LoggingEvent loggingEvent = (LoggingEvent) argument;
                String content = loggingEvent.getFormattedMessage();
                return containsBasicJSONFields(content)
                        && containsFieldInJSON("\"logLevel\":\"DEBUG\"", content)
                        && containsFieldInJSON("\"exception_message\":\"Exception was null\"", content)
                        && !containsFieldInJSON("\"transaction_id\":\"tid_test1\"", content)
                        && !containsFieldInJSON("\"msg\":\"test message 1\"", content)
                        && !containsFieldInJSON("\"uuid\":\"7398d82a-6e76-11dd-a80a-0000779fd18c\"", content)
                        && !containsFieldInJSON("\"method\":\"GET\"", content)
                        && !containsFieldInJSON("\"uri\":\"http://localhost:8080/content\"", content)
                        && !containsFieldInJSON("\"path\":\"/content\"", content)
                        && !containsFieldInJSON("\"userAgent\":\"" + APPLICATION_NAME + " (+" + RUNBOOK_URI + ")\"", content)
                        && !containsFieldInJSON("\"accept\":\"application/json\"", content)
                        && !containsFieldInJSON("\"content-type\":\"application/json\"", content)
                        && !containsFieldInJSON("\"status\":" + "\"200\"", content)
                        && !containsFieldInJSON("\"client\":\"test client\"", content)
                        && !containsFieldInJSON("\"host\":\"test host\"", content)
                        && !containsFieldInJSON("\"protocol\":\"HTTP/1.1\"", content)
                        && !containsFieldInJSON("\"stacktrace_log\"", content);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private Appender<ILoggingEvent> getAppender() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender<ILoggingEvent> mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        return mockAppender;
    }

    private boolean containsFieldInJSON(String field, String content) {
        return content.contains(field);
    }

    private boolean containsBasicJSONFields(String content) {
        return content.contains("\"systemcode\":\"api-policy-component\"")
                && content.contains("\"class\":\"class com.ft.up.apipolicy.util.FluentLoggingWrapperTest\"")
                && content.contains("\"operation\":\"testOperation\"");
    }
}
