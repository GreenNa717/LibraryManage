package library.common;

public final class RoleConstants {
    private RoleConstants() {}

    public static final int SUPER_ADMIN = 0;
    public static final int USER = 1;
    public static final int ASSISTANT_ADMIN = 2;
    public static final int GUEST = 3;

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_ASSISTANT_ADMIN = "ASSISTANT_ADMIN";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_GUEST = "GUEST";

    public static boolean isSuperAdmin(Integer role) {
        return role != null && role == SUPER_ADMIN;
    }

    public static boolean isAssistantAdmin(Integer role) {
        return role != null && role == ASSISTANT_ADMIN;
    }

    public static boolean isUser(Integer role) {
        return role != null && role == USER;
    }

    public static boolean isGuest(Integer role) {
        return role != null && role == GUEST;
    }

    public static boolean isAdmin(Integer role) {
        return isSuperAdmin(role) || isAssistantAdmin(role);
    }

    public static boolean isValid(Integer role) {
        return isSuperAdmin(role) || isAssistantAdmin(role) || isUser(role) || isGuest(role);
    }

    public static String toRoleName(Integer role) {
        if (role == null || !isValid(role)) {
            throw new IllegalArgumentException("无效的用户角色: " + role);
        }
        if (isSuperAdmin(role)) return ROLE_SUPER_ADMIN;
        if (isAssistantAdmin(role)) return ROLE_ASSISTANT_ADMIN;
        if (isUser(role)) return ROLE_USER;
        return ROLE_GUEST;
    }
}
