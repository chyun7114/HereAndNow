# 쿼리 최적화 1단계: 데이터셋/실행 컨테이너 준비

## 목차

- 1. 목적
- 2. 생성한 파일
- 3. 실행 방법
- 4. 준비 완료 확인 방법
- 5. 정리 방법

---

## 1. 목적

`query-test-plan.md`를 단계적으로 수행하기 위해, 먼저 아래를 자동화한다.

- PostGIS/Redis 실행 컨테이너 기동
- 스키마 마이그레이션 적용
- 쿼리 성능 테스트용 데이터셋 생성 (10k, 100k)

---

## 2. 생성한 파일

- 컨테이너 정의: `docker/query-test/docker-compose.yml`
- 데이터셋 생성 SQL: `scripts/query-test/seed_query_dataset.sql`
- 준비 자동화 스크립트(Windows PowerShell): `scripts/query-test/setup-query-test.ps1`

---

## 3. 실행 방법

프로젝트 루트에서 실행한다.

### 3.1 10k 데이터셋만 준비

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\query-test\setup-query-test.ps1 -Dataset small
```

### 3.2 100k 데이터셋만 준비

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\query-test\setup-query-test.ps1 -Dataset large
```

### 3.3 10k, 100k 순차 준비

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\query-test\setup-query-test.ps1 -Dataset both
```

참고:

- `both`는 마지막에 `ds100k` 데이터로 덮어써진다.
- 10k와 100k를 각각 테스트할 때는 `small`, `large`를 분리 실행하는 것을 권장한다.

---

## 4. 준비 완료 확인 방법

### 4.1 컨테이너 상태

```powershell
docker compose -f .\docker\query-test\docker-compose.yml ps
```

### 4.2 PostGIS 연결 정보

- Host: `localhost`
- Port: `5432`
- DB: `hereandnow_query`
- User: `hereandnow`
- Password: `hereandnow`

### 4.3 인덱스 확인 예시

```sql
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE tablename = 'place'
ORDER BY indexname;
```

`idx_place_location_gist`와 `idx_place_location_geom_gist`가 보이면 정상이다.

---

## 5. 정리 방법

컨테이너만 내리기:

```powershell
docker compose -f .\docker\query-test\docker-compose.yml down
```

데이터 볼륨까지 완전 삭제:

```powershell
docker compose -f .\docker\query-test\docker-compose.yml down -v
```
