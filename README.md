# 📷 MyGallery – Android 사진 갤러리 앱

<img src="https://img.shields.io/badge/platform-Android-blue" />
<img src="https://img.shields.io/badge/language-Java-yellow" />
<img src="https://img.shields.io/badge/minSdk-30-brightgreen" />


**MyGallery**는 Android에서 작고 빠르게 동작하는 개인용 사진 갤러리 앱입니다.  
사용자의 프라이버시와 사용성을 모두 고려해 설계된 이 앱은 다음과 같은 기능을 제공합니다:

---

## ✨ 주요 기능

- **저장된 모든 사진 불러오기**
- **썸네일 보기 & 전체 화면 보기 (PhotoView 기반, 최대 1000배 확대)**
- **좌우 스와이프 이동**
- **날짜별 정렬 및 헤더 표시**
- **핀치 제스처로 한 줄에 표시되는 썸네일 수 조절 (줌 인/아웃)**
- **앱 실행 시 생체 인증 잠금 (지문, 얼굴, PIN 등)**
- **최근 앱 미리보기 차단 (FLAG_SECURE 적용)**
- **minifyEnabled / shrinkResources 설정으로 최적화된 릴리스 빌드**

---

## 🔐 보안 설계

- 앱 실행 시 **기기 등록 생체 인증** 필수
- 홈 화면으로 나갔다가 돌아와도 **반드시 재인증**
- 최근 앱 보기(멀티태스킹 화면)에서 **미리보기 완전 차단**


---

## 🛠️ 기술 스택

- **Language**: Java (Android SDK)
- **UI**: ViewPager2, RecyclerView, ConstraintLayout
- **Libraries**:
  - [Glide](https://github.com/bumptech/glide) – 이미지 로딩
  - [PhotoView](https://github.com/chrisbanes/PhotoView) – 확대/축소 지원
  - [Biometric API](https://developer.android.com/reference/androidx/biometric/package-summary) – 생체 인증

---

## 📦 빌드 정보

```gradle
minifyEnabled true
shrinkResources true
proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'```
