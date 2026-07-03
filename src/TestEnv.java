import ai.VirtualAssistant;
public class TestEnv {
    public static void main(String[] args) {
        try {
            java.io.File envFile = new java.io.File(".env");
            System.out.println(".env exists: " + envFile.exists());
            System.out.println("Absolute path: " + envFile.getAbsolutePath());
            VirtualAssistant va = new VirtualAssistant();
            System.out.println("VA initialized");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
