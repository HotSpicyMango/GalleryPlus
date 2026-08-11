package com.example.mygallery;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GalleryApplication extends Application {
    private int startedActivities = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (startedActivities == 0) {
                    AuthState.markForegrounded();
                }
                startedActivities++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0 && !activity.isChangingConfigurations()) {
                    AuthState.markBackgrounded();
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(@NonNull Context context, @NonNull Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    AuthState.lock();
                }
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }
}
