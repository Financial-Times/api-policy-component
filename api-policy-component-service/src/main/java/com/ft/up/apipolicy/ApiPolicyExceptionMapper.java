package com.ft.up.apipolicy;

import com.ft.up.apipolicy.filters.FilterException;
import com.ft.up.apipolicy.resources.UnsupportedRequestException;
import com.ft.up.apipolicy.util.FluentLoggingWrapper;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.net.SocketTimeoutException;

import static com.ft.up.apipolicy.util.FluentLoggingWrapper.MESSAGE;
import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.slf4j.MDC.get;

/**
 * RuntimeExceptionMapper
 * <p>
 * Converts any RuntimeException including those that result in a response code of
 * - 415
 * - 500
 * - 503
 * to a sensible response with a human-readable error message
 * <p>
 * Some other exceptional scenarios like requests that result in a response code of
 * - 400
 * are not handled because Jersey constructs error responses.
 *
 * @author Simon.Gibbs
 */
@Provider
public class ApiPolicyExceptionMapper implements ExceptionMapper<Throwable> {

    public static final String GENERIC_MESSAGE = "server error";

    private FluentLoggingWrapper log;

    @Override
    public Response toResponse(Throwable throwable) {

        String message = "";
        // unless we know otherwise
        int status = SC_INTERNAL_SERVER_ERROR;
        message = GENERIC_MESSAGE;

        if (throwable instanceof NotFoundException) {
            message = "404 Not Found";

            return respondWith(SC_NOT_FOUND, message, throwable);
        }

        if (throwable instanceof WebApplicationException) {
            Response response = ((WebApplicationException) throwable).getResponse();

            // skip processing of responses that are already present.
            if (response.getEntity() != null) {
                return respondWith(response.getStatus(), response.getEntity().toString(), throwable);
            }

            // fill out null responses
            message = firstNonNull(throwable.getMessage(), GENERIC_MESSAGE);

            if (response.getStatus() < 500) {
                if (GENERIC_MESSAGE.equals(message)) { // if we didn't get a specific message from the exception
                    message = "client error";
                }
            }

            return respondWith(response.getStatus(), message, throwable);
        }

        if (throwable instanceof ProcessingException) {
            message = throwable.getMessage();
            if (throwable.getCause() instanceof SocketTimeoutException) {
                status = SC_GATEWAY_TIMEOUT;
            }
            return respondWith(status, message, throwable);
        }

        if (throwable instanceof UnsupportedRequestException) {
            message = throwable.getMessage();
            return respondWith(status, message, throwable);
        }

        if (throwable instanceof FilterException) {
            message = throwable.getMessage();
            return respondWith(status, message, throwable);
        }


        return respondWith(status, message, throwable);
    }

    private Response respondWith(int status, String reason, Throwable t) {
        String responseMessage = format("{\"message\":\"%s\"}", reason);
        Response response = Response.status(status).entity(responseMessage).type(APPLICATION_JSON_TYPE).build();
        logResponse(response, reason, t);

        return response;
    }

    private void logResponse(Response response, String reason, Throwable t) {
        log = new FluentLoggingWrapper()
                .withClassName(this.getClass().toString())
                .withMethodName("toResponse")
                .withTransactionId(get("transaction_id"))
                .withResponse(response)
                .withField(MESSAGE, reason)
                .withException(t);

        int status = response.getStatus();

        if (status == 404) {
            log.build().logDebug();
        } else if (400 <= status && status < 500) {
            log.build().logWarn();
        } else {
            log.build().logError();
        }

    }

}
