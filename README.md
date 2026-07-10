# server-team1

**적(積)** — 랜덤 미션 기반 디지털 디톡스 서비스. 제8회 코커톤.

매일 설정한 디톡스 시간에 랜덤 미션을 수행하고 인증사진을 올려 벽돌을 쌓는다.
**로그인이 없다.** 앱이 생성한 `deviceId` 로 사용자를 식별하고, 대부분의 API 는
`X-Device-Id` 헤더로 사용자를 구분한다.

Spring Boot 4.1.0 / Java 21 / PostgreSQL / AWS(EC2, RDS, S3).

## 빠른 실행

```bash
docker compose up -d           # Postgres 기동 (호스트 5433)
./gradlew bootRun
```

`.env` 는 필요 없다. `application.yml` 과 `docker-compose.yml` 의 기본값이 서로 맞춰져 있어
클론하고 위 두 줄만 치면 바로 뜬다.

호스트 포트가 5433인 이유: 로컬에 PostgreSQL 이 이미 설치돼 있어도 5432 와 충돌하지 않게 하기 위함.

테스트도 실제 Postgres 로 돈다. `./gradlew test` 전에 `docker compose up -d` 가 떠 있어야 한다.

- Swagger UI: http://localhost:8080/swagger-ui.html

## 구조

```
domain/
  user/    온보딩, 디톡스 시간, 홈 화면, 벽돌 정산, 팀 전환, 알림 설정
  team/    팀 생성 / 초대코드 참여 / 팀 조회 (N:M, 한 사용자가 여러 팀에 속할 수 있음)
  mission/ 오늘의 미션, 10분 타이머, 사진 인증
  detox/   디톡스 진행 화면 (겹치는 팀원 조회)
  image/   S3 업로드 (미션 인증 사진)
global/
  config/    WebConfig(CORS), SwaggerConfig
  response/  ApiResponse, ErrorResponse
  exception/ ErrorCode, BusinessException, GlobalExceptionHandler
```

## API 요약

인증 없이 `X-Device-Id` 헤더로 사용자를 식별한다. 상세 스펙은 Swagger UI 참고.

| 도메인 | Method | Path | 설명 |
|---|---|---|---|
| User | POST | `/api/users` | 온보딩 (사용자 생성/로그인) |
| User | GET/POST/PATCH | `/api/users/detox-time` | 디톡스 시간 조회/설정/수정 |
| User | PATCH | `/api/users/{userId}/active-team` | 선택된 팀 전환 |
| User | PATCH | `/api/users/{userId}/email` | 이메일 등록/수정 (정보 수집만, 발송 없음) |
| User | PATCH | `/api/users/{userId}/notification` | 알림 허용 여부 토글 |
| User | GET | `/api/users/{userId}` | 유저 정보 조회 |
| User | GET | `/api/users/{userId}/home` | 홈 화면 (벽돌/인증 현황/정산) |
| Team | POST | `/api/teams` | 팀 생성 + 초대코드 발급 |
| Team | POST | `/api/teams/join` | 초대코드로 참여 |
| Team | GET | `/api/users/{userId}/teams` | 내 팀 목록 |
| Team | GET | `/api/teams/{teamId}` | 팀 상세 |
| Mission | GET | `/api/missions/today`, `/today/status` | 오늘의 미션 조회 |
| Mission | POST | `/api/missions/today/popup` | 팝업 표시 시각 기록 |
| Mission | POST | `/api/missions/today/confirm` | 미션 확인 |
| Mission | POST/PATCH | `/api/missions/today/certification` | 사진 인증 / 재인증 (multipart) |
| Detox | GET | `/api/detox/progress` | 디톡스 진행 상태, 겹치는 팀원 |

## 공통 응답 형식

컨트롤러는 `ApiResponse.ok(...)` / `ApiResponse.created(...)` 를 반환한다.

성공
```json
{ "success": true, "status": 200, "code": "SUCCESS",
  "message": "...", "data": {}, "timestamp": "2026-07-10T12:30:00.123Z" }
```

실패
```json
{ "success": false, "status": 400, "code": "TEAM_ERROR_409_TEAM_FULL",
  "message": "...", "path": "/api/teams/join",
  "timestamp": "2026-07-10T12:30:00.123Z", "reasons": {} }
```

에러를 내려면 `ErrorCode` 에 상수를 추가하고 서비스에서 `throw new BusinessException(ErrorCode.XXX)` 한다.
`GlobalExceptionHandler` 가 공통 포맷으로 변환한다. `code` 는 enum 이름을 그대로 쓴다.
`reasons` 는 `@Valid` 검증 실패나 필수 헤더 누락 시 `{ "필드명": "메시지" }` 로 채워진다.

## 이미지 업로드

미션 인증 사진은 presigned URL 방식이 아니라 **백엔드로 파일을 직접 업로드**한다.

```
POST /api/missions/today/certification  (multipart/form-data)
  헤더: X-Device-Id
  바디: image 파트에 이미지 파일 (jpg/png, 10MB 이하)
```

백엔드가 S3 업로드와 DB 기록(`image_url`, `status=SUCCESS`)을 함께 처리한다.

## 환경변수

로컬은 기본값으로 동작한다. 배포(prod 프로파일)는 아래를 환경변수로 주입한다.

- `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` — RDS 접속
- `AWS_REGION` / `AWS_S3_BUCKET` / `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` — S3 업로드

## 배포

`main` 에 푸시하면 자동으로 배포된다.

```
GitHub Actions → 이미지 빌드 → Docker Hub → SSM → EC2 에서 컨테이너 교체
```

EC2 는 SSH 인바운드를 열지 않고 SSM 으로만 명령을 받는다.
앱 시크릿은 EC2 의 `/home/ec2-user/.env` 에만 있고 GitHub 에는 두지 않는다.

필요한 GitHub Actions Secrets: `DOCKERHUB_USERNAME` `DOCKERHUB_TOKEN`
`AWS_ACCESS_KEY_ID` `AWS_SECRET_ACCESS_KEY` `AWS_REGION` `EC2_INSTANCE_ID`

## 협업

`main` 에 직접 푸시하지 않는다. 브랜치를 파고 PR 을 연다.
