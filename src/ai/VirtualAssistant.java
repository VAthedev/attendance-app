package ai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

public class VirtualAssistant {

    // Define the interface for LangChain4j AI Service
    public interface ChatAssistant {
        @SystemMessage({
            "Bạn là trợ lý ảo AI hỗ trợ học vụ cho sinh viên và giảng viên của trường đại học.",
            "Nhiệm vụ của bạn là giải đáp các thắc mắc về lịch học, điểm danh, môn học.",
            "Hãy trả lời ngắn gọn, súc tích và thân thiện bằng tiếng Việt.",
            "Luôn kiểm tra ngữ cảnh người dùng cung cấp (nếu có) trước khi trả lời."
        })
        String chat(String userMessage);
    }

    private ChatAssistant assistant;

    public VirtualAssistant() {
        init();
    }

    private String getApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            return apiKey;
        }
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                for (String line : lines) {
                    if (line.trim().startsWith("GEMINI_API_KEY=")) {
                        return line.split("=", 2)[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi đọc file .env: " + e.getMessage());
        }
        // Fallback khẩn cấp nếu không đọc được file .env
        return "<YOUR_GEMINI_API_KEY_HERE>";
    }

    private void init() {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("CẢNH BÁO: Chưa thiết lập GEMINI_API_KEY trong biến môi trường hoặc file .env.");
            return;
        }

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash") // Đã cập nhật lên model mới nhất
                .build();

        // Khởi tạo AI Service kết hợp với DatabaseTools
        this.assistant = AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(new DatabaseTools())
                .build();
    }

    public String ask(String message, String userId) {
        if (this.assistant == null) {
            return "Xin lỗi, Chatbot chưa được khởi tạo đúng cách (thiếu API Key).";
        }
        
        // Đính kèm userId vào câu hỏi để bot có ngữ cảnh
        String contextualMessage = String.format("[Người dùng hiện tại có ID: %s] %s", userId, message);
        try {
            return this.assistant.chat(contextualMessage);
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            if (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED")) {
                return "API Key của Chatbot đã hết hạn mức sử dụng (Lỗi 429). Vui lòng tạo API Key mới tại Google AI Studio và cập nhật vào mã nguồn để tiếp tục sử dụng.";
            }
            return "Lỗi khi gọi Chatbot: " + errorMsg;
        }
    }
}
