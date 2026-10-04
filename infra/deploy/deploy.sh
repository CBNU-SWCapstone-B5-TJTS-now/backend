#!/usr/bin/env bash
# 운영 서버(~/backend)에서 blue-green 방식으로 app을 무중단 교체한다.
#
#   1. Nginx가 바라보지 않는 쪽(비활성 색)에 새 이미지를 띄운다
#   2. 새 쪽 헬스체크 → 실패하면 새 쪽만 지우고 끝 (트래픽은 기존 쪽 그대로)
#   3. Nginx upstream을 새 쪽 포트로 바꾸고 reload (기존 연결은 끊지 않음)
#   4. Nginx를 거쳐 새 쪽이 응답하는지 확인 → 실패하면 upstream 원복
#   5. 기존 쪽을 graceful하게 종료
#
# 사용법: infra/deploy/deploy.sh <이미지 태그>   (예: sha-c34166b)
# GHCR 로그인은 호출하는 쪽(GitHub Actions)에서 미리 해 둔다.
#
# 종료 코드 (0 외에는 모두 기존 버전이 계속 서비스 중)
#   0  새 이미지로 전환 성공
#   1  사전 단계 실패 (이미지 pull 실패, Nginx 설정 없음 등) — 아무것도 바뀌지 않음
#   2  새 이미지 헬스체크 실패 — 새 쪽 제거, 트래픽 전환 안 함
#   3  트래픽 전환 실패 — upstream 원복, 새 쪽 제거
set -euo pipefail

REPO="ghcr.io/cbnu-swcapstone-b5-tjts-now/backend"
STATE_DIR="$HOME/.nowhere-deploy"          # 레포 밖에 두어 git pull과 섞이지 않게 한다
UPSTREAM_FILE="/etc/nginx/snippets/nowhere-upstream.conf"
PUBLIC_HOST="api.nowhere-app.cloud"
LEGACY_CONTAINER="nowhere-app"             # blue-green 이전의 단일 app 컨테이너
DRAIN_SECONDS=5                            # 전환 후 기존 쪽의 처리 중 요청이 끝나길 기다리는 시간
COMPOSE=(docker compose -f docker-compose.prod.yml)

TAG="${1:?사용법: deploy.sh <이미지 태그>}"
NEW_IMAGE="$REPO:$TAG"

cd "$(dirname "$0")/../.."
mkdir -p "$STATE_DIR"

log() { echo "[deploy] $*"; }
# GitHub Actions에서 실행될 때는 실행 화면 상단에 오류로 표시한다.
alert() {
  if [[ -n "${GITHUB_ACTIONS:-}" ]]; then echo "::error::$*"; else log "$*"; fi
}

port_of() { [[ "$1" == blue ]] && echo 8080 || echo 8081; }

running() { [[ "$(docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null)" == true ]]; }

# Nginx upstream snippet에 적힌 포트로 현재 활성 색을 판단한다.
active_color() {
  local port
  port="$(grep -oE '127\.0\.0\.1:[0-9]+' "$UPSTREAM_FILE" 2>/dev/null | head -1 | cut -d: -f2)"
  case "$port" in
    8080) echo blue ;;
    8081) echo green ;;
    *) return 1 ;;
  esac
}

write_upstream() {
  printf '%s\n' \
    "# 배포 스크립트(infra/deploy/deploy.sh)가 관리한다. 직접 수정하지 말 것." \
    "# blue = 8080, green = 8081" \
    "server 127.0.0.1:$1;" | sudo tee "$UPSTREAM_FILE" >/dev/null
  sudo nginx -t -q && sudo systemctl reload nginx
}

# 직전 이미지 기록: 마지막 성공 기록 → 없으면 지금 서비스 중인 컨테이너의 이미지 digest
current_image() {
  if [[ -s "$STATE_DIR/current_image" ]]; then
    cat "$STATE_DIR/current_image"
    return
  fi
  local id
  id="$(docker inspect -f '{{.Image}}' "$1" 2>/dev/null)" || return 0
  docker image inspect -f '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$id" 2>/dev/null || true
}

# t3.micro는 기동이 느려서 5초 간격으로 최대 30번(약 2분 30초) 확인한다.
wait_healthy() {
  local url="$1"; shift
  for i in $(seq 1 30); do
    if curl -fsS -o /dev/null "$@" "$url"; then
      log "healthy (attempt $i): $url"
      return 0
    fi
    sleep 5
  done
  return 1
}

