# 1) 베이스 이미지로 Ubuntu 22.04 사용
FROM ubuntu:22.04

# 2) 필수 패키지, JRE, Chromium과 Chromedriver 설치
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

# 3) 작업 디렉터리 설정
WORKDIR /app

COPY build/libs/*.jar app.jar

# 5) 컨테이너 내 chromedriver 경로 설정 (optional)
#    -- 코드에서 System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver"); 로 지정했다면 필요 없음
ENV TZ=Asia/Seoul \
    PAGE_LOAD_WAIT_MS=2000

# 6) (필요 시) 애플리케이션 포트 오픈
EXPOSE 8080

# 7) 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]