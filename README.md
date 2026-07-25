# AgoraX - Backend API

The robust, high-performance Spring Boot backend powering **AgoraX** (formerly Discussion Forum). It provides a secure, cached, and real-time API for the digital public square.

## 🚀 Tech Stack

- **Framework**: Java 17, Spring Boot 3
- **Database**: PostgreSQL (Primary Data), Redis (Feed Caching & Fast Lookups)
- **Authentication**: JWT (JSON Web Tokens) & Google OAuth2
- **Media Storage**: Cloudinary integration for image uploads
- **Real-Time**: Spring WebSocket for instant notifications
- **Build Tool**: Maven

## ✨ Core Features

- **Authentication & Security**: Custom JWT implementation with in-memory dynamic secret generation. Google OAuth2 ID Token verification.
- **Complex Feed Algorithms**: Reddit-style ranking algorithms for `Hot` and `Trending` feeds, powered by upvote/downvote momentum and time decay.
- **High-Performance Caching**: Redis implementation for heavily requested queries (like the main feeds) to ensure sub-millisecond response times.
- **Nested Discussions**: Recursive, infinite-depth nested comment threads.
- **Real-Time Notifications**: WebSocket integration pushes events to clients instantly when their posts are interacted with.

## 🛠️ Local Development Setup

### Prerequisites
- Java 17 or higher
- Maven
- PostgreSQL running locally or in Docker
- Redis running locally or in Docker

### Environment Variables
Create an `application-local.properties` file or inject these directly into your environment before running the server:

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/agorax
DATABASE_USERNAME=your_db_username
DATABASE_PASSWORD=your_db_password

GOOGLE_CLIENT_ID=your_google_oauth_client_id.apps.googleusercontent.com

CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
PORT=8080
```

### Running the Server
1. Clone the repository and navigate to the backend directory.
2. Install dependencies and compile:
   ```bash
   mvn clean install
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
The API will be available at `http://localhost:8080`.

## 🔒 Security Note
This application utilizes dynamically generated, cryptographically secure 256-bit JWT secrets (`Keys.secretKeyFor`) on startup. While highly secure against repository leaks, restarting the server will invalidate all active user sessions.