# Nginx(HTTPS)를 거쳐 응답하는지 서버 안에서 확인한다.
public_healthy() {
  for i in $(seq 1 6); do
    if curl -fsS -o /dev/null --resolve "$PUBLIC_HOST:443:127.0.0.1" "https://$PUBLIC_HOST/actuator/health"; then
      log "Nginx 경유 healthy (attempt $i)"
      return 0
    fi
    sleep 2
  done
  return 1
}

remove_app() {
  "${COMPOSE[@]}" rm -sf "app-$1" >/dev/null 2>&1 || true
}

# 현재/직전 이미지만 남기고 이 레포의 나머지 이미지는 지운다 (디스크 관리).
cleanup_images() {
  local keep_current="$1" keep_prev="${2:-}"
  docker images "$REPO" --format '{{.Repository}}:{{.Tag}}' \
    | grep -v ':<none>$' \
    | grep -vxF -e "$keep_current" -e "${keep_prev:-__none__}" \
    | xargs -r docker rmi >/dev/null 2>&1 || true
  docker image prune -f >/dev/null
}

# ---------- 사전 확인 (실패해도 아무것도 바뀌지 않음) ----------

if ! ACTIVE="$(active_color)"; then
  alert "Nginx upstream 설정($UPSTREAM_FILE)을 읽을 수 없음. infra/nginx/README.md 참고"
  exit 1
fi
NEW="$([[ "$ACTIVE" == blue ]] && echo green || echo blue)"
NEW_PORT="$(port_of "$NEW")"
ACTIVE_PORT="$(port_of "$ACTIVE")"

# 지금 서비스 중인 컨테이너 (blue-green 도입 전에는 nowhere-app 하나였다)
OLD_CONTAINER=""
if running "nowhere-app-$ACTIVE"; then
  OLD_CONTAINER="nowhere-app-$ACTIVE"
elif [[ "$ACTIVE" == blue ]] && running "$LEGACY_CONTAINER"; then
  OLD_CONTAINER="$LEGACY_CONTAINER"
fi

PREV_IMAGE="$(current_image "$OLD_CONTAINER")"
log "활성: $ACTIVE(:$ACTIVE_PORT) ${OLD_CONTAINER:-(실행 중인 컨테이너 없음)} / ${PREV_IMAGE:-(이미지 기록 없음)}"
log "배포: $NEW(:$NEW_PORT) $NEW_IMAGE"

if ! docker pull "$NEW_IMAGE"; then
  alert "이미지를 받을 수 없음: $NEW_IMAGE (기존 버전 유지)"
  exit 1
fi

# ---------- 새 쪽 기동 + 헬스체크 ----------

remove_app "$NEW"   # 이전 실패 등으로 남아 있을 수 있는 잔재 정리
APP_IMAGE="$NEW_IMAGE" "${COMPOSE[@]}" up -d "app-$NEW"

if ! wait_healthy "http://127.0.0.1:$NEW_PORT/actuator/health"; then
  "${COMPOSE[@]}" logs "app-$NEW" --tail=50 || true
  remove_app "$NEW"
  alert "새 버전 헬스체크 실패로 배포 중단: $NEW_IMAGE (트래픽은 기존 버전 유지)"
  exit 2
fi

# ---------- 트래픽 전환 ----------

log "Nginx upstream 전환: :$ACTIVE_PORT → :$NEW_PORT"
if ! write_upstream "$NEW_PORT" || ! public_healthy; then
  log "전환 실패, upstream 원복"
  write_upstream "$ACTIVE_PORT" || true
  remove_app "$NEW"
  alert "트래픽 전환 실패로 배포 중단: $NEW_IMAGE (기존 버전으로 원복)"
  exit 3
fi

# ---------- 기존 쪽 종료 ----------

sleep "$DRAIN_SECONDS"
if [[ -n "$OLD_CONTAINER" ]]; then
  log "기존 컨테이너 종료: $OLD_CONTAINER"
  docker stop -t 35 "$OLD_CONTAINER" >/dev/null || true
  docker rm "$OLD_CONTAINER" >/dev/null || true
fi

echo "$NEW_IMAGE" > "$STATE_DIR/current_image"
if [[ -n "$PREV_IMAGE" && "$PREV_IMAGE" != "$NEW_IMAGE" ]]; then
  echo "$PREV_IMAGE" > "$STATE_DIR/previous_image"
fi
cleanup_images "$NEW_IMAGE" "$PREV_IMAGE"
log "배포 성공: $NEW_IMAGE ($NEW 활성)"
