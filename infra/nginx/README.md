# Nginx 설정 (프로덕션)

AWS EC2(Amazon Linux 2023) 배포 서버의 Nginx 리버스 프록시 설정 원본입니다.
서버 재구축 시 아래 순서로 그대로 배치하면 됩니다.

## 구성

| 저장소 경로 | 서버 경로 |
|---|---|
| `nowhere-api.conf` | `/etc/nginx/conf.d/nowhere-api.conf` |
| `snippets/proxy-common.conf` | `/etc/nginx/snippets/proxy-common.conf` |
| `snippets/sse-common.conf` | `/etc/nginx/snippets/sse-common.conf` |

- `api.nowhere-app.cloud` → `127.0.0.1:8080` (Spring Boot 컨테이너)
- `nowhere-app.cloud`, `www.nowhere-app.cloud` → 상태 확인용 텍스트 응답
  (웹 프론트 배포 전까지 API를 이 호스트로 노출하지 않는다)

## SSE

아래 세 엔드포인트는 `sse-common.conf`로 버퍼링/캐시를 끄고 타임아웃을 24h로 둔다.

- `GET /api/locations/stream`
- `GET /api/sse/notifications`
- `GET /api/sos/posts/{postId}/stream`

## 설치

```bash
sudo dnf install -y nginx certbot python3-certbot-nginx
sudo mkdir -p /etc/nginx/snippets
sudo install -m 644 snippets/*.conf /etc/nginx/snippets/
sudo install -m 644 nowhere-api.conf /etc/nginx/conf.d/nowhere-api.conf
sudo nginx -t && sudo systemctl enable --now nginx
```

## TLS

`# managed by Certbot` 주석이 붙은 443 listen 블록과 80 → 443 리다이렉트는
Certbot이 이 파일을 직접 수정해 넣은 것이다. 새 서버에서는 위 파일을 배치한 뒤
아래 명령으로 다시 생성한다.

```bash
sudo certbot --nginx --agree-tos -m <email> --redirect \
  -d api.nowhere-app.cloud -d nowhere-app.cloud -d www.nowhere-app.cloud
sudo systemctl enable --now certbot-renew.timer
```

자동 갱신은 `certbot-renew.timer`(하루 2회)가 담당한다. 확인은 `sudo certbot renew --dry-run`.

## 보안그룹

Nginx가 443에서 받아 내부적으로만 8080을 호출하므로 인바운드는
**22(SSH) / 80(리다이렉트용) / 443(HTTPS)** 만 열려 있으면 된다.
