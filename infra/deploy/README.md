# 배포 / 롤백

운영 서버(EC2, `~/backend`)의 app 교체는 `deploy.sh`가 **blue-green 방식으로 무중단** 처리한다.
GitHub Actions(`Deploy`, `Rollback` 워크플로우)가 SSH로 접속해 이 스크립트를 실행한다.

## 구성

| 이름 | 컨테이너 | 포트 |
|---|---|---|
| blue | `nowhere-app-blue` | `127.0.0.1:8080` |
| green | `nowhere-app-green` | `127.0.0.1:8081` |

평소에는 한쪽만 떠 있다. Nginx가 어느 쪽을 바라볼지는
`/etc/nginx/snippets/nowhere-upstream.conf`의 포트 한 줄로 정해진다 (`infra/nginx/` 참고).

## 동작

1. 지정한 이미지(`ghcr.io/cbnu-swcapstone-b5-tjts-now/backend:sha-xxxxxxx`)를 pull
   - 실패하면 아무것도 바꾸지 않고 종료
2. 비활성 쪽에 새 이미지로 app 기동 (postgres/redis는 건드리지 않음)
3. 새 쪽 `/actuator/health`를 5초 간격으로 최대 30회 확인
   - 실패 → 새 쪽만 제거하고 종료. **트래픽은 한 번도 넘어가지 않았으므로 기존 버전이 그대로 서비스된다**
4. Nginx upstream을 새 쪽 포트로 바꾸고 `systemctl reload nginx` (기존 연결은 끊지 않음)
5. Nginx(HTTPS)를 거쳐 새 쪽이 응답하는지 확인
   - 실패 → upstream을 원래 포트로 되돌리고 새 쪽 제거
6. 5초 대기 후 기존 쪽을 graceful하게 종료 (처리 중인 요청 마무리, 최대 35초)
7. 실행 중인 이미지를 기록하고, 현재/직전 이미지만 남기고 정리

배포 기록은 레포 밖 `~/.nowhere-deploy/`에 둔다.

| 파일 | 내용 |
|---|---|
| `current_image` | 지금 서비스 중인 이미지 |
| `previous_image` | 그 직전 이미지 |

SSE(실시간 알림) 연결은 기존 쪽이 종료될 때 한 번 끊기고, 클라이언트가 재연결하면 새 쪽에 붙는다.
일반 API 요청은 전환 중에도 실패하지 않는다.

## 수동 롤백

GitHub → Actions → **Rollback** → Run workflow (branch: `main`) → 태그 입력 (예: `sha-c34166b`)

롤백도 같은 스크립트로 무중단 전환된다. 태그는 GHCR 패키지 페이지 또는
Deploy 실행 기록의 build-and-push 단계에서 확인한다.

## 직접 확인 / 재기동

```bash
cd ~/backend
cat /etc/nginx/snippets/nowhere-upstream.conf          # 활성 포트 (8080=blue, 8081=green)
docker ps --format '{{.Names}} {{.Image}} {{.Ports}}'
```

app은 compose `profiles`로 묶여 있어 `docker compose up -d`만으로는 뜨지 않는다.
활성 쪽을 직접 재기동해야 하면 서비스명과 이미지를 지정한다 (예: blue가 활성일 때).

```bash
APP_IMAGE="$(cat ~/.nowhere-deploy/current_image)" docker compose -f docker-compose.prod.yml up -d app-blue
```

## 주의

- 무중단 전환 중 30초 안팎 app 2개가 함께 떠 있다. t3.micro에서는 기존 쪽 메모리가 swap으로 밀려나며,
  트래픽이 많을 때는 이 구간에 응답이 느려질 수 있다.
- 두 app이 겹치는 동안 `TrustScoreScheduler`가 중복 실행되지 않도록 PostgreSQL advisory lock을 쓴다.
  새로 `@Scheduled` 작업을 추가할 때도 같은 처리가 필요하다.
