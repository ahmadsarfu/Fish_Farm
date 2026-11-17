package com.fishfarm.management.security;

public final class FarmContext {
    private static final ThreadLocal<Long> FARM = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private FarmContext() {}
    public static long farmId() { return FARM.get() == null ? 0L : FARM.get(); }
    public static long userId() { return USER.get() == null ? 0L : USER.get(); }
    public static String role() { return ROLE.get() == null ? "ANONYMOUS" : ROLE.get(); }
    public static void set(long farmId, long userId, String role) { FARM.set(farmId); USER.set(userId); ROLE.set(role); }
    public static void clear() { FARM.remove(); USER.remove(); ROLE.remove(); }
}
