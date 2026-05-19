package service;

import database.MongoDBConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.MongoCollection;
import com.mongodb.MongoException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * ExcelImportService - Import dữ liệu từ file Excel vào MongoDB
 * Hỗ trợ import: Students, Lecturers, Subjects, Schedules
 */
public class ExcelImportService {

    private final MongoDBConnection mongoConnection;
    private final int batchSize;
    private final int timeoutSeconds;

    public ExcelImportService() {
        this.mongoConnection = MongoDBConnection.getInstance();
        this.batchSize = 100;
        this.timeoutSeconds = 300;
    }

        /**
     * Import lecturers từ file TKB UIT
     */
    public ImportResult importUsers(File excelFile)
            throws IOException {

        ImportResult result =
                new ImportResult(
                        "Users",
                        excelFile.getName()
                );

        List<Document> usersToInsert =
                new ArrayList<>();

        // Tránh duplicate lecturer
        Set<String> importedLecturers =
                new HashSet<>();

        try (FileInputStream fis =
                    new FileInputStream(excelFile);

            Workbook workbook =
                    new XSSFWorkbook(fis)) {

            Sheet sheet =
                    workbook.getSheet("TKB LT");

            MongoCollection<Document> usersCollection =
                    mongoConnection.getUsersCollection();

            // File UIT bắt đầu từ row 8
            for (int i = 8;
                i <= sheet.getLastRowNum();
                i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                try {

                    // ===== Lecturer info =====

                    String lecturerId =
                            getCellValueAsString(row, 4).trim();

                    String lecturerName =
                            getCellValueAsString(row, 5).trim();

                    // Skip rỗng
                    if (lecturerName.isEmpty()) {
                        continue;
                    }

                    // Tránh duplicate
                    if (importedLecturers.contains(lecturerId)) {
                        continue;
                    }

                    importedLecturers.add(lecturerId);

                    // Username tự sinh
                    String username =
                            lecturerId.toLowerCase();

                    // Password mặc định
                    String defaultPassword =
                            "123456";

                    // Hash password
                    String salt =
                            security.SHA256Util.generateSalt();

                    String passwordHash =
                            security.SHA256Util
                                    .hashWithSalt(
                                            defaultPassword,
                                            salt
                                    );

                    // ===== Create user =====

                    Document userDoc =
                            new Document(
                                    "_id",
                                    new ObjectId().toHexString()
                            )

                            .append("username",
                                    username)

                            .append("password_hash",
                                    passwordHash)

                            .append("salt",
                                    salt)

                            .append("role",
                                    "LECTURER")

                            .append("full_name",
                                    lecturerName)

                            .append("lecturer_id",
                                    lecturerId)

                            .append("email",
                                    username + "@uit.edu.vn")

                            .append("device_id",
                                    null)

                            .append("session_token",
                                    null)

                            .append("created_at",
                                    new Date());

                    usersToInsert.add(userDoc);

                    // Batch insert
                    if (usersToInsert.size()
                            >= batchSize) {

                        insertBatch(
                                usersCollection,
                                usersToInsert,
                                result
                        );
                    }

                    result.incrementProcessed();

                } catch (Exception e) {

                    result.addError(
                            i + 1,
                            e.getMessage()
                    );
                }
            }

            // Insert phần còn lại
            if (!usersToInsert.isEmpty()) {

                insertBatch(
                        usersCollection,
                        usersToInsert,
                        result
                );
            }

        } catch (Exception e) {

            result.addError(
                    0,
                    "Error: " + e.getMessage()
            );
        }

        logImportResult(result);

        return result;
    }

