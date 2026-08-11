# Implementation Plan - Fix Warnings and Errors in MainActivity.java

This plan addresses several issues in `MainActivity.java` and related files to improve code quality, performance, and compatibility.

## User Review Required

> [!IMPORTANT]
> **Permission Changes**: I will be adding `READ_EXTERNAL_STORAGE` to the `AndroidManifest.xml` to support devices running Android 11 and 12 (API 30-32), as the current implementation only handles Android 13+ (API 33+).

> [!NOTE]
> **Threading**: The image loading logic will be moved to a background thread to prevent the UI from freezing (ANR) while scanning the gallery.

## Proposed Changes

### [MainActivity](file:///Users/ohmingyu/GalleryPlus/app/src/main/java/com/example/mygallery/MainActivity.java)

#### [MODIFY] [MainActivity.java](file:///Users/ohmingyu/GalleryPlus/app/src/main/java/com/example/mygallery/MainActivity.java)
- **Remove Unused Code**: Delete the unused `titleText` variable.
- **Simplify Lifecycle/UI**: Remove redundant API level checks for status bar (minSdk is already 30).
- **Fix Permissions**: Update `requestPermissions` logic to handle `READ_EXTERNAL_STORAGE` on API < 33.
- **Background Loading**: Wrap `loadImages()` logic in a background thread using `Executor` and update the adapter on the main thread.
- **Accessibility**: Add `view.performClick()` to `OnTouchListener` to resolve lint warnings.
- **Constant Usage**: Replace hardcoded view type `0` with `ImageAdapter.TYPE_HEADER`.

---

### [ImageAdapter](file:///Users/ohmingyu/GalleryPlus/app/src/main/java/com/example/mygallery/ImageAdapter.java)

#### [MODIFY] [ImageAdapter.java](file:///Users/ohmingyu/GalleryPlus/app/src/main/java/com/example/mygallery/ImageAdapter.java)
- **Expose Constants**: Change `TYPE_HEADER` and `TYPE_IMAGE` from `private` to `public`.
- **Fix Touch Listener**: Add `v.performClick()` in `onBindViewHolder` for accessibility.

---

### [Manifest](file:///Users/ohmingyu/GalleryPlus/app/src/main/AndroidManifest.xml)

#### [MODIFY] [AndroidManifest.xml](file:///Users/ohmingyu/GalleryPlus/app/src/main/AndroidManifest.xml)
- **Add Permission**: Add `<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />`.

## Verification Plan

### Automated Tests
- N/A (No existing unit tests, but can be added later).

### Manual Verification
- Deploy to an emulator/device.
- Verify that permissions are requested correctly on first launch.
- Verify that images load without freezing the UI.
- Test the pinch-to-zoom (span count) feature.
- Verify that sorting (descending/ascending) works correctly.
