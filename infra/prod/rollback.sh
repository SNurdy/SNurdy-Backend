#!/bin/bash
set -e

source "$(dirname "$0")/common.sh"

acquire_deploy_lock

CURRENT_SLOT=$(cat "$ACTIVE_SLOT_FILE")
PREVIOUS_SLOT=$(other_slot "$CURRENT_SLOT")
PREVIOUS_PORT=$(slot_port "$PREVIOUS_SLOT")
PREVIOUS_CONTAINER="snurdy-app-${PREVIOUS_SLOT}"

# 배포는 이전 슬롯을 rm 하지 않고 stop 만 하므로 직전 버전 컨테이너가 남아 있다
if ! docker inspect "$PREVIOUS_CONTAINER" > /dev/null 2>&1; then
    echo "${PREVIOUS_CONTAINER} 컨테이너가 없습니다. 이전 SHA 로 deploy.sh 를 실행하세요."
    exit 1
fi

CURRENT_IMAGE=$(docker inspect -f '{{.Config.Image}}' "snurdy-app-${CURRENT_SLOT}" 2>/dev/null || echo "unknown")
PREVIOUS_IMAGE=$(docker inspect -f '{{.Config.Image}}' "$PREVIOUS_CONTAINER")

echo "=== 롤백: ${CURRENT_SLOT} (${CURRENT_IMAGE}) → ${PREVIOUS_SLOT} (${PREVIOUS_IMAGE}) ==="

echo "[1/5] ${PREVIOUS_SLOT} 슬롯 시작..."
docker start "$PREVIOUS_CONTAINER"

echo "[2/5] 헬스체크 중..."
if ! wait_for_health "$PREVIOUS_PORT"; then
    echo "  → 롤백 중단. 트래픽은 ${CURRENT_SLOT} 그대로"
    stop_slot "$PREVIOUS_SLOT"
    exit 1
fi

echo "[3/5] Nginx 트래픽 전환..."
if ! switch_upstream "$PREVIOUS_PORT"; then
    echo "  → 롤백 중단. 트래픽은 ${CURRENT_SLOT} 그대로"
    stop_slot "$PREVIOUS_SLOT"
    exit 1
fi

echo "[4/5] Nginx 경유 확인..."
if ! verify_through_nginx; then
    echo "  → ${CURRENT_SLOT} 로 되돌립니다"
    restore_upstream
    stop_slot "$PREVIOUS_SLOT"
    exit 1
fi

echo "[5/5] 정리 중..."
echo "$PREVIOUS_SLOT" > "$ACTIVE_SLOT_FILE"
sleep "$DRAIN_SECONDS"
stop_slot "$CURRENT_SLOT"

echo "=== 롤백 완료: ${PREVIOUS_SLOT} (port ${PREVIOUS_PORT}) ==="
