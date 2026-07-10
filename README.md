# server-team1

**적(積)** — 랜덤 미션 기반 디지털 디톡스 서비스. 제8회 코커톤.

매일 설정한 디톡스 시간에 랜덤 미션을 수행하고 인증사진을 올려 습관을 쌓는다.
**로그인이 없다.** 앱이 생성한 `deviceId` 로 사용자를 식별한다.

Spring Boot 4.1.0 / Java 21 / PostgreSQL.

## 빠른 실행

```bash
docker compose up -d           # Postgres 기동 (호스트 5433)
./gradlew bootRun
```

`.env` 는 **필요 없다.** `application.yml` 과 `docker-compose.yml` 의 기본값이 서로 맞춰져 있어
클론하고 위 두 줄만 치면 바로 뜬다.

호스트 포트가 5433인 이유: 로컬에 PostgreSQL 이 이미 설치돼 있어도 5432 와 충돌하지 않게 하기 위함.

테스트도 실제 Postgres 로 돈다. `./gradlew test` 전에 `docker compose up -d` 가 떠 있어야 한다.

- Swagger UI: http://localhost:8080/swagger-ui.html

## 구조

```
domain/
  user/    사용자 (deviceId, 닉네임, 디톡스 시간)
  team/    팀 생성 / 초대코드 참여
  mission/ 랜덤 미션, 인증
  image/   S3 Presigned URL 발급
global/
  config/    WebConfig(CORS), SwaggerConfig
  response/  ApiResponse, ErrorResponse
  exception/ ErrorCode, BusinessException, GlobalExceptionHandler
```

## 공통 응답 형식

모든 API 는 아래 형식으로 응답한다. 컨트롤러는 `ApiResponse.ok(...)` / `ApiResponse.created(...)` 를 반환한다.

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

`reasons` 는 `@Valid` 검증 실패 시 `{ "필드명": "메시지" }` 로 채워진다.

## 환경변수

로컬은 기본값으로 동작한다. 배포(prod 프로파일)는 `DB_URL / DB_USERNAME / DB_PASSWORD` 를 환경변수로 주입한다.

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
