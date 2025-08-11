# REST Client Examples

This document provides comprehensive examples of how to interact with the gRPC Learning Service REST endpoints using curl commands and other HTTP clients.

## Prerequisites

- The gRPC Learning Service should be running on `http://localhost:8080`
- curl should be installed on your system
- jq (optional) for pretty-printing JSON responses

## Base URL

All REST endpoints are available at: `http://localhost:8080/api/users`

## User Management Operations

### 1. Create a User

Create a new user by sending a POST request with user details.

```bash
# Basic user creation
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john.doe@example.com"
  }'

# With pretty-printed response (requires jq)
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Smith",
    "email": "jane.smith@example.com"
  }' | jq '.'

# Expected Response:
# {
#   "id": "generated-uuid",
#   "name": "John Doe",
#   "email": "john.doe@example.com",
#   "createdAt": "2024-01-15T10:30:00Z",
#   "updatedAt": "2024-01-15T10:30:00Z"
# }
```

### 2. Get a User by ID

Retrieve a specific user by their ID.

```bash
# Replace {user-id} with actual user ID from creation response
curl http://localhost:8080/api/users/{user-id}

# Example with actual ID
curl http://localhost:8080/api/users/123e4567-e89b-12d3-a456-426614174000

# With pretty-printed response
curl http://localhost:8080/api/users/{user-id} | jq '.'

# Expected Response:
# {
#   "id": "123e4567-e89b-12d3-a456-426614174000",
#   "name": "John Doe",
#   "email": "john.doe@example.com",
#   "createdAt": "2024-01-15T10:30:00Z",
#   "updatedAt": "2024-01-15T10:30:00Z"
# }
```

### 3. Update a User

Update an existing user's information.

```bash
# Update user details
curl -X PUT http://localhost:8080/api/users/{user-id} \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Smith",
    "email": "john.smith@example.com"
  }'

# Example with actual ID
curl -X PUT http://localhost:8080/api/users/123e4567-e89b-12d3-a456-426614174000 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Smith",
    "email": "john.smith@example.com"
  }' | jq '.'

# Expected Response:
# {
#   "id": "123e4567-e89b-12d3-a456-426614174000",
#   "name": "John Smith",
#   "email": "john.smith@example.com",
#   "createdAt": "2024-01-15T10:30:00Z",
#   "updatedAt": "2024-01-15T10:35:00Z"
# }
```

### 4. Delete a User

Delete a user by their ID.

```bash
# Delete user
curl -X DELETE http://localhost:8080/api/users/{user-id}

# Example with actual ID
curl -X DELETE http://localhost:8080/api/users/123e4567-e89b-12d3-a456-426614174000

# Expected Response:
# {
#   "success": true,
#   "message": "User deleted successfully"
# }
```

### 5. List All Users

Retrieve all users in the system.

```bash
# List all users
curl http://localhost:8080/api/users

# With pretty-printed response
curl http://localhost:8080/api/users | jq '.'

# Expected Response:
# [
#   {
#     "id": "123e4567-e89b-12d3-a456-426614174000",
#     "name": "John Smith",
#     "email": "john.smith@example.com",
#     "createdAt": "2024-01-15T10:30:00Z",
#     "updatedAt": "2024-01-15T10:35:00Z"
#   },
#   {
#     "id": "987fcdeb-51a2-43d1-9f4e-123456789abc",
#     "name": "Jane Doe",
#     "email": "jane.doe@example.com",
#     "createdAt": "2024-01-15T10:32:00Z",
#     "updatedAt": "2024-01-15T10:32:00Z"
#   }
# ]
```

## Complete Workflow Examples

### Example 1: Complete User Lifecycle

```bash
#!/bin/bash

echo "=== Complete User Lifecycle Demo ==="

# 1. Create a user
echo "1. Creating user..."
CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo User",
    "email": "demo@example.com"
  }')

echo "Created user: $CREATE_RESPONSE"

# Extract user ID (requires jq)
USER_ID=$(echo $CREATE_RESPONSE | jq -r '.id')
echo "User ID: $USER_ID"

# 2. Get the user
echo -e "\n2. Retrieving user..."
curl -s http://localhost:8080/api/users/$USER_ID | jq '.'

# 3. Update the user
echo -e "\n3. Updating user..."
curl -s -X PUT http://localhost:8080/api/users/$USER_ID \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Demo User",
    "email": "updated.demo@example.com"
  }' | jq '.'

# 4. List all users
echo -e "\n4. Listing all users..."
curl -s http://localhost:8080/api/users | jq '.'

# 5. Delete the user
echo -e "\n5. Deleting user..."
curl -s -X DELETE http://localhost:8080/api/users/$USER_ID | jq '.'

echo -e "\n=== Demo Complete ==="
```

### Example 2: Batch User Creation

