package tools.cevi.infra;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class AppExceptionMapper implements ExceptionMapper<Exception> {
    @Inject
    Template error500;

    @Context
    private UriInfo uriInfo;

    @Override
    @Produces(MediaType.TEXT_HTML)
    public Response toResponse(Exception exception) {
        String errorId = UUID.randomUUID().toString();

        // A rejected request (too large a body, a malformed form) is the caller's problem, not a
        // server fault. Reporting it as 500 would hide exactly the request limits that are there to
        // keep an anonymous caller from filling the database.
        if (exception instanceof WebApplicationException webApplicationException
                && webApplicationException.getResponse().getStatus() < 500) {
            int status = webApplicationException.getResponse().getStatus();
            Log.warnf("HTTPStatus[%d], errorId[%s], Url[%s], Message[%s]",
                    status, errorId, uriInfo.getRequestUri(), exception.toString());
            return Response.status(status).entity(error500.data("errorId", errorId)).build();
        }

        Log.error("HTTPStatus[500], errorId[" + errorId + "], Url[" + uriInfo.getRequestUri() + "], Message[" + exception + "], Stack Trace ", exception);
        return Response.status(500).entity(error500.data("errorId", errorId)).build();
    }
}