    /**
     * Import Subjects từ file TKB UIT
     */
    public ImportResult importSubjects(File excelFile)
            throws IOException {

        ImportResult result =
                new ImportResult(
                        "Subjects",
                        excelFile.getName()
                );

        List<Document> subjectsToInsert =
                new ArrayList<>();

        // Dùng để tránh duplicate
        Set<String> importedCodes =
                new HashSet<>();

        try (FileInputStream fis =
                    new FileInputStream(excelFile);

            Workbook workbook =
                    new XSSFWorkbook(fis)) {

            Sheet sheet =
                    workbook.getSheet("TKB LT");

            MongoCollection<Document> subjectsCollection =
                    mongoConnection.getSubjectsCollection();

            // File UIT bắt đầu data từ row 8
            for (int i = 8;
                i <= sheet.getLastRowNum();
                i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                try {

                    // ===== Mapping =====

                    String code =
                            getCellValueAsString(row, 1).trim();

                    String name =
                            getCellValueAsString(row, 3).trim();

                    String lecturerName =
                            getCellValueAsString(row, 5).trim();

                    String totalCredits =
                            getCellValueAsString(row, 7).trim();

                    String practiceCredits =
                            getCellValueAsString(row, 8).trim();

                    String managingFaculty =
                            getCellValueAsString(row, 18).trim();

                    // ===== Validate =====

                    if (code.isEmpty()
                            || name.isEmpty()) {

                        result.addError(
                                i + 1,
                                "Code hoặc name bị trống"
                        );

                        continue;
                    }

                    // ===== Skip duplicate =====

                    if (importedCodes.contains(code)) {
                        continue;
                    }

                    importedCodes.add(code);

                    // ===== Create document =====

                    Document subjectDoc =
                            new Document(
                                    "_id",
                                    new ObjectId().toHexString()
                            )

                            .append("code", code)

                            .append("name", name)

                            .append("lecturer_name",
                                    lecturerName)

                            .append("total_credits",
                                    totalCredits)

                            .append("practice_credits",
                                    practiceCredits)

                            .append("managing_faculty",
                                    managingFaculty)

                            .append("created_at",
                                    new Date());

                    subjectsToInsert.add(subjectDoc);

                    // ===== Batch insert =====

                    if (subjectsToInsert.size()
                            >= batchSize) {

                        insertBatch(
                                subjectsCollection,
                                subjectsToInsert,
                                result
                        );
                    }

                    result.incrementProcessed();

                } catch (Exception e) {

                    result.addError(
                            i + 1,
                            e.getMessage()
                    );
                }
            }

            // Insert phần còn lại
            if (!subjectsToInsert.isEmpty()) {

                insertBatch(
                        subjectsCollection,
                        subjectsToInsert,
                        result
                );
            }

        } catch (Exception e) {

            result.addError(
                    0,
                    "Error: " + e.getMessage()
            );
        }

        logImportResult(result);

        return result;
    }

    /**
     * Import Schedules từ file TKB UIT
     * Sheet: TKB LT
     */
    public ImportResult importSchedules(File excelFile) throws IOException {

        ImportResult result =
                new ImportResult("Schedules", excelFile.getName());

        List<Document> schedulesToInsert =
                new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook = new XSSFWorkbook(fis)) {

            // Sheet lý thuyết
            Sheet sheet = workbook.getSheet("TKB LT");

            MongoCollection<Document> schedulesCollection =
                    mongoConnection.getSchedulesCollection();

            // Bắt đầu từ dòng 9 trong Excel
            // (index 8)
            for (int i = 8; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                try {

                    // ===== Mapping đúng theo file Excel =====

                    String subjectCode =
                            getCellValueAsString(row, 1).trim();

                    String classCode =
                            getCellValueAsString(row, 2).trim();

                    String subjectName =
                            getCellValueAsString(row, 3).trim();

                    String lecturerId =
                            getCellValueAsString(row, 4).trim();

                    String lecturerName =
                            getCellValueAsString(row, 5).trim();

                    String classSize =
                            getCellValueAsString(row, 6).trim();

                    String totalCredits =
                            getCellValueAsString(row, 7).trim();

                    String practiceCredits =
                            getCellValueAsString(row, 8).trim();

                    String educationType =
                            getCellValueAsString(row, 9).trim();

                    String dayOfWeek =
                            getCellValueAsString(row, 10).trim();

                    String periods =
                            getCellValueAsString(row, 11).trim();

                    String alternateWeeks =
                            getCellValueAsString(row, 12).trim();

                    String room =
                            getCellValueAsString(row, 13).trim();

                    String course =
                            getCellValueAsString(row, 14).trim();

                    String semester =
                            getCellValueAsString(row, 15).trim();

                    String schoolYear =
                            getCellValueAsString(row, 16).trim();

                    String educationSystem =
                            getCellValueAsString(row, 17).trim();

                    String managingFaculty =
                            getCellValueAsString(row, 18).trim();

                    String startDate =
                            getCellValueAsString(row, 19).trim();

                    String endDate =
                            getCellValueAsString(row, 20).trim();

                    // Skip dòng rỗng
                    if (subjectCode.isEmpty()
                            || classCode.isEmpty()) {

                        result.addError(i + 1,
                                "Subject code hoặc class code bị trống");

                        continue;
                    }

                    // ===== Tạo document =====

                    Document scheduleDoc =
                            new Document("_id",
                                    new ObjectId().toHexString())

                            .append("subject_code", subjectCode)

                            .append("class_code", classCode)

                            .append("subject_name", subjectName)

                            .append("lecturer_id", lecturerId)

                            .append("lecturer_name", lecturerName)

                            .append("class_size", classSize)

                            .append("total_credits", totalCredits)

                            .append("practice_credits", practiceCredits)

                            .append("education_type", educationType)

                            .append("day_of_week", dayOfWeek)

                            .append("periods", periods)

                            .append("alternate_weeks", alternateWeeks)

                            .append("room", room)

                            .append("course", course)

                            .append("semester", semester)

                            .append("school_year", schoolYear)

                            .append("education_system", educationSystem)

                            .append("managing_faculty", managingFaculty)

                            .append("start_date", startDate)

                            .append("end_date", endDate)

                            .append("created_at", new Date());

                    schedulesToInsert.add(scheduleDoc);

                    // ===== Batch insert =====

                    if (schedulesToInsert.size() >= batchSize) {

                        insertBatch(
                                schedulesCollection,
                                schedulesToInsert,
                                result
                        );
                    }

                    result.incrementProcessed();

                } catch (Exception e) {

                    result.addError(
                            i + 1,
                            e.getMessage()
                    );
                }
            }

            // Insert phần còn lại
            if (!schedulesToInsert.isEmpty()) {

                insertBatch(
                        schedulesCollection,
                        schedulesToInsert,
                        result
                );
            }

        } catch (Exception e) {

            result.addError(
                    0,
                    "Error: " + e.getMessage()
            );
        }

