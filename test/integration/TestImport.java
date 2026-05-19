package test.integration;

import service.ExcelImportService;

import java.io.File;

public class TestImport {

    public static void main(String[] args) {

        try {

            File excelFile =
                    new File("resources/TKB.xlsx");

            ExcelImportService service =
                    new ExcelImportService();

            // Import lecturers
            service.importUsers(excelFile);

            // Import subjects
            service.importSubjects(excelFile);

            // Import schedules
            service.importSchedules(excelFile);

            System.out.println("START");
            System.out.println("IMPORT USERS...");
            service.importUsers(excelFile);

            System.out.println("IMPORT SUBJECTS...");
            service.importSubjects(excelFile);

            System.out.println("IMPORT SCHEDULES...");
            service.importSchedules(excelFile);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}