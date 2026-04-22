# Attendance App - He thong TKB va Diem danh

## Tong quan
Ung dung desktop JavaFX + server socket cho cham cong, lich hoc va thong bao.

## Cong nghe su dung
| Tang      | Cong nghe                         |
|-----------|-----------------------------------|
| UI        | JavaFX 21 + FXML                  |
| Mang      | TCP Socket + TLS/SSL              |
| CSDL      | MongoDB Atlas + MongoDB Driver    |
| Bao mat   | SHA-256, AES-128, Nonce           |
| Email     | JavaMail (SMTP)                   |
| Xuat file  | Apache POI (Excel), OpenCSV       |

## Cau hinh MongoDB
Project dung MongoDB Atlas. URI mac dinh da duoc cau hinh trong [src/database/DatabaseHelper.java](src/database/DatabaseHelper.java).

Neu muon doi URI ma khong sua code, set bien moi truong `ATTENDANCE_MONGODB_URI` truoc khi chay server.

## Thu vien can co trong `lib/`
- `mongodb-driver-sync-4.11.1.jar`
- `mongodb-driver-core-4.11.1.jar`
- `bson-4.11.1.jar`
- JavaFX SDK 21 trong `lib/javafx-sdk-21/`

## Cach chay
### Trong VS Code
1. Mo `Run and Debug`.
2. Chon `Launch Server`.
3. Chay `Launch Client (JavaFX)`.

### Bang PowerShell
Compile:
```powershell
$files = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -cp "lib\*" -d bin $files
```

Chay server:
```powershell
java -cp "bin;lib\*" server.Server
```

Chay client:
```powershell
java --module-path "lib\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\*" client.Main
```

## Yeu cau
- Java 21+
- JavaFX SDK 21
- MongoDB Atlas hoac MongoDB compatible connection string
- VS Code + Extension Pack for Java

## Ghi chu
- Neu VS Code chua nhan jar moi, chay lenh `Java: Clean Java Language Server Workspace` va reload workspace.
- Thu muc `bin/` duoc dung de chua class da compile.

## Thanh vien nhom
| Ten | MSSV | Phan cong |
|-----|------|-----------|
|     |      |           |
