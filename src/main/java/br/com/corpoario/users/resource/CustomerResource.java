package br.com.corpoario.users.resource;

import br.com.corpoario.users.dto.CustomerRegistrationRequest;
import br.com.corpoario.users.dto.CustomerResponse;
import br.com.corpoario.users.entity.Account;
import br.com.corpoario.users.exception.ErrorResponse;
import br.com.corpoario.users.service.CustomerService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @Inject
    CustomerService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "201", description = "Customer account created with the CUSTOMER role",
        content = @Content(schema = @Schema(implementation = CustomerResponse.class)))
    @APIResponse(responseCode = "400", description = "Missing body, malformed JSON, or invalid email or password",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "An account with this email already exists",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response register(@NotNull(message = "Request body cannot be null") @Valid CustomerRegistrationRequest request) {
        Account account = service.register(request.email(), request.password());

        return Response.status(Response.Status.CREATED)
            .entity(toResponse(account))
            .build();
    }

    private CustomerResponse toResponse(Account account) {
        return new CustomerResponse(
            account.getId(),
            account.getEmail(),
            account.getRole(),
            account.getStatus(),
            account.getCreatedAt());
    }
}
