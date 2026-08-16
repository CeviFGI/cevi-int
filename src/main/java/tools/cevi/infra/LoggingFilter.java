package tools.cevi.infra;

import io.vertx.core.http.HttpServerRequest;
import org.jboss.logging.Logger;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LoggingFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @Override
    public void filter(ContainerRequestContext context) {

        final String method = context.getMethod();
        final String path = info.getPath();

        LOG.infof("Request %s %s", method, path);
        // An IP address is personal data. It stays useful for tracing a single incident, but a
        // production system running at INFO should not accumulate a visitor movement profile.
        if (LOG.isDebugEnabled()) {
            LOG.debugf("Request %s %s from IP %s", method, path, request.remoteAddress());
        }
    }
}