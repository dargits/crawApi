FROM maven:3-openjdk-17 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn clean package -DskipTests

#Run stage

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/coupon-scraper-0.0.1-SNAPSHOT.war coupon-scraper.war

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "coupon-scraper.war"]