```bash
#!/bin/bash

echo "=== Batch User Creation Demo ==="

# Create multiple users
USERS=(
  '{"name": "Alice Johnson", "email": "alice@company.com"}'
  '{"name": "Bob Smith", "email": "bob@company.com"}'
  '{"name": "Carol Williams", "email": "carol@company.com"}'
  '{"name": "David Brown", "email": "david@company.com"}'
)

USER_IDS=()

for user_data in "${USERS[@]}"; do
  echo "Creating user: $user_data"
  response=$(curl -s -X POST http://localhost:8080/api/users \
    -H "Content-Type: application/json" \
    -d "$user_data")

  user_id=$(echo $response | jq -r '.id')
  USER_IDS+=($user_id)
  echo "Created user with ID: $user_id"
done

echo -e "\nAll created user IDs: ${USER_IDS[@]}"

# List all users to verify
echo -e "\nListing all users:"
curl -s http://localhost:8080/api/users | jq '.'
```

## Error Handling Examples

### 1. Invalid User Data

```bash
# Missing required fields
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": ""
  }'

# Expected Response (400 Bad Request):
# {
#   "error": "Validation failed",
#   "message": "Name is required",
#   "timestamp": "2024-01-15T10:40:00Z"
# }
```

### 2. Invalid Email Format

```bash
# Invalid email format
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "not-an-email"
  }'

# Expected Response (400 Bad Request):
# {
#   "error": "Validation failed",
#   "message": "Valid email is required",
#   "timestamp": "2024-01-15T10:40:00Z"
# }
```

### 3. User Not Found

```bash
# Try to get non-existent user
curl http://localhost:8080/api/users/non-existent-id

# Expected Response (404 Not Found):
# {
#   "error": "User not found",
#   "message": "User with ID 'non-existent-id' not found",
#   "timestamp": "2024-01-15T10:40:00Z"
# }
```

### 4. Duplicate Email

```bash
# Create user with duplicate email
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Another User",
    "email": "existing@example.com"
  }'

# Expected Response (409 Conflict):
# {
#   "error": "Duplicate email",
#   "message": "User with email 'existing@example.com' already exists",
#   "timestamp": "2024-01-15T10:40:00Z"
# }
```

## Performance Testing Examples

### 1. Load Testing with curl

```bash
#!/bin/bash

echo "=== Load Testing Demo ==="

# Create 100 users concurrently
for i in {1..100}; do
  (
    curl -s -X POST http://localhost:8080/api/users \
      -H "Content-Type: application/json" \
      -d "{
        \"name\": \"Load Test User $i\",
        \"email\": \"loadtest$i@example.com\"
      }" > /dev/null
    echo "Created user $i"
  ) &
done

wait
echo "All users created"

# Count total users
total_users=$(curl -s http://localhost:8080/api/users | jq '. | length')
echo "Total users in system: $total_users"
```

### 2. Response Time Testing

```bash
#!/bin/bash

echo "=== Response Time Testing ==="

# Test response times for different operations
operations=(
  "POST http://localhost:8080/api/users"
  "GET http://localhost:8080/api/users"
  "GET http://localhost:8080/api/users/123"
)

for operation in "${operations[@]}"; do
  echo "Testing: $operation"

  # Use curl's timing options
  curl -w "Time: %{time_total}s, Status: %{http_code}\n" \
       -s -o /dev/null \
       -X GET http://localhost:8080/api/users
done
```

## Integration with Other Tools

### 1. Using with HTTPie

```bash
# Install HTTPie: pip install httpie

# Create user
http POST localhost:8080/api/users name="HTTPie User" email="httpie@example.com"

# Get user
http GET localhost:8080/api/users/{user-id}

# Update user
http PUT localhost:8080/api/users/{user-id} name="Updated HTTPie User" email="updated.httpie@example.com"

# Delete user
http DELETE localhost:8080/api/users/{user-id}

# List users
http GET localhost:8080/api/users
```

### 2. Using with Postman

Import the following collection into Postman:

```json
{
  "info": {
    "name": "gRPC Learning Service REST API",
    "description": "Collection for testing the REST endpoints"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    }
  ],
  "item": [
    {
      "name": "Create User",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"{{$randomFullName}}\",\n  \"email\": \"{{$randomEmail}}\"\n}"
        },
        "url": {
          "raw": "{{baseUrl}}/api/users",
          "host": ["{{baseUrl}}"],
          "path": ["api", "users"]
        }
      }
    }
  ]
}
```

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure the service is running on port 8080
2. **404 Not Found**: Check the endpoint URL and HTTP method
3. **400 Bad Request**: Verify JSON format and required fields
4. **500 Internal Server Error**: Check server logs for detailed error information

### Debugging Tips

```bash
# Enable verbose output
curl -v http://localhost:8080/api/users

# Show response headers
curl -I http://localhost:8080/api/users

# Save response to file for analysis
curl http://localhost:8080/api/users -o response.json

# Test connectivity
curl -f http://localhost:8080/health || echo "Service not available"
```

This completes the REST client examples documentation. Use these examples to interact with the gRPC Learning Service REST endpoints and understand how the REST layer bridges to the underlying gRPC services.
