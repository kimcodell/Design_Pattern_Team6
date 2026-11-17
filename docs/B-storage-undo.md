# B - 저장과 되돌리기

- 원본 문제: 저장 형식이 Neighborhood 안의 Java 직렬화로 고정, 좌표를 두 번 기록, 불러온 셀이 활성 표시를 안 함, 오류를 Universe가 JOptionPane으로 직접 표시
core가 바꾼 것:

    3계층: BoardSnapshot(불변, 크기 + 산 셀 좌표) ← BoardSerializer(형식) ← BoardFileIO(확장자로 형식 선택, 파일 열고 닫기)
    LifeModel.snapshot() / restore(). 두 번 기록·불러오기 활성 표시 문제 해결
    원본 .life 파일 호환 형식 하나(JavaSerializationFormat)
    편집 단위 BoardEdit (여러 셀을 한 번에 바꾸고 알림은 한 번)

- 대안 후보: GoF Memento(바깥에서 내용을 못 봄) vs 지금의 공개 Snapshot / 형식 선택을 확장자 맵, Factory, 파일 내용 감지 중 무엇으로
고민할 것:

    스냅샷에 규칙·경계까지 넣을지 (지금은 보드 상태만)
    저장·불러오기 오류를 어디서 사용자에게 보여 줄지

- 남은 결함:

    설계: 저장·불러오기 오류를 Life.installGridMenu가 JOptionPane으로 직접 표시 (원본 코드를 그대로 옮긴 상태)

---
- 담당: 정영묵 (GreenBlue7)
- 상태: 적용 패턴/설계 검토 중 (구현 전)
- 기준: [`team-guide.html`](team-guide.html) 03-B

## 아직 정하지 않은 것

- 되돌리기 기록 단위 (클릭 하나, 편집 묶음, 불러오기 등)
- 되돌리기에 쓸 상태를 무엇으로 담을지: GoF Memento vs 지금의 공개 `BoardSnapshot`
- 스냅샷에 규칙·경계까지 넣을지
- 저장·불러오기 오류를 사용자에게 보여 주는 위치
