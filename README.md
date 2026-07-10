# Hackathon Backend

제7회 코커톤 백엔드 템플릿. Spring Boot 4.1.0 / Java 21 / JWT 인증 / 카카오·구글 소셜로그인 / PostgreSQL.

## 빠른 실행

### 방법 A. Docker Postgres (권장, 실제 개발용)
```bash
cp .env.example .env           # 최초 1회
docker compose up -d           # Postgres 기동 (호스트 5433)
./gradlew bootRun              # 기본 프로파일 = Postgres
```
호스트 포트가 5433인 이유: 로컬에 PostgreSQL 이 이미 설치돼 있어도 5432 와 충돌하지 않게 하기 위함.

### 방법 B. Docker 없이 (H2 인메모리)
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔(local): http://localhost:8080/h2-console  (JDBC URL: `jdbc:h2:mem:hackathon`)

## API 요약

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/signup` | 회원가입 (+토큰 발급) | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/oauth/kakao` | 카카오 로그인 (code 전달) | X |
| POST | `/api/auth/oauth/google` | 구글 로그인 (code 전달) | X |
| GET | `/api/users/me` | 내 정보 조회 | O (Bearer) |
| GET | `/api/sample/applications` | Mock 데이터 예시 | O |

인증 필요한 API는 헤더에 `Authorization: Bearer <accessToken>`.
Swagger UI 우측 상단 **Authorize** 버튼에 토큰만 넣으면 이후 요청에 자동 첨부됨.

## 소셜 로그인 흐름 (프론트/백 분리)

```
프론트가 카카오/구글에서 인가코드(code) 획득
  → POST /api/auth/oauth/{kakao|google}  { "code": "...", "redirectUri": "..." }
  → 백엔드가 code 로 제공자 API 호출 → 유저 조회/생성 → 우리 JWT 발급
```
`.env.example` 를 복사해 `.env` 로 만들고 카카오/구글 클라이언트 키를 채우세요.

## 환경변수
`.env.example` 참고. 로컬은 기본값으로 동작하며 **소셜 키만** 채우면 됩니다.
배포(prod 프로파일)는 `DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET` 를 환경변수로 주입.
prod 는 `JWT_SECRET` 이 없으면 부팅에 실패한다 (개발용 기본 시크릿이 운영에 새는 것을 막기 위함).

JWT 시크릿 생성 (64바이트 난수 → 128자 hex):
```powershell
$b = New-Object byte[] 64; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
```

## 구조
```
domain/
  auth/    회원가입·로그인·소셜로그인 (controller/service/dto/oauth)
  user/    사용자 엔티티·내 정보 조회 (controller/service/repository/entity/dto)
  sample/  하드코딩 Mock 데이터 예시
global/
  config/  SecurityConfig, SwaggerConfig
  jwt/     JwtProvider, JwtAuthenticationFilter
  exception/ GlobalExceptionHandler
```
