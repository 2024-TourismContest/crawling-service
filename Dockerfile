# 1) 베이스 이미지를 Eclipse Temurin으로 변경
FROM eclipse-temurin:17-jre-alpine

# 2) 필수 패키지 설치 (apk 사용)
RUN apk update && apk add --no-cache \
    wget \
    unzip \
    xvfb \
    chromium \
    chromium-chromedriver \
    ttf-freefont \
    dbus \
 && rm -rf /var/cache/apk/*

# 3) 작업 디렉터리
WORKDIR /app

# 4) JAR 복사
COPY build/libs/*.jar app.jar

# 5) 환경변수
ENV TZ=Asia/Seoul \
    PAGE_LOAD_WAIT_MS=2000

# 6) 애플리케이션 포트
EXPOSE 18080

# 7) 실행
ENTRYPOINT ["java", "-jar", "app.jar"]