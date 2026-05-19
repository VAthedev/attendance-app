package integration;

import service.EnrollmentGeneratorService;

/**
 * GenerateEnrollments - Tạo 200 enrollment random vào MongoDB
 */
public class GenerateEnrollments {

    public static void main(String[] args) {

        try {
            System.out.println("========================================");
            System.out.println("🚀 BẮTĐẦU GENERATE 200 ENROLLMENTS");
            System.out.println("========================================\n");

            EnrollmentGeneratorService service =
                    new EnrollmentGeneratorService();

            // Check hiện tại
            long currentCount = service.countEnrollments();
            System.out.println("📊 Enrollments hiện tại: " + currentCount);

            // Clear cũ (tuỳ chọn)
            if (currentCount > 0) {
                System.out.println("\n⚠️  Xóa enrollments cũ...");
                service.clearEnrollments();
            }

            // Generate mới
            System.out.println("\n📝 Generating 200 random enrollments...");
            service.generateRandomEnrollments();

            // Kiểm tra lại
            long newCount = service.countEnrollments();
            System.out.println("📊 Enrollments sau generate: " + newCount);

            System.out.println("\n========================================");
            System.out.println("✅ GENERATE HOÀN TẤT");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
