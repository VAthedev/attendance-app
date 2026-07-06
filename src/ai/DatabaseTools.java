package ai;

import client.network.ServerApi;
import dev.langchain4j.agent.tool.Tool;
import protocol.RequestType;

public class DatabaseTools {

    @Tool("Lay danh sach cac mon hoc ma sinh vien dang dang ky dua vao ma sinh vien studentId")
    public String getRegisteredSubjects(String studentId) {
        return queryServer("registeredSubjects", studentId);
    }

    @Tool("Lay lich hoc chi tiet cua cac mon hoc ma sinh vien da dang ky dua vao studentId")
    public String getStudentSchedule(String studentId) {
        return queryServer("studentSchedule", studentId);
    }

    @Tool("Lay thong tin diem danh cua sinh vien dua vao studentId")
    public String getStudentAttendanceSummary(String studentId) {
        return queryServer("studentAttendanceSummary", studentId);
    }

    @Tool("Lay diem thi va diem tong ket cua sinh vien dua vao studentId")
    public String getStudentGrades(String studentId) {
        return "[He thong diem so dang bao tri] Du lieu diem thi chua duoc dong bo tu phong dao tao.";
    }

    @Tool("Lay lich thi cuoi ky cua sinh vien dua vao studentId")
    public String getExamSchedules(String studentId) {
        return "Lich thi chinh thuc se duoc cong bo truoc ngay thi. Hien chua co lich thi moi trong he thong.";
    }

    @Tool("Lay thong tin ke hoach dao tao nam hoc 2026-2027 cua truong")
    public String getAcademicCalendar() {
        return "Nam hoc 2026-2027 gom hoc ky 1, hoc ky 2 va hoc ky he. Lich dang ky hoc phan, thi va nghi le se duoc cap nhat theo thong bao cua phong dao tao.";
    }

    @Tool("Lay thong tin lien lac email va Microsoft Teams cua giang vien dua vao ten")
    public String getLecturerContactInfo(String lecturerName) {
        if (lecturerName == null || lecturerName.trim().isEmpty()) {
            return "Khong co ten giang vien de tao email.";
        }

        String normalized = java.text.Normalizer.normalize(lecturerName.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replace("\u0111", "d");

        String[] parts = normalized.split("\\s+");
        if (parts.length < 2) {
            return "Ten giang vien khong hop le.";
        }

        String givenName = parts[parts.length - 1];
        StringBuilder prefix = new StringBuilder(givenName);
        prefix.append(parts[0].charAt(0));
        for (int i = 1; i < parts.length - 1; i++) {
            prefix.append(parts[i].charAt(0));
        }

        String account = prefix.toString();
        return "Email: " + account + "@uit.edu.vn\nMicrosoft Teams: " + account + "@hcmuit.edu.vn";
    }

    @Tool("Lay lich day cua giang vien trong tuan nay dua vao lecturerName")
    public String getLecturerSchedule(String lecturerName) {
        return queryServer("lecturerSchedule", lecturerName);
    }

    private String queryServer(String tool, String input) {
        try {
            protocol.Response res = ServerApi.send(RequestType.AI_TOOL_QUERY,
                    java.util.Map.of(
                            "tool", tool != null ? tool : "",
                            "input", input != null ? input : ""));
            if (!res.isOk()) {
                return "Khong lay duoc du lieu tu server: " + res.getMessage();
            }
            String result = res.getDataValue("result");
            return result != null && !result.isBlank() ? result : "Server khong tra ve du lieu.";
        } catch (Exception e) {
            return "Loi ket noi server khi lay du lieu: " + e.getMessage();
        }
    }
}
