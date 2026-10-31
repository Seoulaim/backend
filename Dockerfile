# 빌드: 멀티모듈 전체 복사 후 api bootJar 생성 (-plain.jar 제외)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew \
    && ./gradlew :api:bootJar --no-daemon \
    && cp "$(ls api/build/libs/*.jar | grep -v plain)" /workspace/app.jar

# 실행: JRE 만 포함, root 가 아닌 사용자로 실행
FROM eclipse-temurin:21-jre
WORKDIR /app
# 시간대 UTC 고정 → hibernate.jdbc.time_zone(UTC)과 일치
ENV TZ=UTC \
    JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -XX:MaxRAMPercentage=60"
RUN useradd --system --uid 1001 spring
COPY --from=build /workspace/app.jar app.jar
USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]