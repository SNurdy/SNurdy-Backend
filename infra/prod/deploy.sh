#!/bin/bash
set -e

source "$(dirname "$0")/common.sh"

IMAGE_TAG="${1:?이미지 태그를 인자로 전달해주세요}"
IMAGE_FULL="${IMAGE_NAME}:${IMAGE_TAG}"

acquire_deploy_lock

CURRENT_SLOT=$(cat "$ACTIVE_SLOT_FILE")
TARGET_SLOT=$(other_slot "$CURRENT_SLOT")
TARGET_PORT=$(slot_port "$TARGET_SLOT")

echo "=== 현재: $CURRENT_SLOT → 배포 대상: $TARGET_SLOT ==="

echo "[1/7] 이미지 Pull..."
docker pull "$IMAGE_FULL"

echo "[2/7] Redis 확인..."
docker compose -f "$COMPOSE_FILE" up -d redis-prod

echo "[3/7] ${TARGET_SLOT} 슬롯 시작..."
stop_slot "$TARGET_SLOT"
docker compose -f "$COMPOSE_FILE" --profile "$TARGET_SLOT" rm -f "app-${TARGET_SLOT}" 2>/dev/null || true
IMAGE="$IMAGE_FULL" docker compose -f "$COMPOSE_FILE" --profile "$TARGET_SLOT" up -d

echo "[4/7] 헬스체크 중..."
if ! wait_for_health "$TARGET_PORT"; then
    echo "  → 배포 중단. 트래픽은 ${CURRENT_SLOT} 그대로"
    stop_slot "$TARGET_SLOT"
    exit 1
fi

echo "[5/7] Nginx 트래픽 전환..."
if ! switch_upstream "$TARGET_PORT"; then
    echo "  → 배포 중단. 트래픽은 ${CURRENT_SLOT} 그대로"
    stop_slot "$TARGET_SLOT"
    exit 1
fi

# 이전 슬롯을 내리기 전에 확인해야 실패 시 upstream 만 되돌려 즉시 복구할 수 있다
echo "[6/7] Nginx 경유 확인..."
if ! verify_through_nginx; then
    echo "  → ${CURRENT_SLOT} 로 되돌립니다"
    restore_upstream
    stop_slot "$TARGET_SLOT"
    exit 1
fi

echo "[7/7] 정리 중..."
echo "$TARGET_SLOT" > "$ACTIVE_SLOT_FILE"
sleep "$DRAIN_SECONDS"
stop_slot "$CURRENT_SLOT"
docker image prune -f

echo "=== 배포 완료: ${TARGET_SLOT} (port ${TARGET_PORT}) ==="
echo "    문제 시 롤백: bash ${SHARED_DIR}/rollback.sh"
