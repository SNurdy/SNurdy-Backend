#!/bin/bash
# 배포와 롤백 스크립트가 함께 쓰는 설정과 함수

IMAGE_NAME="yo0oni/snurdy-app-prod"
SHARED_DIR="/app/shared"
COMPOSE_FILE="${SHARED_DIR}/docker-compose-prod.yml"
ACTIVE_SLOT_FILE="${SHARED_DIR}/active_slot"
LOCK_FILE="${SHARED_DIR}/deploy.lock"
UPSTREAM_FILE="/etc/nginx/conf.d/active_upstream.conf"
UPSTREAM_BACKUP_FILE="${UPSTREAM_FILE}.bak"

# actuator 는 Nginx 에서 외부 차단되므로 Nginx 경유 확인은 DB 를 거치지 않는 공개 API 로 한다
VERIFY_URL="https://127.0.0.1/api/v1/app/version/check?clientVersion=1.0.0&platform=IOS"

# 이전 Nginx worker 가 이전 슬롯과의 연결을 정리할 시간
DRAIN_SECONDS=5

acquire_deploy_lock() {
    exec 9>"$LOCK_FILE"
    if ! flock -n 9; then
        echo "다른 배포 또는 롤백이 진행 중입니다. 종료합니다."
        exit 1
    fi
}

slot_port() {
    if [ "$1" = "blue" ]; then echo 8080; else echo 8081; fi
}

other_slot() {
    if [ "$1" = "blue" ]; then echo "green"; else echo "blue"; fi
}

stop_slot() {
    docker compose -f "$COMPOSE_FILE" --profile "$1" stop "app-$1" 2>/dev/null || true
}

wait_for_health() {
    local port="$1"
    local status
    for i in $(seq 1 12); do
        status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}/api/actuator/health" 2>/dev/null || echo "000")
        if [ "$status" = "200" ]; then
            echo "  → 헬스체크 통과"
            return 0
        fi
        echo "  → 대기 중... (${i}/12)"
        sleep 5
    done
    echo "  → 헬스체크 실패"
    return 1
}

# 설정 파일과 실제 트래픽이 어긋난 채로 남지 않도록 검증 실패 시 이전 파일로 되돌린다
switch_upstream() {
    local port="$1"
    sudo cp "$UPSTREAM_FILE" "$UPSTREAM_BACKUP_FILE"
    sudo tee "$UPSTREAM_FILE" > /dev/null << EOF
upstream spring_backend {
    server 127.0.0.1:${port};
}
EOF
    if ! sudo nginx -t; then
        echo "  → Nginx 설정 검증 실패, 이전 설정으로 원복"
        sudo cp "$UPSTREAM_BACKUP_FILE" "$UPSTREAM_FILE"
        return 1
    fi
    if ! sudo nginx -s reload; then
        echo "  → Nginx reload 실패, 이전 설정으로 원복"
        sudo cp "$UPSTREAM_BACKUP_FILE" "$UPSTREAM_FILE"
        return 1
    fi
}

restore_upstream() {
    sudo cp "$UPSTREAM_BACKUP_FILE" "$UPSTREAM_FILE"
    sudo nginx -t && sudo nginx -s reload
}

verify_through_nginx() {
    local status
    sleep 2
    for i in $(seq 1 5); do
        status=$(curl -sk -o /dev/null -w "%{http_code}" "$VERIFY_URL" 2>/dev/null || echo "000")
        if [ "$status" = "200" ]; then
            echo "  → Nginx 경유 확인 통과"
            return 0
        fi
        echo "  → Nginx 경유 응답 ${status}, 재시도 (${i}/5)"
        sleep 2
    done
    echo "  → Nginx 경유 확인 실패"
    return 1
}
