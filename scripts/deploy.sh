#!/bin/bash

# =================================================================
# 1. 변수 설정
# =================================================================
COMPOSE_FILE="/home/mschoi/crawling-service/_work/crawling-service/crawling-service/docker-compose.yml"
NGINX_INC_FILE="/home/mschoi/crawling-service/_work/crawling-service/crawling-service/nginx/service-url.inc"
IMAGE_NAME="mschoi6641/crawling-service"
NEW_TAG=$1 # GitHub Actions 워크플로우로부터 전달받을 새 이미지 태그 (첫 번째 인자)

# 스크립트 실행 중 오류 발생 시 즉시 중단
set -e

# =================================================================
# 2. 현재 활성화된(Active) 서비스 확인
# =================================================================
CURRENT_COLOR=$(grep -o 'blue\|green' $NGINX_INC_FILE)
echo "### 현재 활성화된(Active) 서비스: ${CURRENT_COLOR} ###"

# =================================================================
# 3. 배포할 비활성(Inactive) 서비스 결정
# =================================================================
if [ "$CURRENT_COLOR" == "blue" ]; then
  INACTIVE_COLOR="green"
else
  INACTIVE_COLOR="blue"
fi
echo "### 새 버전을 배포할 서비스: ${INACTIVE_COLOR} ###"

# =================================================================
# 4. 새 버전의 컨테이너 실행
# =================================================================
# docker-compose.yml에서 사용할 이미지 태그 환경변수 설정
export DOCKER_IMAGE_TAG_BLUE=$( [ "$INACTIVE_COLOR" = "blue" ] && echo "$NEW_TAG" || echo "current" )
export DOCKER_IMAGE_TAG_GREEN=$( [ "$INACTIVE_COLOR" = "green" ] && echo "$NEW_TAG" || echo "current" )

# Docker Hub에서 새 버전의 이미지 받아오기
docker pull ${IMAGE_NAME}:${NEW_TAG}

# 비활성 서비스 컨테이너 실행 (의존성 관계 무시하고 해당 서비스만)
docker-compose -f $COMPOSE_FILE up -d --no-deps backend-${INACTIVE_COLOR}
echo "### backend-${INACTIVE_COLOR} 컨테이너 실행 완료 ###"
sleep 10

# =================================================================
# 5. 새 컨테이너 상태 확인 (Health Check)
# =================================================================
echo "### Health Check 시작 (최대 60초) ###"
HEALTH_CHECK_SUCCESS=false
for i in {1..6}; do
  # 새로 띄운 컨테이너의 IP 주소 가져오기
  CONTAINER_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' backend-${INACTIVE_COLOR})

  # 컨테이너 IP와 18080포트로 직접 health check 요청
  HEALTH_STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://${CONTAINER_IP}:18080/actuator/health)

  if [ "$HEALTH_STATUS_CODE" -eq 200 ]; then
    echo "### Health Check 성공! (상태 코드: 200) ###"
    HEALTH_CHECK_SUCCESS=true
    break
  else
    echo "### Health Check 실패... (상태 코드: ${HEALTH_STATUS_CODE}) 10초 후 재시도... ###"
    sleep 10
  fi
done

# =================================================================
# 6. 배포 실패 시 롤백 및 종료
# =================================================================
if [ "$HEALTH_CHECK_SUCCESS" = false ]; then
  echo "!!! 배포 실패: Health Check 타임아웃 !!!"
  docker-compose -f $COMPOSE_FILE stop backend-${INACTIVE_COLOR}
  exit 1
fi

# =================================================================
# 7. Nginx 트래픽 전환
# =================================================================
echo "### Nginx 트래픽을 ${INACTIVE_COLOR}으로 전환합니다. ###"
# service-url.inc 파일의 내용을 새 버전의 서비스 주소로 덮어쓰기
echo "server backend-${INACTIVE_COLOR}:18080;" > $NGINX_INC_FILE

# Nginx 설정을 다시 불러와서 적용 (무중단 리로드)
docker-compose -f $COMPOSE_FILE exec proxy nginx -s reload
echo "### 트래픽 전환 완료 ###"

# =================================================================
# 8. 이전 버전 컨테이너 중지
# =================================================================
echo "### 이전 버전 컨테이너(${CURRENT_COLOR})를 중지합니다. ###"
docker-compose -f $COMPOSE_FILE stop backend-${CURRENT_COLOR}

echo "🎉🎉🎉 새 버전(${INACTIVE_COLOR}) 배포가 성공적으로 완료되었습니다. 🎉🎉🎉"