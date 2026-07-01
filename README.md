# Attendance App - Hệ Thống Quản Lý & Điểm Danh (Tích Hợp AI)

## Tổng quan
Ứng dụng Desktop JavaFX kết hợp với Server Socket xử lý đồng thời tính năng chấm công, quản lý lịch học, thông báo, chatbot AI và nhận diện khuôn mặt.

## Công nghệ sử dụng
| Tầng      | Công nghệ                         |
|-----------|-----------------------------------|
| UI        | JavaFX 21 + FXML                  |
| Mạng      | TCP Socket + TLS/SSL              |
| CSDL      | MongoDB Atlas + MongoDB Driver    |
| Bảo mật   | SHA-256, AES-128, Nonce           |
| Trợ lý AI | LangChain4j + Google Gemini 2.5   |
| AI Face   | Python (FastAPI, dlib, OpenCV)    |
| Xuất file | Apache POI (Excel), OpenCSV       |

## Cấu hình
1. **MongoDB**: Project dùng MongoDB Atlas. URI được cấu hình trong `src/database/DatabaseHelper.java`. (Ghi đè bằng biến môi trường `ATTENDANCE_MONGODB_URI`).
2. **Gemini AI**: Khởi tạo API key trong `VirtualAssistant.java` hoặc sử dụng biến môi trường `GEMINI_API_KEY`.

## Yêu cầu hệ thống
- Java 21 LTS & Maven 3.9+
- Python 3.10+ (Dành cho Server nhận diện khuôn mặt)

---

## Hướng dẫn chạy Ứng dụng

Dự án này sử dụng Maven để quản lý tự động toàn bộ thư viện (MongoDB, LangChain4j, Apache POI, v.v...). Do đó, **bạn nên sử dụng Maven để chạy ứng dụng thay vì gọi lệnh `java` thủ công.**

### 1. Khởi động AI Face Recognition Server (Python)
Chức năng điểm danh khuôn mặt yêu cầu server Python chạy ngầm.
Mở 1 cửa sổ PowerShell mới:
```powershell
cd face_recognition_service
pip install -r requirements.txt
python main.py
```
*(Lưu ý: Quá trình cài đặt thư viện dlib và face_recognition_models có thể mất vài phút).*

### 2. Khởi động Backend Server (Java)
Mở 1 cửa sổ PowerShell mới tại thư mục gốc của dự án:
```powershell
mvn compile
mvn exec:java "-Dexec.mainClass=server.Server"
```

### 3. Khởi động Client App (JavaFX)
Mở 1 cửa sổ PowerShell mới tại thư mục gốc của dự án:
```powershell
mvn compile
mvn exec:java "-Dexec.mainClass=client.Main"
```

---

## Tính năng nổi bật
- **Điểm danh bằng khuôn mặt:** Quét hình ảnh từ Camera, so khớp với CSDL và trả về thông tin qua FastAPI.
- **Trợ lý học vụ AI (Floating Widget):** Tích hợp Google Gemini 2.5 Flash thông qua LangChain4j, giúp sinh viên tra cứu và hỏi đáp tự nhiên.
- **Tương tác thời gian thực:** Mọi thông báo được đẩy ngay lập tức qua Socket.

## Nâng cấp gần đây
- **2026-06-30**: Nâng cấp Chatbot AI lên Google Gemini 2.5 Flash. Tái cấu trúc thành Floating Widget.
- **2026-06-19**: Nâng cấp dự án từ Java 17 lên Java 21 LTS (Long-Term Support).
