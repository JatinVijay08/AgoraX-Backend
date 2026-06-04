# Discussion Forum Backend

A robust and scalable REST API backend for a discussion forum application built with Spring Boot, PostgreSQL, and Redis. It provides comprehensive endpoints for user authentication, posting, commenting, and voting, while leveraging Redis for caching and Cloudinary for media management.

## 🚀 Features

- **User Authentication & Authorization**: Secure JWT-based authentication and Google OAuth2 integration.
- **User Management**: Profile management and user interactions.
- **Posts & Discussions**: Create, read, update, and delete discussion posts.
- **Commenting System**: Nested comments on posts for threaded discussions.
- **Voting System**: Upvote and downvote posts and comments.
- **Media Uploads**: Seamless image and file uploads using Cloudinary.
- **Caching**: Redis-backed caching for high-performance data retrieval.

## 🛠️ Tech Stack

- **Framework**: Spring Boot
- **Language**: Java 17
- **Database**: PostgreSQL (via Spring Data JPA)
- **Caching**: Redis
- **Authentication**: Spring Security, JWT, OAuth2
- **Media Storage**: Cloudinary
- **Build Tool**: Maven

## 📋 Prerequisites

Ensure you have the following installed on your local machine:
- [Java 17](https://adoptium.net/) or higher
- [PostgreSQL](https://www.postgresql.org/)
- [Redis](https://redis.io/) server running locally or remotely

## ⚙️ Environment Variables

The application relies on several environment variables. You can set them in your system or use an IDE configuration. The variables correspond to the properties in `src/main/resources/application.properties`:

```properties
# Server Port (Defaults to 8080)
PORT=8080

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/forum_db
DATABASE_USERNAME=your_db_username
DATABASE_PASSWORD=your_db_password

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password

# Google OAuth2
GOOGLE_CLIENT_ID=your_google_client_id

# Cloudinary Configuration
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

## 🚀 Running the Application

1. **Navigate to the project directory**:
   ```bash
   cd discussion-forum-backend
   ```

2. **Run the application using the Maven wrapper**:
   
   On Windows:
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```
   
   On macOS/Linux:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access the API**:
   The server will start at `http://localhost:8080` by default. You can check the health endpoint at `http://localhost:8080/actuator/health`.

## 📦 Building for Production

To build a production-ready standalone JAR file, run:

On Windows:
```cmd
.\mvnw.cmd clean package -DskipTests
```

On macOS/Linux:
```bash
./mvnw clean package -DskipTests
```

The compiled JAR will be available in the `target/` directory and can be executed using:
```bash
java -jar target/discussion-forum-backend-0.0.1-SNAPSHOT.jar
```
