package org.isaac.rest.user;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.isaac.dto.CreateUserDto;
import org.isaac.dto.ErrorResponse;
import org.isaac.dto.UpdateUserDto;
import org.isaac.dto.UserDto;
import org.isaac.grpc.user.*;
import org.isaac.grpc.user.UserProto.CreateUserRequest;
import org.isaac.grpc.user.UserProto.DeleteUserRequest;
import org.isaac.grpc.user.UserProto.GetUserRequest;
import org.isaac.grpc.user.UserProto.UpdateUserRequest;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;

/**
 * REST controller that bridges REST API calls to gRPC service calls.
 * Demonstrates how to integrate gRPC services with traditional REST endpoints
 * using Quarkus reactive capabilities.
 */
@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserRestController {

    private static final Logger LOG = Logger.getLogger(UserRestController.class);

    @GrpcClient
    UserService userGrpcClient;


    /**
     * Creates a new user via gRPC service call.
     * 
     * @param createUserDto the user data to create
     * @return created user as DTO
     */
    @POST
    public Uni<Response> createUser(@Valid CreateUserDto createUserDto) {
        LOG.debugf("REST: Creating user with name=%s, email=%s", createUserDto.getName(), createUserDto.getEmail());

        CreateUserRequest grpcRequest = CreateUserRequest.newBuilder()
                .setName(createUserDto.getName())
                .setEmail(createUserDto.getEmail())
                .build();

        return userGrpcClient.createUser(grpcRequest)
                .onItem().transform(this::toUserDto)
                .onItem().transform(userDto -> {
                    LOG.debugf("REST: Successfully created user with id=%s", userDto.getId());
                    return Response.status(Response.Status.CREATED).entity(userDto).build();
                })
                .onFailure().transform(this::mapGrpcErrorToWebApplicationException);
    }

    /**
     * Retrieves a user by ID via gRPC service call.
     * 
     * @param id the user ID
     * @return user DTO or 404 if not found
     */
    @GET
    @Path("/{id}")
    public Uni<UserDto> getUser(@PathParam("id") String id) {
        LOG.debugf("REST: Getting user with id=%s", id);

        GetUserRequest grpcRequest = GetUserRequest.newBuilder()
                .setId(id)
                .build();

        return userGrpcClient.getUser(grpcRequest)
                .onItem().transform(this::toUserDto)
                .onItem().invoke(userDto -> LOG.debugf("REST: Successfully retrieved user with id=%s", userDto.getId()))
                .onFailure().transform(this::mapGrpcErrorToWebApplicationException);
    }

    /**
     * Updates an existing user via gRPC service call.
     * 
     * @param id            the user ID to update
     * @param updateUserDto the updated user data
     * @return updated user DTO
     */
    @PUT
    @Path("/{id}")
    public Uni<UserDto> updateUser(@PathParam("id") String id, @Valid UpdateUserDto updateUserDto) {
        LOG.debugf("REST: Updating user with id=%s, name=%s, email=%s", id, updateUserDto.getName(),
                updateUserDto.getEmail());

        UpdateUserRequest grpcRequest = UpdateUserRequest.newBuilder()
                .setId(id)
                .setName(updateUserDto.getName())
                .setEmail(updateUserDto.getEmail())
                .build();

        return userGrpcClient.updateUser(grpcRequest)
                .onItem().transform(this::toUserDto)
                .onItem().invoke(userDto -> LOG.debugf("REST: Successfully updated user with id=%s", userDto.getId()))
                .onFailure().transform(this::mapGrpcErrorToWebApplicationException);
    }

    /**
     * Deletes a user via gRPC service call.
     * 
     * @param id the user ID to delete
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{id}")
    public Uni<Response> deleteUser(@PathParam("id") String id) {
        LOG.debugf("REST: Deleting user with id=%s", id);

        DeleteUserRequest grpcRequest = DeleteUserRequest.newBuilder()
                .setId(id)
                .build();

        return userGrpcClient.deleteUser(grpcRequest)
                .onItem().transform(deleteResponse -> {
                    LOG.debugf("REST: Successfully deleted user with id=%s", id);
                    return Response.noContent().build();
                })
                .onFailure().transform(this::mapGrpcErrorToWebApplicationException);
    }

    /**
     * Lists all users by converting gRPC server streaming to REST collection.
     * This demonstrates how to handle streaming operations in REST endpoints.
     * 
     * @return list of all users
     */
    @GET
    public Uni<List<UserDto>> listUsers() {
        LOG.debug("REST: Listing all users");

        UserProto.Empty grpcRequest = UserProto.Empty.newBuilder().build();

        return userGrpcClient.listUsers(grpcRequest)
                .onItem().transform(this::toUserDto)
                .collect().asList()
                .onItem().invoke(users -> LOG.debugf("REST: Successfully retrieved %d users", users.size()))
                .onFailure().transform(this::mapGrpcErrorToWebApplicationException);
    }

    /**
     * Converts gRPC User message to REST UserDto.
     * Handles timestamp conversion from long to Instant.
     * 
     * @param grpcUser the gRPC User message
     * @return UserDto for REST response
     */
    private UserDto toUserDto(UserProto.User grpcUser) {
        return new UserDto(
                grpcUser.getId(),
                grpcUser.getName(),
                grpcUser.getEmail(),
                Instant.ofEpochMilli(grpcUser.getCreatedAt()),
                Instant.ofEpochMilli(grpcUser.getUpdatedAt()));
    }

    /**
     * Maps gRPC errors to appropriate WebApplicationException for REST responses.
     * This ensures proper HTTP status codes are returned to REST clients.
     * 
     * @param throwable the gRPC error
     * @return WebApplicationException with appropriate HTTP status
     */
    private WebApplicationException mapGrpcErrorToWebApplicationException(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException statusException) {
            Status.Code code = statusException.getStatus().getCode();
            String message = statusException.getStatus().getDescription();

            LOG.warnf("REST: gRPC error occurred - code=%s, message=%s", code, message);

            ErrorResponse errorResponse = switch (code) {
                case NOT_FOUND -> new ErrorResponse("User not found",
                        message != null ? message : "The requested user was not found");
                case INVALID_ARGUMENT -> new ErrorResponse("Invalid request",
                        message != null ? message : "The request contains invalid data");
                case ALREADY_EXISTS -> new ErrorResponse("User already exists",
                        message != null ? message : "A user with this email already exists");
                case PERMISSION_DENIED -> new ErrorResponse("Permission denied",
                        message != null ? message : "You don't have permission to perform this action");
                default -> {
                    LOG.errorf(throwable, "REST: Unexpected gRPC error - code=%s", code);
                    yield new ErrorResponse("Internal server error", "An unexpected error occurred");
                }
            };

            Response response = switch (code) {
                case NOT_FOUND -> Response.status(Response.Status.NOT_FOUND).entity(errorResponse).build();
                case INVALID_ARGUMENT -> Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
                case ALREADY_EXISTS -> Response.status(Response.Status.CONFLICT).entity(errorResponse).build();
                case PERMISSION_DENIED -> Response.status(Response.Status.FORBIDDEN).entity(errorResponse).build();
                default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
            };

            return new WebApplicationException(response);
        }

        LOG.errorf(throwable, "REST: Unexpected non-gRPC error occurred");
        ErrorResponse errorResponse = new ErrorResponse("Internal server error", "An unexpected error occurred");
        Response response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        return new WebApplicationException(response);
    }
}