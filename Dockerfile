# 1) 베이스 이미지
FROM ubuntu:22.04

# 2) 필수 패키지, JRE 설치
RUN apt-get update \
 && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    openjdk-17-jre-headless \
    ca-certificates \
    wget \
    unzip \
    xvfb \
    chromium-browser \
    chromium-chromedriver \
    libgtk-3-0 \
    libx11-xcb1 \
    libxcomposite1 \
    libxrandr2 \
    libasound2 \
    libpangocairo-1.0-0 \
 && rm -rf /var/lib/apt/lists/*

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