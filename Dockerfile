# 1) 베이스 이미지 - OpenJDK 17 Alpine으로 최적화
FROM openjdk:17-jre-alpine

# 2) 시스템 패키지 업데이트 및 필수 패키지 설치
RUN apk update && apk add --no-cache \
    curl \
    wget \
    chromium \
    chromium-chromedriver \
    xvfb \
    dbus \
    fontconfig \
    ttf-dejavu \
    && rm -rf /var/cache/apk/*

# 3) 비root 사용자 생성 (보안 강화)
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D -s /bin/sh appuser

# 4) 작업 디렉터리 설정
WORKDIR /app

# 5) 애플리케이션 소유자 변경
RUN chown -R appuser:appgroup /app

# 6) 사용자 전환
USER appuser

# 7) JAR 파일 복사 (빌드 컨텍스트에서)
COPY --chown=appuser:appgroup build/libs/*.jar app.jar

# 8) 환경변수 설정
ENV TZ=Asia/Seoul \
    PAGE_LOAD_WAIT_MS=2000 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200" \
    SPRING_PROFILES_ACTIVE=prod

## 9) 헬스체크 설정
#HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
#    CMD curl -f http://localhost:18080/actuator/health || exit 1

# 10) 포트 노출
EXPOSE 18080

# 11) 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]