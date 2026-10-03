# syntax=docker/dockerfile:1
#
# Một Dockerfile dùng chung cho cả 8 ứng dụng (7 service + api-gateway).
# - Stage "build": chạy Maven MỘT lần cho toàn bộ project. Stage này không dùng ARG MODULE,
#   nên docker compose build 8 image vẫn chỉ chạy Maven một lần (các image sau dùng lại cache).
# - Stage cuối: chỉ có JRE 17 + file jar của module được chọn qua ARG MODULE, chạy bằng user thường.
#
# Thường chạy qua docker compose:   docker compose --profile app up -d --build
# Build tay một image:              docker build --build-arg MODULE=catalog-service -t eclinic-catalog .
# Lần build đầu tải toàn bộ thư viện Maven (vài phút, tuỳ tốc độ mạng); các lần sau dùng cache /root/.m2.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
# -DskipTests (không phải -Dmaven.test.skip): vẫn build test-jar của common mà các service cần.
# Test chạy riêng bằng "mvn clean verify" (xem README / Carry-forward).
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
