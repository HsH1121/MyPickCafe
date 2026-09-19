# MyPickCafe Spring Boot 웹 서버
#   - 빌드 컨텍스트는 MyPickCafe_Springboot/ 입니다:  docker build -t mypickcafe-web:dev .
#   - 설정은 이미지에 넣지 않고 실행할 때 환경변수로 주입합니다(application*.properties 의 ${...} 자리).
#
# 멀티스테이지: 빌드에는 JDK+Gradle 이 필요하지만, 실행에는 JRE 와 jar 하나면 됩니다.
# 최종 이미지에 소스·Gradle 배포판·의존성 캐시가 남지 않습니다.

# ── 1단계: 빌드 ──────────────────────────────────────────────────────────────
# 래퍼가 가리키는 배포판(8.14.3)을 그대로 쓰도록 gradlew 로 빌드합니다.
FROM eclipse-temurin:17-jdk AS build

WORKDIR /build

# 래퍼와 빌드 스크립트를 먼저 복사해, 소스만 바뀐 경우 의존성 내려받기 레이어를 재사용합니다.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet

COPY src ./src
# 테스트는 이미지 빌드에서 제외합니다. 테스트는 CI/로컬에서 돌리는 것이고,
# 여기서 돌리면 이미지 빌드가 그만큼 느려집니다.
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar app.jar

# ── 2단계: 실행 ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /build/app.jar ./app.jar

# 업로드 파일(file.upload-dir=./uploads)은 컨테이너 밖(볼륨)에 두는 지점입니다.
# 작업 디렉터리가 /app 이므로 상대경로 ./uploads 는 /app/uploads 로 풀립니다.
RUN mkdir -p /app/uploads \
    && useradd --create-home --uid 10001 appuser \
    && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 컨테이너 메모리 한도를 기준으로 힙을 잡습니다(-Xmx 고정값 대신).
# compose 에서 SPRING_JAVA_OPTS 로 덮어쓸 수 있습니다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"

# JAVA_OPTS 를 펼쳐야 해서 셸을 거치되, exec 로 교체해 PID 1 이 JVM 이 되게 합니다.
# (그래야 docker stop 의 SIGTERM 이 JVM 에 바로 전달돼 정상 종료됩니다.)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

# Actuator 가 없어 HTTP 상태 대신 포트 열림만 확인합니다.
# JRE 이미지에 curl·wget 이 없어 bash 의 /dev/tcp 를 씁니다.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD bash -c "exec 3<>/dev/tcp/127.0.0.1/8080" || exit 1
