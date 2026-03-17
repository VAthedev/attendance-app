package protocol;

public enum RequestType {
    // Auth
    LOGIN,
    LOGOUT,
    REGISTER,
    FORGOT_PASSWORD,
    VERIFY_OTP,
    RESET_PASSWORD,

    // TKB
    GET_SCHEDULE_BY_DAY,
    GET_SCHEDULE_BY_WEEK,
    GET_SCHEDULE_BY_SUBJECT,

    // Diem danh
    OPEN_SESSION,
    CLOSE_SESSION,
    CHECKIN_GPS,
    CHECKIN_WIFI,
    GET_ATTENDANCE_HISTORY,
    GET_ATTENDANCE_STATS,

    // Xuat file
    EXPORT_CSV,
    EXPORT_EXCEL,

    // Chat
    SEND_CHAT,
    GET_CHAT_HISTORY,

    // Thong bao
    GET_NOTIFICATIONS,
    MARK_NOTIFICATION_READ,

    // System
    PING,
    ERROR
}
