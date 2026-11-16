# Design Pattern Team 6 — Life Game

Allen Holub의 Game of Life(HolubLife.jar v1.02, 2005)를 디자인 패턴으로 개선하는 프로젝트입니다.

**팀원 안내: [`docs/team-guide.html`](docs/team-guide.html)** (브라우저로 열기). 원본의 문제, core가 바꾼 구조, 담당자별로 고민할 것과 남은 결함, 함께 지킬 것이 있습니다.

## 폴더 구조

```
src/com/holub/
├── life/          Life (객체 생성·연결)
│   ├── model/     모델: LifeModel, BoardView, Rule, Boundary, … (Composite는 package-private)
│   ├── view/      화면: UniversePanel
│   ├── control/   시계·메뉴: Clock, ClockMenu
│   └── storage/   파일 입출력: BoardFileIO
│       └── format/  저장 형식: BoardSerializer, JavaSerializationFormat
├── ui/            원본 라이브러리: MenuSite, Colors
├── tools/         원본 라이브러리: Publisher, Log
└── io/            원본 라이브러리: Files (파일 선택 창)
test/com/holub/life/   JUnit 5 테스트 (src와 같은 패키지 구조), bench/ 벤치마크, GuiSmokeCheck
docs/team-guide.html   팀원 안내
scripts/               실행·테스트 스크립트
```

## 실행

- 실행: `scripts/run.sh` (컴파일과 실행을 한 번에). IDE에서는 `src`를 소스 폴더로 지정하고 `com.holub.life.Life` 실행
- 테스트: `scripts/test.sh` (JDK 17 이상, 처음 한 번 JUnit 5 jar를 받아 옴)
- GUI 점검: `scripts/gui-check.sh` (실제 창을 띄워 클릭·메뉴·저장/불러오기를 자동 점검, 결과는 `test-output/gui-check/`)
- 성능 측정: `scripts/bench.sh` 또는 `scripts/bench.sh 0.01` (셀 밀도)
