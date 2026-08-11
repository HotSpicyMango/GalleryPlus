package com.example.mygallery;

public final class AuthState {
    private static final long LOCK_TIMEOUT_MILLIS = 30_000L;

    private static boolean authenticated = false;
    private static long backgroundAtMillis = 0L;

    private AuthState() {
    }

    public static void unlock() {
        authenticated = true;
        backgroundAtMillis = 0L;
    }

    public static void lock() {
        authenticated = false;
        backgroundAtMillis = 0L;
    }

    public static void markBackgrounded() {
        if (authenticated) {
            backgroundAtMillis = System.currentTimeMillis();
        }
    }

    public static void markForegrounded() {
        backgroundAtMillis = 0L;
    }

    public static boolean shouldRequireUnlock() {
        if (!authenticated) {
            return true;
        }

        if (backgroundAtMillis == 0L) {
            return false;
        }

        return System.currentTimeMillis() - backgroundAtMillis >= LOCK_TIMEOUT_MILLIS;
    }
}
