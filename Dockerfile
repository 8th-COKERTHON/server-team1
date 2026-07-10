# ---------- 1단계: 빌드 ----------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 의존성 목록만 먼저 복사해서 받아둔다.
# src 가 바뀌어도 이 레이어는 캐시에 남아 재빌드가 빨라진다.
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src src
# 테스트는 CI(.github/workflows/ci.yml)에서 이미 돌았으므로 이미지 빌드에서는 건너뛴다.
RUN ./gradlew --no-daemon bootJar -x test

# ---------- 2단계: 실행 ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# root 로 돌리지 않는다.
RUN addgroup -S app && adduser -S -G app app
USER app

COPY --from=builder --chown=app:app /app/build/libs/*.jar app.jar

EXPOSE 8080

# DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET 등은 실행 시점에 -e 로 주입한다.
# 비밀 값을 이미지에 굽지 않는다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
