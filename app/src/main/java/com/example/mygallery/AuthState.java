package com.example.mygallery;

public final class AuthState {
    private static boolean authenticated = false;

    private AuthState() {
    }

    public static void unlock() {
        authenticated = true;
    }

    public static void lock() {
        authenticated = false;
    }

    public static void markBackgrounded() {
        lock();
    }

    public static boolean shouldRequireUnlock() {
        return !authenticated;
    }
}
