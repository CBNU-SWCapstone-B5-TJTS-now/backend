#!/usr/bin/env bash
# 운영 서버(~/backend)에서 app 컨테이너를 지정한 이미지로 교체하고,
# 헬스체크가 실패하면 직전 이미지로 자동 복귀한다.
#
# 사용법: infra/deploy/deploy.sh <이미지 태그>   (예: sha-c34166b)
# GHCR 로그인은 호출하는 쪽(GitHub Actions)에서 미리 해 둔다.
#
# 종료 코드
#   0  새 이미지 배포 성공
#   1  배포 전 단계 실패 (이미지 pull 실패 등, 기존 컨테이너는 그대로)
#   2  헬스체크 실패 → 직전 이미지로 복귀 성공
#   3  헬스체크 실패 → 복귀 대상이 없거나 복귀도 실패 (수동 조치 필요)
set -euo pipefail

REPO="ghcr.io/cbnu-swcapstone-b5-tjts-now/backend"
CONTAINER="nowhere-app"
HEALTH_URL="http://127.0.0.1:8080/actuator/health"
STATE_DIR="$HOME/.nowhere-deploy"          # 레포 밖에 두어 git pull과 섞이지 않게 한다
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

# 직전 이미지: 마지막 성공 기록 → 없으면(최초 1회) 지금 떠 있는 컨테이너의 이미지 digest
current_image() {
  if [[ -s "$STATE_DIR/current_image" ]]; then
    cat "$STATE_DIR/current_image"
    return
  fi
  local id
  id="$(docker inspect -f '{{.Image}}' "$CONTAINER" 2>/dev/null)" || return 0
  docker image inspect -f '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$id" 2>/dev/null || true
}

# compose 파일의 image가 ${APP_IMAGE}를 참조한다. 셸 환경변수가 .env보다 우선한다.
start_app() {
  APP_IMAGE="$1" "${COMPOSE[@]}" up -d app
}

# t3.micro는 기동이 느려서 5초 간격으로 최대 30번(약 2분 30초) 확인한다.
wait_healthy() {
  for i in $(seq 1 30); do
    if curl -fsS -o /dev/null "$HEALTH_URL"; then
      log "healthy (attempt $i)"
      return 0
    fi
    sleep 5
  done
  return 1
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

PREV_IMAGE="$(current_image)"
log "현재 이미지: ${PREV_IMAGE:-(없음)}"
log "배포 이미지: $NEW_IMAGE"

if [[ "$PREV_IMAGE" == "$NEW_IMAGE" ]]; then
  log "이미 같은 이미지로 실행 중이지만 그대로 재기동한다."
fi

# pull이 실패하면 기존 컨테이너를 건드리기 전에 끝난다 (종료 코드 1).
if ! docker pull "$NEW_IMAGE"; then
  alert "이미지를 받을 수 없음: $NEW_IMAGE (기존 컨테이너 유지)"
  exit 1
fi

start_app "$NEW_IMAGE"

if wait_healthy; then
  echo "$NEW_IMAGE" > "$STATE_DIR/current_image"
  if [[ -n "$PREV_IMAGE" && "$PREV_IMAGE" != "$NEW_IMAGE" ]]; then
    echo "$PREV_IMAGE" > "$STATE_DIR/previous_image"
  fi
  cleanup_images "$NEW_IMAGE" "$PREV_IMAGE"
  log "배포 성공: $NEW_IMAGE"
  exit 0
fi

log "헬스체크 실패: $NEW_IMAGE"
"${COMPOSE[@]}" logs app --tail=50 || true

if [[ -z "$PREV_IMAGE" || "$PREV_IMAGE" == "$NEW_IMAGE" ]]; then
  alert "헬스체크 실패, 복귀할 직전 이미지가 없음. 수동 조치 필요: $NEW_IMAGE"
  exit 3
fi

log "직전 이미지로 복귀: $PREV_IMAGE"
start_app "$PREV_IMAGE"

if wait_healthy; then
  alert "헬스체크 실패로 자동 롤백함: $NEW_IMAGE → $PREV_IMAGE"
  exit 2
fi

alert "롤백 후에도 헬스체크 실패. 수동 조치 필요: $PREV_IMAGE"
exit 3
