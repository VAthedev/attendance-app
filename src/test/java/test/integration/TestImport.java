package test.integration;

import java.io.File;

import service.ExcelImportService;
import service.ExcelImportService.ImportResult;

public class TestImport {

    public static void main(String[] args) {

        try {
            File excelFile = new File("resources/TKB.xlsx");

            // Kiểm tra file tồn tại
            if (!excelFile.exists()) {
                System.err.println("❌ Lỗi: File không tồn tại: " + excelFile.getAbsolutePath());
                return;
            }

            ExcelImportService service = new ExcelImportService();

            System.out.println("========================================");
            System.out.println("🚀 BẮTĐẦU ĐẢYDATA TỪ EXCEL LÊN MONGODB");
            System.out.println("========================================\n");

            // Import lecturers
            System.out.println("📝 IMPORT USERS (Giáo viên)...");
            ImportResult usersResult = service.importUsers(excelFile);
            System.out.println("✅ Kết quả: " + usersResult.inserted + " users được thêm\n");

            // Import subjects
            System.out.println("📝 IMPORT SUBJECTS (Môn học)...");
            ImportResult subjectsResult = service.importSubjects(excelFile);
            System.out.println("✅ Kết quả: " + subjectsResult.inserted + " subjects được thêm\n");

            // Import schedules
            System.out.println("📝 IMPORT SCHEDULES (Lịch học)...");
            ImportResult schedulesResult = service.importSchedules(excelFile);
            System.out.println("✅ Kết quả: " + schedulesResult.inserted + " schedules được thêm\n");

            System.out.println("========================================");
            System.out.println("🎉 ĐẢY DATA THÀNH CÔNG");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi đảy data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}