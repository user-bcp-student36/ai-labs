package com.bcp.workshop.auth;

import com.bcp.workshop.auth.service.AuthException;
import com.bcp.workshop.auth.service.AuthService;
import com.bcp.workshop.auth.service.AuthenticatedUser;
import com.bcp.workshop.auth.service.Session;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class);
    private static final String LEGACY_FLOW_MARKER = "authenticated-with-legacy-flow";
    private static final String UNEXPECTED_ERROR = "Unexpected error";

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        if (request == null) {
            return error(Response.Status.BAD_REQUEST, "Request is null");
        }
        try {
            return Response.ok(toResponse(authService.login(request.username, request.password))).build();
        } catch (AuthException rejected) {
            return error(rejected.status(), rejected.getMessage());
        } catch (RuntimeException failure) {
            LOG.error("Login failed unexpectedly", failure);
            return error(Response.Status.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR);
        }
    }

    private static LoginResponse toResponse(Session session) {
        AuthenticatedUser user = session.user();
        return new LoginResponse(
                session.token(),
                session.expiresIn(),
                new LoginResponse.UserView(user.username(), HtmlEscaper.escape(user.displayName()), user.role()),
                LEGACY_FLOW_MARKER);
    }

    private static Response error(Response.Status status, String message) {
        return Response.status(status).entity(Map.of("message", message)).build();
    }
}
