package tools.cevi.infra;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Produces;
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

    @Override
    @Produces(MediaType.TEXT_HTML)
    public Response toResponse(NotFoundException exception) {
        String errorId = UUID.randomUUID().toString();
        Log.warn("HTTPStatus[404], errorId[" + errorId + "], Message[" + exception.getMessage() + "]");
        return Response.status(Response.Status.NOT_FOUND).entity(error404.render()).build();
    }
}
