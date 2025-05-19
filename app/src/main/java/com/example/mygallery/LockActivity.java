package com.example.mygallery;

import android.content.Intent;
import androidx.biometric.BiometricManager;
import static androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

public class LockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        authenticateUser();
    }

    private void authenticateUser() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "기기 보안 설정이 필요합니다", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
            finish();
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("앱 잠금 해제")
                .setSubtitle("등록된 지문 또는 PIN 으로 인증")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG
                                | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                ) // ✅ 지문 + PIN/패턴/비밀번호 허용
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        // ✅ 인증 성공 처리
                        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
                        prefs.edit().putBoolean("authenticated", true).apply();

                        finish(); // LockActivity 닫기

                        Intent intent = new Intent(LockActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra("from_lock", true); // 인증 경로 표시
                        startActivity(intent);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            finish(); // 인증 성공 후 약간 딜레이 주고 종료
                        }, 300);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        finish(); // 인증 취소 시 종료
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(LockActivity.this, "인증 실패", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }
}
