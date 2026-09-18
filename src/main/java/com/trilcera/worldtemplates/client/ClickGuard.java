package com.trilcera.worldtemplates.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Debounce against input-replay mods (Ixeris).
 * Ixeris queues GLFW mouse callbacks and dispatches them on the render
 * thread, which can deliver the SAME click more than once (original +
 * replayed) within a couple of frames. The replayed dispatch lands on
 * whatever button occupies the same coordinates at that moment, causing:
 * double toggles (config options flip back silently) and instant re-opening
 * of screens (Cancel -> replay hits "Crear Trilcera" on the rebuilt world
 * list -> selector again).
 * Every button handler that flips state or switches screens must call
 * {@link #allow(String)} and ignore the action when it returns false.
 */
public final class ClickGuard {
    private static final long WINDOW_MS = 300;
    private static final Map<String, Long> lastAccepted = new HashMap<>();

    private ClickGuard() {
    }

    /**
     * Returns true at most once per {@link #WINDOW_MS} for the same action id.
     * Additional calls within the window are replayed duplicates.
     */
    public static synchronized boolean allow(String id) {
        long now = System.currentTimeMillis();
        Long prev = lastAccepted.get(id);
        if (prev != null && now - prev < WINDOW_MS) {
            return false;
        }
        lastAccepted.put(id, now);
        return true;
    }
}