# server-team1

제8회 코커톤 백엔드. Spring Boot 4.1.0 / Java 21 / PostgreSQL.

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
  user/    사용자 (인증용)
global/
  config/  SecurityConfig, SwaggerConfig
  jwt/     JwtProvider, JwtAuthenticationFilter
  exception/ GlobalExceptionHandler
```

기능 도메인은 `domain/` 아래에 패키지를 하나씩 추가한다.
**새 엔드포인트는 기본이 공개(permitAll)** 라 로그인 없이 바로 호출·시연할 수 있다.

## 환경변수

로컬은 기본값으로 동작하므로 보통 `.env` 를 만들 필요가 없다.
다음 경우에만 `.env.example` 를 복사해 `.env` 로 만든다.

- 카카오·구글 소셜로그인을 로컬에서 테스트할 때 (클라이언트 키 필요)
- 로컬 DB 비밀번호를 기본값(`hackathon`) 과 다르게 쓸 때

배포(prod 프로파일)는 `DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET` 를 환경변수로 주입한다.
prod 는 `JWT_SECRET` 이 없으면 부팅에 실패한다 (개발용 기본 시크릿이 운영에 새는 것을 막기 위함).

JWT 시크릿 생성 (64바이트 난수 → 128자 hex):
```powershell
$b = New-Object byte[] 64; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
```

## 배포

`main` 에 푸시하면 자동으로 배포된다.

```
GitHub Actions → 이미지 빌드 → Docker Hub → SSM → EC2 에서 컨테이너 교체
```

EC2 는 SSH 인바운드를 열지 않고 SSM 으로만 명령을 받는다.
앱 시크릿은 EC2 의 `/home/ec2-user/.env` 에만 있고 GitHub 에는 두지 않는다.

필요한 GitHub Actions Secrets: `DOCKERHUB_USERNAME` `DOCKERHUB_TOKEN`
`AWS_ACCESS_KEY_ID` `AWS_SECRET_ACCESS_KEY` `AWS_REGION` `EC2_INSTANCE_ID`

## 인증 (선택 기능)

로그인은 **시간이 남으면 붙이는 선택 기능**이다. 지금은 코드가 들어 있지만 언제든 통째로 뺄 수 있게 격리해 두었다.
그래서 **새 기능 코드는 로그인에 의존하지 않는다.** 사용자를 식별해야 하면 `userId` 를 요청 파라미터로 받고,
엔티티에서 `User` 를 참조하지 않는다.

붙여서 쓸 경우 회원가입 `POST /api/auth/signup`, 로그인 `POST /api/auth/login` 이 JWT 를 발급하고,
`GET /api/users/me` 가 토큰으로 내 정보를 돌려준다. 카카오·구글 소셜로그인도 들어 있다
(`.env` 에 클라이언트 키 필요).

### 인증 제거

아래만 지우면 나머지 코드는 그대로 돈다.

1. `domain/auth/` 전체, `domain/user/` 전체
2. `global/jwt/` 전체
3. `SecurityConfig` 에서 `[AUTH]` 주석이 붙은 줄과 `AUTHENTICATED_PATHS` 배열
4. `SwaggerConfig` 의 `SecurityScheme` / `SecurityRequirement` 설정
5. `build.gradle` 의 `spring-boot-starter-security`, `jjwt-*` 의존성
6. `.env` 의 `JWT_*`, `KAKAO_*`, `GOOGLE_*`
