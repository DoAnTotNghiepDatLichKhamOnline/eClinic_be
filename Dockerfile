# syntax=docker/dockerfile:1
# Một Dockerfile cho cả 8 ứng dụng: stage build chạy Maven 1 lần, stage cuối chỉ có JRE 17 + jar của ARG MODULE.
# Build tay 1 image: docker build --build-arg MODULE=catalog-service -t eclinic-catalog .

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
# -DskipTests (không phải -Dmaven.test.skip) để vẫn build test-jar của common; test chạy bằng "mvn clean verify".
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre
# Tên thư mục module, ví dụ: catalog-service, api-gateway
ARG MODULE
RUN groupadd --system eclinic && useradd --system --gid eclinic --no-create-home eclinic
WORKDIR /app
# *.jar khớp file jar Spring Boot (không khớp *.jar.original)
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
USER eclinic
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
