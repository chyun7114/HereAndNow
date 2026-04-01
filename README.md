# 📍 Here & Now

> **Kusitms_32nd_Meet_Up_Team3_HereAndNow_Backend**

<br/>

## 📖 프로젝트 소개

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/f7b04a9f-f46a-4f84-bbe0-c05c389b3718" /></br>

> **지금 이 순간을 기록하고 싶은 연인들을 위한 데이트 장소 추천과 아카이브 서비스**<br/>
> 히어 앤 나우는 광고와 저품질 정보로 인한 피로감을 없애고, 신뢰할 수 있는 UGC 기반의 데이트 코스를 제안합니다. <br/>
> 커플이 함께 계획을 세우고 다녀온 장소를 지도 위에 기록하는, 데이트 코스 추천 및 아카이빙 서비스입니다.

## 주요 성과 정리

## 🚀 PostGIS 공간 쿼리 성능 최적화

공간 조회 쿼리에서 `ST_Intersects` / `ST_DWithin`과 같은 정밀 공간 연산이 데이터 증가 시 병목이 되는 문제를 해결하기 위해, GiST 인덱스와 MBR(`&&`) 선필터를 조합한 구조로 쿼리를 재설계했습니다.

### 핵심 개선

- **Seq Scan 기반 병목 제거 → Index Scan 중심 실행계획으로 전환**
- **MBR(`&&`) 선필터를 통해 정밀 공간 연산 이전 후보군 축소**
- **대용량(100k) 데이터에서 성능 안정성 확보**

### 주요 성과

- **Place 조회 (100k, 일반 범위)**  
  `109.830ms → 2.027ms` (**약 54배 개선**)

- **Course 조회 (100k, 넓은 범위)**  
  `7463.794ms → 16.935ms` (**약 440배 개선**)

- **실행계획 개선**
  - `Seq Scan + Join Filter` → `Bitmap/Index Scan` 기반으로 전환
  - Join Filter 제거량 수천만 건 → 후보군 축소 구조로 변경

### 설계 포인트

- GiST 인덱스만으로는 넓은 범위/대용량에서 후보군 축소가 충분하지 않음을 확인
- MBR(`&&`)을 명시적으로 추가하여 1차 필터링 수행
- 정밀 연산(`ST_Intersects`)은 최종 필터로만 사용하도록 구조 분리

### 추가 검증

- 10k / 100k 데이터셋 + 좁은/일반/넓은 범위 시나리오별 실험
- 인덱스 단일화 A/B 테스트 수행 → 일관된 우세 없음 → 병행 유지 결정
- 성능 회귀 방지를 위한 기준선 및 실행계획 가드레일 정의

### 상세 보고서

[상세 보고서 링크](./docs/result/README.md)

---

## 🛠️ 주요 사용 기술

### Backend

- Java 21
- Spring Boot 3.3.1
- Spring Security
- Spring Data JPA

### Database

- PostgreSQL
- PostGIS (for spatial data)
- Redis
- Flyway (for database migration)

### Authentication

- JSON Web Token (JWT)
- OAuth2

### API Documentation

- Swagger (SpringDoc)

### Cloud Services

- Naver Cloud Platform (NCP) Object Storage
- NCP NAT GateWay
- NCP Cloud Functions

### Testing

- JUnit 5
- Testcontainers
- Fixture-Monkey
- H2 Database

### Etc

- Lombok
- Gradle

<br/>

## 🚀 배포 주소

**[Here & Now](https://here-and-now-fe.vercel.app/)**

<br/>

## 📁 프로젝트 구조

```
.
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── meetup
│   │   │           └── hereandnow
│   │   │               ├── connect     # 커플 연결 관련
│   │   │               ├── core        # 공통 모듈 (예외 처리, 보안 등)
│   │   │               ├── course      # 데이트 코스 관련
│   │   │               ├── member      # 사용자 관련
│   │   │               └── place       # 장소 관련
│   │   └── resources
│   └── test
│       └── java
├── build.gradle
└── README.md
```

<br/>

## 📝 API 명세서

**[Swagger API Documentation](https://hereandnow.p-e.kr/swagger-ui/index.html)**

<br/>

## ERD

<img width="600" height="800" alt="image" src="https://github.com/user-attachments/assets/0888981e-16ec-4e79-8307-4c742493063c" />

## System Architecture

<img width="960" height="689" alt="image" src="https://github.com/user-attachments/assets/11b0b811-e3d6-494a-baca-ed7cf498b372" />
