# Gallery+

Android용 개인 사진 갤러리 앱입니다. 사진 목록을 날짜별로 보여주고, 앱 잠금과 전체화면 이미지 뷰어를 제공합니다.

현재 버전: `1.0`

## 주요 기능

- 기기 사진을 날짜별로 그룹화해서 표시
- 최신순/오래된순 정렬 전환
- 날짜 헤더별 사진 개수 표시
- 상단 전체 사진 개수 표시
- 정사각형 썸네일 그리드
- 핀치 제스처로 메인 그리드 열 수 조절
- 사진이 없거나 권한이 없을 때 빈 화면 상태 표시
- Glide 기반 썸네일 placeholder 및 fade-in 로딩

## 전체화면 이미지 뷰어

- ViewPager2 기반 좌우 스와이프
- PhotoView 기반 핀치 확대/축소
- 더블탭 확대/축소
- 한 번 탭으로 버튼, 카운터, 오버레이 표시/숨김
- 확대 상태에서 ViewPager 스와이프 비활성화
- 확대 시 시스템 바와 오버레이를 숨겨 이미지 영역 확장
- 현재 이미지 번호 표시
- 이미지 삭제 지원
- 삭제 후 메인 갤러리 목록 자동 갱신

## 앱 잠금 및 보안

- 앱 실행 시 생체 인증 또는 기기 PIN/패턴/비밀번호 인증
- 앱 전체 생명주기 기반 잠금 상태 관리
- 백그라운드 전환 즉시 재잠금
- 화면이 꺼지면 즉시 잠금
- 메인 화면과 전체화면 이미지 화면에 `FLAG_SECURE` 적용
- 민감 화면은 외부에서 직접 실행되지 않도록 `exported=false` 설정

## 날짜 처리

사진 날짜는 아래 우선순위로 계산합니다.

1. `DATE_TAKEN`
2. `DATE_ADDED`
3. `DATE_MODIFIED`
4. `현재 시각`

## 기술 스택

- Java
- Android SDK
- Material Components
- RecyclerView
- ViewPager2
- ConstraintLayout
- Glide
- PhotoView
- AndroidX Biometric

## 현재 제한사항

- 동영상 뷰어는 아직 별도로 구현되어 있지 않습니다.
- 이미지 삭제는 Android `MediaStore.createDeleteRequest` 흐름을 사용합니다.
- `startIntentSenderForResult` 사용으로 deprecated 경고가 남아 있습니다.
