# 1주차 구현 결과

## 완료 항목
- 라이선스 안전 문서화
  - `docs/architecture/license-safe-scope.md`
- 모듈 레지스트리 도입
  - `core/module/RpgRuntimeModule`
  - `core/module/RpgModuleRegistry`
  - 모듈: `permission-core`, `progression-core`, `itemization-core`
- 운영 커맨드 추가
  - `/rpgdev modules`
  - `/rpgdev modules reload all`
  - `/rpgdev modules reload <moduleId>`
  - `/rpgdev license`
- 성장 수식 엔진 1차
  - `config/rpgmod/progression-formulas.json`
  - 전직 레벨 요구치 배율 적용
  - 숙련도 레벨업 exp 배율 적용
- 아이템화 1차(무기)
  - `weapons.json`에 `affixPool`, `socket` 지원
  - 드랍 시 affix/socket 롤링
  - 툴팁에 롤 결과 표기

## 2주차 추천
- 아이템 롤 결과를 강화/재련 시스템과 연결
- affix 가중치(희귀도별)와 금지 조합 룰 추가
- 모듈별 메트릭(로딩시간/캐시크기/리로드횟수) 수집
