# SSAFY 7기 자율 프로젝트 A101
---



# Unity 작업 내용
| Unity 작업사항이 Git Commit으로 남지 않아 작업 이력을 작성합니다.

## Thinking
### 가구배치 UX/UI

pc로 할수도 있고 모바일로 할 수도 있음

모바일의 경우 멀티터치 가능

pc의 경우 마우스, 키보드 사용 가능

### 가구배치 모드 진입 방법

- 인벤토리를 열고 해당 가구를 누르면 배치모드로 진입
    - 배치모드 진입과 동시에 이 가구를 핸들링
    - 바로 방 중심에 배치해버림
- UI의 배치모드 버튼을 통해 진입
    - 이 상태에서 가구를 누르거나 현재 배치된 가구리스트 아이콘을 눌러서 해당 가구 핸들링

### 가구배치 모드

- 현재 배치하려는 가구 아이콘 표시
    - 가구를 삭제하는 버튼 표시
- 상단에 조작 타입 설정 UI 표시(각 배치모드별 설명 등장)
    - 가구 선택 모드(누른 가구를 배치 대상으로 설정)
    - 터치한 곳에 배치(새롭게 배치하는 경우 일단 이 모드로 배치해야함)
        - 가구는 기본 각도로 배치
        - 표면 배치 모드 켤 경우 노멀 벡터 계산해서 자동으로 배치
    - 좌표 축 이동
        - 일반터치로 x,y 좌표축 이동, z축은 우클릭 혹은 두 손가락 터치
    - 각도 조정
        - 이건 각도조정 API같은거 가져와야할듯?
- 진입 시 원래 위치정보 기억해둠
    - 확인 누르면 현재 바꾸려는 위치로 이동
    - 취소 누르면 원래 위치로 롤백, 배치모드 나가기
    - 삭제 누르면 이 가구 삭제



### 할일

- [ ]  모드별 구분(normal, put, cam)
    - [x]  normal
    - [x]  put
    - [ ]  cam
- [x]  Pick모드에선 인벤 안없어지게
- [x]  풋모드 ui 변경
    - [x]  put모드일때만 exitPanel 나오게
    - [x]  pick모드에서 설명과 선택했을 시 하단 배치모드 진입 UI
    - [x]  인벤토리에서 가구눌렀을때 pick상태 ui뜸
    - [x]  풋모드 on 상태 표시와 나가기버튼
    - [x]  모드변경 버튼
    - [x]  각도변경 패드
    - [x]  되돌리기 버튼
    - [x]  초기 인벤토리에서 꺼낼 때 각도 틀기
    - [x]  선택오브젝트 테두리
- [ ]  inspector에 public 변수들 구분지어놓기
- [ ]  정리되면 주석달기
- [ ]  프리뷰 처음 접근 시 null exception 간헐적으로 발생하는 버그
- [ ]  2점 터치 인식해서 기능 작동시키기