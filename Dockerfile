# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Gradle wrapper + build config first so dependency layer is cached
# separately from source changes (Docker layer cache keyed on these files).
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./

RUN chmod +x ./gradlew \
    && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

# Source changes invalidate only the layers from here down.
COPY src ./src

RUN ./gradlew bootJar --no-daemon -x test

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre AS runtime

RUN groupadd --system app && useradd --system --gid app --no-create-home app

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

RUN chown app:app app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -XX:MaxRAMPercentage=70.0 -XX:InitialRAMPercentage=50.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
