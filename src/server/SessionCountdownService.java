package server;

import database.SessionRepository;
import org.bson.Document;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionCountdownService {

    private static SessionCountdownService instance;
    private final ScheduledExecutorService scheduler;

    private SessionCountdownService() {
        // Tạo một thread pool nhỏ để xử lý các task đếm ngược
        this.scheduler = Executors.newScheduledThreadPool(5);
    }

    public static synchronized SessionCountdownService getInstance() {
        if (instance == null) {
            instance = new SessionCountdownService();
        }
        return instance;
    }

    /**
     * Lên lịch tự động đóng phiên điểm danh sau X phút
     */
    public void scheduleSessionClose(String sessionId, int durationMinutes) {
        if (durationMinutes <= 0) return;

        scheduler.schedule(() -> {
            try {
                // Kiem tra trang thai hien tai cua phien
                Document session = SessionRepository.getInstance().findById(sessionId);
                if (session != null && "OPEN".equals(session.getString("status"))) {
                    // Dong phien
                    boolean closed = SessionRepository.getInstance().closeSession(sessionId);
                    if (closed) {
                        System.out.println("[SessionCountdown] Auto-closed session: " + sessionId);
                        
                        // Finalize attendance
                        new service.AttendanceService().finalizeSessionAttendance(sessionId);
                        
                        // Broadcast cho toan bo sinh vien
                        BroadcastManager.getInstance().broadcastAnnouncement("SESSION_CLOSED:" + sessionId);
                    }
                }
            } catch (Exception e) {
                System.err.println("[SessionCountdown] Loi khi tu dong dong phien: " + e.getMessage());
            }
        }, durationMinutes, TimeUnit.MINUTES);
        
        System.out.println("[SessionCountdown] Scheduled close for session " + sessionId + " in " + durationMinutes + " minutes");
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
