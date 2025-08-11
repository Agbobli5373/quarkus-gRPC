# API Reference

This document provides comprehensive reference documentation for all gRPC and REST endpoints in the gRPC Learning Service.

## Table of Contents

1. [gRPC Service API](#grpc-service-api)
2. [REST API](#rest-api)
3. [Message Types](#message-types)
4. [Error Codes](#error-codes)
5. [Examples](#examples)

## gRPC Service API

### Service Definition

```protobuf
service UserService {
  // Unary RPC methods
  rpc CreateUser(CreateUserRequest) returns (User);
  rpc GetUser(GetUserRequest) returns (User);
  rpc UpdateUser(UpdateUserRequest) returns (User);
  rpc DeleteUser(DeleteUserRequest) returns (DeleteUserResponse);

  // Server streaming RPC
  rpc ListUsers(Empty) returns (stream User);

  // Client streaming RPC
  rpc CreateUsers(stream CreateUserRequest) returns (CreateUsersResponse);

  // Bidirectional streaming RPC
  rpc SubscribeToUserUpdates(stream SubscribeRequest) returns (stream UserNotification);
}
```

### Unary Methods

#### CreateUser

Creates a new user with the provided information.

**Request**: `CreateUserRequest`
**Response**: `User`

**gRPC Call**:

```bash
grpcurl -plaintext -d '{"name":"John Doe","email":"john@example.com"}' \
  localhost:9000 userservice.UserService/CreateUser
```

**Possible Errors**:

- `INVALID_ARGUMENT`: Invalid name or email format
- `ALREADY_EXISTS`: Email already exists
- `INTERNAL`: Server error

#### GetUser

Retrieves a user by their unique ID.

**Request**: `GetUserRequest`
**Response**: `User`

**gRPC Call**:

```bash
grpcurl -plaintext -d '{"id":"user-123"}' \
  localhost:9000 userservice.UserService/GetUser
```

**Possible Errors**:

- `NOT_FOUND`: User with specified ID not found
- `INVALID_ARGUMENT`: Invalid ID format
- `INTERNAL`: Server error

#### UpdateUser

Updates an existing user's information.

**Request**: `UpdateUserRequest`
**Response**: `User`

**gRPC Call**:

```bash
grpcurl -plaintext -d '{"id":"user-123","name":"John Smith","email":"john.smith@example.com"}' \
  localhost:9000 userservice.UserService/UpdateUser
```

**Possible Errors**:

- `NOT_FOUND`: User with specified ID not found
- `INVALID_ARGUMENT`: Invalid name or email format
- `ALREADY_EXISTS`: Email already exists for another user
- `INTERNAL`: Server error

#### DeleteUser

Deletes a user by their unique ID.

**Request**: `DeleteUserRequest`
**Response**: `DeleteUserResponse`

**gRPC Call**:

```bash
grpcurl -plaintext -d '{"id":"user-123"}' \
  localhost:9000 userservice.UserService/DeleteUser
```

**Possible Errors**:

- `NOT_FOUND`: User with specified ID not found
- `INVALID_ARGUMENT`: Invalid ID format
- `INTERNAL`: Server error

### Streaming Methods

#### ListUsers (Server Streaming)

Streams all users in the system. Each user is sent as a separate message in the stream.

**Request**: `Empty`
**Response**: `stream User`

**gRPC Call**:

```bash
grpcurl -plaintext -d '{}' \
  localhost:9000 userservice.UserService/ListUsers
```

**Behavior**:

- Streams each user individually
- Completes when all users have been sent
- If no users exist, completes immediately without sending any messages

**Possible Errors**:

- `INTERNAL`: Server error during streaming

#### CreateUsers (Client Streaming)

Accepts a stream of user creation requests and returns a summary of the operation.

**Request**: `stream CreateUserRequest`
**Response**: `CreateUsersResponse`

**Usage**: Use the provided client applications for streaming examples.

**Behavior**:

- Processes each request in the stream
- Continues processing even if some requests fail
- Returns summary with success count and error details

**Possible Errors**:

- `INTERNAL`: Server error during processing

#### SubscribeToUserUpdates (Bidirectional Streaming)

Establishes a bidirectional stream for real-time user update notifications.

**Request**: `stream SubscribeRequest`
**Response**: `stream UserNotification`

**Usage**: Use the `NotificationSubscriberClient` for examples.

**Behavior**:

- Client sends subscription requests
- Server sends notifications for user changes
- Connection remains open until client disconnects
- Supports multiple concurrent subscribers

**Possible Errors**:

- `CANCELLED`: Client cancelled the stream
- `INTERNAL`: Server error during streaming

## REST API

### Base URL

```
http://localhost:8080/api/users
```

### Endpoints

#### POST /api/users

Creates a new user.

**Request Body**:

```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

**Response** (201 Created):

```json
{
  "id": "user-123",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**cURL Example**:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```

#### GET /api/users/{id}

Retrieves a user by ID.

**Path Parameters**:

- `id` (string): User ID

**Response** (200 OK):

```json
{
  "id": "user-123",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**cURL Example**:

```bash
curl http://localhost:8080/api/users/user-123
```

#### PUT /api/users/{id}

Updates an existing user.

**Path Parameters**:

- `id` (string): User ID

**Request Body**:

```json
{
  "name": "John Smith",
  "email": "john.smith@example.com"
}
```

**Response** (200 OK):

```json
{
  "id": "user-123",
  "name": "John Smith",
  "email": "john.smith@example.com",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T11:45:00Z"
}
```

**cURL Example**:

```bash
curl -X PUT http://localhost:8080/api/users/user-123 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Smith","email":"john.smith@example.com"}'
```

#### DELETE /api/users/{id}

Deletes a user by ID.

**Path Parameters**:

- `id` (string): User ID

**Response** (200 OK):

```json
{
  "success": true,
  "message": "User deleted successfully"
}
```

**cURL Example**:

```bash
curl -X DELETE http://localhost:8080/api/users/user-123
```

#### GET /api/users

Lists all users.

**Response** (200 OK):

```json
[
  {
    "id": "user-123",
    "name": "John Doe",
    "email": "john@example.com",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  },
  {
    "id": "user-456",
    "name": "Jane Smith",
    "email": "jane@example.com",
    "createdAt": "2024-01-15T11:00:00Z",
    "updatedAt": "2024-01-15T11:00:00Z"
  }
]
```

**cURL Example**:

```bash
curl http://localhost:8080/api/users
```

## Message Types

### User

Represents a user entity.

```protobuf
message User {
  string id = 1;           // Unique user identifier
  string name = 2;         // User's full name
  string email = 3;        // User's email address
  int64 created_at = 4;    // Creation timestamp (Unix epoch)
  int64 updated_at = 5;    // Last update timestamp (Unix epoch)
}
```

**JSON Representation**:

```json
{
  "id": "user-123",
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

### CreateUserRequest

Request message for creating a new user.

```protobuf
message CreateUserRequest {
  string name = 1;   // User's full name (required)
  string email = 2;  // User's email address (required, must be unique)
}
```

**Validation Rules**:

- `name`: Required, non-blank, 2-50 characters
- `email`: Required, valid email format, unique across all users

### GetUserRequest

Request message for retrieving a user by ID.

```protobuf
message GetUserRequest {
  string id = 1;  // User ID (required)
}
```

### UpdateUserRequest

Request message for updating an existing user.

```protobuf
message UpdateUserRequest {
  string id = 1;     // User ID (required)
  string name = 2;   // New name (required)
  string email = 3;  // New email (required, must be unique)
}
```

**Validation Rules**:

- `id`: Required, must exist
- `name`: Required, non-blank, 2-50 characters
- `email`: Required, valid email format, unique across all users (excluding current user)

### DeleteUserRequest

Request message for deleting a user.

```protobuf
message DeleteUserRequest {
  string id = 1;  // User ID (required)
}
```

### DeleteUserResponse

Response message for delete operations.

```protobuf
message DeleteUserResponse {
  bool success = 1;    // Whether the deletion was successful
  string message = 2;  // Descriptive message
}
```

### CreateUsersResponse

Response message for batch user creation (client streaming).

```protobuf
message CreateUsersResponse {
  int32 created_count = 1;           // Number of successfully created users
  repeated string errors = 2;        // List of error messages for failed creations
  repeated string created_user_ids = 3;  // IDs of successfully created users
}
```

### UserNotification

Notification message for real-time user updates.

```protobuf
message UserNotification {
  enum NotificationType {
    CREATED = 0;  // User was created
    UPDATED = 1;  // User was updated
    DELETED = 2;  // User was deleted
  }

  NotificationType type = 1;  // Type of notification
  User user = 2;              // User data (null for DELETED)
  int64 timestamp = 3;        // Notification timestamp
  string message = 4;         // Descriptive message
}
```

### SubscribeRequest

Request message for subscribing to user updates.

```protobuf
message SubscribeRequest {
  string client_id = 1;  // Unique client identifier
  repeated UserNotification.NotificationType notification_types = 2;  // Types to subscribe to
}
```

### Empty

Empty message for operations that don't require parameters.

```protobuf
message Empty {}
```

## Error Codes

### gRPC Status Codes

| Code                | Description        | When Used                             |
| ------------------- | ------------------ | ------------------------------------- |
| `OK`                | Success            | Operation completed successfully      |
| `INVALID_ARGUMENT`  | Invalid input      | Bad request data, validation failures |
| `NOT_FOUND`         | Resource not found | User ID doesn't exist                 |
| `ALREADY_EXISTS`    | Resource exists    | Email already in use                  |
| `INTERNAL`          | Internal error     | Unexpected server errors              |
| `CANCELLED`         | Cancelled          | Client cancelled the request          |
| `DEADLINE_EXCEEDED` | Timeout            | Request took too long                 |

### HTTP Status Codes

| Code | Description           | When Used                   |
| ---- | --------------------- | --------------------------- |
| 200  | OK                    | Successful GET, PUT, DELETE |
| 201  | Created               | Successful POST             |
| 400  | Bad Request           | Invalid input data          |
| 404  | Not Found             | User not found              |
| 409  | Conflict              | Email already exists        |
| 500  | Internal Server Error | Unexpected server error     |

### Error Response Format

**REST Error Response**:

```json
{
  "error": "User not found",
  "message": "User with ID user-123 not found",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**gRPC Error Details**:

```
Status: NOT_FOUND
Message: User with ID user-123 not found
```

## Examples

### Complete User Lifecycle (gRPC)

```bash
# 1. Create a user
grpcurl -plaintext -d '{"name":"John Doe","email":"john@example.com"}' \
  localhost:9000 userservice.UserService/CreateUser

# Response: {"id":"user-123","name":"John Doe","email":"john@example.com","createdAt":"1705315800","updatedAt":"1705315800"}

# 2. Get the user
grpcurl -plaintext -d '{"id":"user-123"}' \
  localhost:9000 userservice.UserService/GetUser

# 3. Update the user
grpcurl -plaintext -d '{"id":"user-123","name":"John Smith","email":"john.smith@example.com"}' \
  localhost:9000 userservice.UserService/UpdateUser

# 4. List all users
grpcurl -plaintext -d '{}' \
  localhost:9000 userservice.UserService/ListUsers

# 5. Delete the user
grpcurl -plaintext -d '{"id":"user-123"}' \
  localhost:9000 userservice.UserService/DeleteUser
```

### Complete User Lifecycle (REST)

```bash
# 1. Create a user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'

# Response: {"id":"user-123","name":"John Doe","email":"john@example.com","createdAt":"2024-01-15T10:30:00Z","updatedAt":"2024-01-15T10:30:00Z"}

# 2. Get the user
curl http://localhost:8080/api/users/user-123

# 3. Update the user
curl -X PUT http://localhost:8080/api/users/user-123 \
  -H "Content-Type: application/json" \
  -d '{"name":"John Smith","email":"john.smith@example.com"}'

# 4. List all users
curl http://localhost:8080/api/users

# 5. Delete the user
curl -X DELETE http://localhost:8080/api/users/user-123
```

### Streaming Examples

#### Server Streaming (List Users)

```bash
# Stream all users
grpcurl -plaintext -d '{}' \
  localhost:9000 userservice.UserService/ListUsers

# Output (one message per user):
# {"id":"user-1","name":"User 1","email":"user1@example.com","createdAt":"1705315800","updatedAt":"1705315800"}
# {"id":"user-2","name":"User 2","email":"user2@example.com","createdAt":"1705315860","updatedAt":"1705315860"}
# {"id":"user-3","name":"User 3","email":"user3@example.com","createdAt":"1705315920","updatedAt":"1705315920"}
```

#### Client Streaming (Batch Create)

Use the provided `BatchUserCreationClient` Java application:

```bash
./mvnw exec:java -Dexec.mainClass="org.isaac.client.BatchUserCreationClient"
```

#### Bidirectional Streaming (Notifications)

Use the provided `NotificationSubscriberClient` Java application:

```bash
./mvnw exec:java -Dexec.mainClass="org.isaac.client.NotificationSubscriberClient"
```

### Error Handling Examples

#### Invalid Input (gRPC)

```bash
# Missing required field
grpcurl -plaintext -d '{"name":""}' \
  localhost:9000 userservice.UserService/CreateUser

# Response: Error: rpc error: code = InvalidArgument desc = Name is required and cannot be blank
```

#### User Not Found (gRPC)

```bash
# Non-existent user ID
grpcurl -plaintext -d '{"id":"non-existent"}' \
  localhost:9000 userservice.UserService/GetUser

# Response: Error: rpc error: code = NotFound desc = User with ID non-existent not found
```

#### Duplicate Email (REST)

```bash
# Try to create user with existing email
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Another User","email":"existing@example.com"}'

# Response: HTTP 409 Conflict
# {"error":"Email already exists","message":"User with email existing@example.com already exists"}
```

## Service Discovery

### gRPC Reflection

The service supports gRPC reflection in development mode:

```bash
# List all services
grpcurl -plaintext localhost:9000 list

# Describe the UserService
grpcurl -plaintext localhost:9000 describe userservice.UserService

# Describe a message type
grpcurl -plaintext localhost:9000 describe userservice.User
```

### Health Checks

```bash
# Check service health
curl http://localhost:8080/q/health

# Check liveness
curl http://localhost:8080/q/health/live

# Check readiness
curl http://localhost:8080/q/health/ready
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/q/metrics

# Application-specific metrics
curl http://localhost:8080/q/metrics/application
```

This API reference provides comprehensive documentation for all endpoints, message types, and usage patterns in the gRPC Learning Service.