        logImportResult(result);

        return result;
    }

    /**
     * Insert batch dữ liệu vào MongoDB
     */
    private void insertBatch(MongoCollection<Document> collection, List<Document> batch, ImportResult result) {
        try {
            collection.insertMany(batch);
            result.addInserted(batch.size());
            batch.clear();
        } catch (MongoException e) {
            if (e.getMessage().contains("duplicate")) {
                result.addSkipped(batch.size());
                batch.clear();
            } else {
                throw e;
            }
        }
    }

    /**
     * Lấy giá trị cell dạng String
     */
    private String getCellValueAsString(Row row, int cellIndex) {

        org.apache.poi.ss.usermodel.Cell cell =
                row.getCell(cellIndex);

        if (cell == null) {
            return "";
        }

        DataFormatter formatter =
                new DataFormatter();

        return formatter.formatCellValue(cell).trim();
    }

    /**
     * Lấy giá trị cell dạng Double
     */
    private double getCellValueAsDouble(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) {
            return 0;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        try {
            return Double.parseDouble(getCellValueAsString(row, cellIndex));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Log kết quả import
     */
    private void logImportResult(ImportResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📊 IMPORT RESULT: " + result.type);
        System.out.println("=".repeat(60));
        System.out.println("File: " + result.fileName);
        System.out.println("Processed: " + result.processed + " rows");
        System.out.println("Inserted: " + result.inserted);
        System.out.println("Skipped: " + result.skipped);
        System.out.println("Errors: " + result.errors.size());

        if (!result.errors.isEmpty()) {
            System.out.println("\n❌ Error Details:");
            for (Map.Entry<Integer, String> error : result.errors.entrySet()) {
                System.out.println("  Row " + error.getKey() + ": " + error.getValue());
            }
        }
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Lớp ImportResult - Chứa kết quả import
     */
    public static class ImportResult {
        public String type;
        public String fileName;
        public int processed = 0;
        public int inserted = 0;
        public int skipped = 0;
        public Map<Integer, String> errors = new LinkedHashMap<>();

        public ImportResult(String type, String fileName) {
            this.type = type;
            this.fileName = fileName;
        }

        public void incrementProcessed() { this.processed++; }
        public void addInserted(int count) { this.inserted += count; }
        public void addSkipped(int count) { this.skipped += count; }
        public void addError(int row, String message) {
            this.errors.put(row, message);
        }

        public boolean isSuccess() {
            return errors.isEmpty() && inserted > 0;
        }
    }
}
