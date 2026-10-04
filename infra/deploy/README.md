# 배포 / 롤백

운영 서버(EC2, `~/backend`)의 app 컨테이너 교체는 `deploy.sh`가 담당한다.
GitHub Actions(`Deploy`, `Rollback` 워크플로우)가 SSH로 접속해 이 스크립트를 실행한다.

## 동작

1. 지정한 이미지(`ghcr.io/cbnu-swcapstone-b5-tjts-now/backend:sha-xxxxxxx`)를 pull
   - 실패하면 기존 컨테이너는 그대로 두고 종료
2. app 컨테이너만 새 이미지로 재기동 (postgres/redis는 건드리지 않음)
3. `http://127.0.0.1:8080/actuator/health`를 5초 간격으로 최대 30회 확인
4. 성공 → 실행 중인 이미지를 기록하고, 현재/직전 이미지만 남기고 정리
5. 실패 → 직전 이미지로 자동 복귀, 워크플로우는 실패로 표시

배포 기록은 레포 밖 `~/.nowhere-deploy/`에 둔다.

| 파일 | 내용 |
|---|---|
| `current_image` | 지금 실행 중인 이미지 |
| `previous_image` | 그 직전 이미지 |

## 수동 롤백

GitHub → Actions → **Rollback** → Run workflow → 태그 입력 (예: `sha-c34166b`)

태그는 GHCR 패키지 페이지 또는 Deploy 실행 기록의 build-and-push 단계에서 확인한다.
지정한 태그도 헬스체크에 실패하면 원래 버전으로 되돌아간다.

## 주의

- `docker-compose.prod.yml`의 app 이미지는 `APP_IMAGE` 환경변수로 지정된다.
  서버에서 `APP_IMAGE` 없이 `docker compose up`을 직접 실행하면 `latest`로 바뀌어
  롤백 상태가 풀릴 수 있다. 직접 재기동이 필요하면 아래처럼 실행한다.

  ```bash
  cd ~/backend
  APP_IMAGE="$(cat ~/.nowhere-deploy/current_image)" docker compose -f docker-compose.prod.yml up -d app
  ```
