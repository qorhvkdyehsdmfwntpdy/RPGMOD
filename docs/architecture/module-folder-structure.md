# RPGMOD 모듈 폴더 구조 (경량 + 대형 확장 대응)

## 현재 구조
- `core/module`
  - `RpgRuntimeModule`: 모듈 공통 인터페이스
  - `RpgModuleRegistry`: 모듈 등록/리로드/상태조회
- `permission/*`: permission-core 구현
- `classes/*`, `proficiency/*`, `progression/*`: progression-core 구현
- `weapon/*`: itemization-core 구현

## 권장 확장 구조
- `content/*`: 퀘스트/NPC/보스
- `economy/*`: 화폐/거래/경매
- `guild/*`: 길드/칭호/전쟁
- `admin/*`: 운영 대시보드/감사로그 확장

## 설계 기준
- 모듈 외부에는 서비스 인터페이스만 노출
- JSON 리포지토리와 런타임 서비스 분리
- 리로드 경로를 모듈 단위로 고정(`rpgdev modules reload`)
