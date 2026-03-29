# now·here (지금 어때) - Backend

> 충북대학교 캠퍼스 실시간 혼잡도 공유 서비스

---

## 프로젝트 소개

now·here는 충북대학교 캠퍼스 내 학식당, 카페, 라운지 등 주요 시설의 실시간 혼잡도를 공유하는 하이퍼로컬 서비스입니다.

### 목표

지오펜싱 기반 현장 인증 시스템을 통해 신뢰도 높은 실시간 혼잡도 정보를 제공하고, 학우들의 불필요한 대기 시간을 줄여 효율적인 캠퍼스 생활을 지원합니다.

---

## 기술 스택

### Backend
- **Spring Boot** - REST API 서버
- **Spring Security** - 인증 및 인가
- **JPA / Hibernate** - ORM

### Database
- **Redis** - 캐싱 및 휘발성 데이터 처리 (혼잡도, TTL 관리)
- **PostgreSQL** - 공간 연산 및 영구 데이터 관리

### Infrastructure
- **Docker** - 컨테이너 기반 환경 관리
- **Prometheus & Grafana** - 네트워크 및 서버 상태 모니터링
- **nGrinder** - 부하 테스트

---

## 주요 기능

### MVP 기능

1. **지오펜싱 기반 현장 인증 API**
   - 사용자 위치 수신 및 현장 인증 거리 계산
   - 인증된 사용자만 혼잡도 제보 허용

2. **실시간 혼잡도 관리 API**
   - 장소별 혼잡도 제보 수신 및 집계
   - 혼잡도 상태 조회 (빨강/노랑/초록)

3. **데이터 자동 휘발 처리**
   - Redis TTL 설정으로 오래된 혼잡도 정보 자동 삭제
   - 마지막 업데이트 시각 관리

### 확장 기능 (Phase 2)

- Peer Review 상호 검증 시스템
- 빈자리 알림 구독 기능
- 현장 한정 SOS 미니 게시판

---

## 시작하기

### 요구사항

- Java 17 이상
- Docker & Docker Compose

### 설치 및 실행

```bash
# 저장소 클론
git clone https://github.com/CBNU-SWCapstone-B5-TJTS-now/backend.git
cd backend

# 인프라 실행 (PostgreSQL, Redis)
docker-compose up -d

# 애플리케이션 빌드 및 실행
./gradlew bootRun
```

---

## 개발 가이드

### 1. 이슈 확인 및 생성

- 이미 정의된 이슈가 있는지 확인
- 없다면 GitHub Issues에서 새로운 이슈 생성

**이슈 제목 형식**:
```
[타입] 기능명

예시:
[feat] 혼잡도 제보 API 구현
[fix] 지오펜싱 거리 계산 오류 수정
[docs] README 업데이트
```

### 2. 브랜치 생성

**GitHub에서 생성 (권장)**:
1. 이슈 페이지 우측 Development 섹션
2. Create a branch 클릭
3. 자동 생성된 브랜치명 사용

**로컬에서 생성**:
```bash
# dev 브랜치에서 시작
git checkout dev
git pull origin dev

# 새 브랜치 생성
git switch -c 이슈번호-기능명

# 예시
git switch -c 1-feat-congestion-api
```

### 3. 개발 및 커밋

**커밋 메시지 규칙**:
```
타입: 간단한 설명

예시:
feat: 혼잡도 제보 API 구현
fix: 지오펜싱 거리 계산 오류 수정
docs: API 명세 추가
```

**주요 커밋 타입**:

| 타입 | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 추가 | `feat: 혼잡도 조회 API 구현` |
| `fix` | 버그 수정 | `fix: TTL 설정 오류 수정` |
| `docs` | 문서 작성/수정 | `docs: API 문서 작성` |
| `style` | 코드 포맷팅 변경 | `style: 코드 정렬 정리` |
| `refactor` | 코드 리팩토링 | `refactor: 서비스 레이어 분리` |
| `perf` | 성능 개선 | `perf: Redis 캐시 전략 개선` |
| `test` | 테스트 코드 추가/수정 | `test: 혼잡도 서비스 테스트 추가` |
| `chore` | 빌드, 패키지 등 기타 작업 | `chore: 의존성 업데이트` |

**커밋 예시**:
```bash
git add .
git commit -m "feat: 혼잡도 제보 API 구현"
git push origin 1-feat-congestion-api
```

### 4. Pull Request 생성

**PR 제목 형식**:
```
타입: 제목

예시:
feat: 실시간 혼잡도 제보 API 구현
```

**PR 템플릿**:
```markdown
## 변경 사항
- 구현한 기능 1
- 구현한 기능 2

## 테스트 방법
1. 애플리케이션 실행
2. API 호출 (엔드포인트, 요청 예시)
3. 응답 확인

## 관련 이슈
Closes #이슈번호
```

### 5. 코드 리뷰 및 병합

1. 리뷰 후 피드백 반영
2. dev 브랜치에 병합

### 6. 로컬 브랜치 정리
```bash
# dev 브랜치로 이동
git checkout dev

# 최신 상태로 업데이트
git pull origin dev

# 작업 완료된 브랜치 삭제 (안 해도 됨)
git branch -d 1-feat-congestion-api
```

---

## 브랜치 전략
```
main (프로덕션)
  └── dev (개발 메인)
       ├── feat/기능명
       ├── fix/버그명
       └── docs/문서명
```

### 브랜치 규칙

- `main`: 프로덕션 배포용 (직접 푸시 금지)
- `dev`: 개발 메인 브랜치 (PR을 통해서만 병합)
- `feat/*`: 새 기능 개발
- `fix/*`: 버그 수정
- `docs/*`: 문서 작업

### 작업 흐름

1. dev에서 새 브랜치 생성
2. 작업 후 커밋 및 푸시
3. dev로 PR 생성
4. 코드 리뷰 후 병합
5. 주기적으로 dev → main 병합 (릴리즈)

---

## 프로젝트 구조
```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/nowhere/
│   │   │       ├── controller/   # REST API 컨트롤러
│   │   │       ├── service/      # 비즈니스 로직
│   │   │       ├── repository/   # DB 접근 계층
│   │   │       ├── domain/       # 엔티티 및 도메인 모델
│   │   │       ├── dto/          # 요청/응답 DTO
│   │   │       └── config/       # 설정 클래스
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── com/nowhere/      # 테스트 코드
├── docker-compose.yml
└── build.gradle
```
