FROM maven:3-openjdk-17 AS build

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built WAR file
COPY --from=build /app/target/coupon-scraper-0.0.1-SNAPSHOT.war coupon-scraper.war

# Create non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

EXPOSE 8080

# Use exec form for better signal handling
ENTRYPOINT ["java", "-jar", "coupon-scraper.war"]

# Build command: docker build -t coupon-scraper .
# Run command: docker run -p 8080:8080 coupon-scraper