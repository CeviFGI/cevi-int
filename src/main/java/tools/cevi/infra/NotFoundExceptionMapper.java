package tools.cevi.infra;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// Note: it is not possible to integrate this in the AppExceptionMapper in a general way
// if this is done, the built-in ExceptionMapper that targets the NotFoundException directly
// is called, see here for more information: https://github.com/quarkusio/quarkus/issues/7883
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Inject
    Template error404;

    /**
     * The media type is set on the response, not by {@code @Produces}: an exception mapper is not
     * a resource method, so the annotation is never read and the answer went out without a
     * Content-Type at all — which a browser renders as the page's own source (NFR-013 in effect,
     * and visible to anyone who mistypes an address).
     */
    @Override
    public Response toResponse(NotFoundException exception) {
        String errorId = UUID.randomUUID().toString();
        Log.warn("HTTPStatus[404], errorId[" + errorId + "], Message[" + exception.getMessage() + "]");
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.TEXT_HTML_TYPE)
                .entity(error404.render())
                .build();
    }
}
