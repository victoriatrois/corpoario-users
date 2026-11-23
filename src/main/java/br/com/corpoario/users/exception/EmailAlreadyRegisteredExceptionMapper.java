package br.com.corpoario.users.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class EmailAlreadyRegisteredExceptionMapper implements ExceptionMapper<EmailAlreadyRegisteredException> {

    @Override
    public Response toResponse(EmailAlreadyRegisteredException exception) {
        ErrorResponse body = new ErrorResponse(
            409,
            "Email already registered",
            exception.getMessage()
        );
        return Response.status(Response.Status.CONFLICT)
            .entity(body)
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
