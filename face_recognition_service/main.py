import os
import cv2
import face_recognition
import numpy as np
from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse

app = FastAPI(title="Attendance Face Recognition API")

# ---------------------------------------------------------------------------
# FIX TC-FAC-004 / TC-FAC-005 / TC-FAC-006:
#   - CONFIDENCE_THRESHOLD: ngưỡng tối thiểu để chấp nhận nhận diện
#   - Mọi error path đều trả JSON thay vì HTTPException text → Java không crash
#   - confidence score được trả về để Java audit chất lượng nhận diện
# ---------------------------------------------------------------------------

# Tolerance=0.5 → distance ≤ 0.5 → confidence ≥ 0.50
# Ta đặt ngưỡng cao hơn một chút để tăng độ tin cậy
CONFIDENCE_THRESHOLD = 0.55   # Tương đương face_distance ≤ 0.45
FACE_RECOGNITION_TOLERANCE = 0.45

# Global variables to hold encodings
known_face_encodings = []
known_face_names = []

DATASET_DIR = "dataset"


def load_dataset():
    """Load all face encodings from dataset directory into memory."""
    global known_face_encodings, known_face_names
    known_face_encodings = []
    known_face_names = []

    if not os.path.exists(DATASET_DIR):
        os.makedirs(DATASET_DIR)
        print(f"[FaceService] Created dataset directory at '{DATASET_DIR}'")
        return

    print("[FaceService] Loading known faces...")
    for student_id in os.listdir(DATASET_DIR):
        student_dir = os.path.join(DATASET_DIR, student_id)
        if os.path.isdir(student_dir):
            for filename in os.listdir(student_dir):
                if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
                    img_path = os.path.join(student_dir, filename)
                    try:
                        image = face_recognition.load_image_file(img_path)
                        encodings = face_recognition.face_encodings(image)
                        if len(encodings) > 0:
                            known_face_encodings.append(encodings[0])
                            known_face_names.append(student_id)
                    except Exception as e:
                        print(f"[FaceService] Error loading {img_path}: {e}")

    print(f"[FaceService] Loaded {len(known_face_encodings)} face encodings from {DATASET_DIR}.")


@app.on_event("startup")
async def startup_event():
    load_dataset()


@app.post("/recognize")
async def recognize_face(file: UploadFile = File(...)):
    """
    Nhận diện khuôn mặt từ ảnh upload.

    Response JSON (tất cả các trường hợp đều trả JSON, KHÔNG raise HTTPException):
      Success:       {"status": "success",        "id": "<student_id>", "confidence": 0.87}
      Low confidence:{"status": "low_confidence", "id": "<student_id>", "confidence": 0.51, "message": "..."}
      Not found:     {"status": "error",           "code": "NOT_RECOGNIZED",  "message": "..."}
      No face:       {"status": "error",           "code": "NO_FACE_DETECTED","message": "..."}
      Empty dataset: {"status": "error",           "code": "NO_DATASET",      "message": "..."}
      Bad image:     {"status": "error",           "code": "INVALID_IMAGE",   "message": "..."}
    """
    # --- Guard: dataset rỗng ---
    # FIX: trả JSON thay vì HTTPException(500) text → Java parse được
    if not known_face_encodings:
        return JSONResponse(
            status_code=200,
            content={
                "status": "error",
                "code": "NO_DATASET",
                "message": "Chưa có dữ liệu khuôn mặt trong hệ thống. "
                           "Vui lòng đăng ký khuôn mặt trước khi điểm danh."
            }
        )

    # --- Đọc và decode ảnh ---
    try:
        contents = await file.read()
        nparr = np.frombuffer(contents, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    except Exception as e:
        return JSONResponse(
            status_code=200,
            content={"status": "error", "code": "INVALID_IMAGE", "message": f"Không đọc được ảnh: {str(e)}"}
        )

    if img is None:
        return JSONResponse(
            status_code=200,
            content={"status": "error", "code": "INVALID_IMAGE", "message": "File ảnh không hợp lệ hoặc bị hỏng."}
        )

    # --- Nhận diện khuôn mặt ---
    rgb_img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    face_locations = face_recognition.face_locations(rgb_img)
    face_encodings_in_img = face_recognition.face_encodings(rgb_img, face_locations)

    if len(face_encodings_in_img) == 0:
        return JSONResponse(
            status_code=200,
            content={
                "status": "error",
                "code": "NO_FACE_DETECTED",
                "message": "Không phát hiện khuôn mặt trong ảnh. "
                           "Vui lòng đảm bảo ánh sáng tốt và nhìn thẳng vào camera."
            }
        )

    # Chỉ xét khuôn mặt đầu tiên tìm được
    encoding_to_check = face_encodings_in_img[0]

    # So khớp với dataset
    matches = face_recognition.compare_faces(
        known_face_encodings, encoding_to_check,
        tolerance=FACE_RECOGNITION_TOLERANCE
    )
    face_distances = face_recognition.face_distance(known_face_encodings, encoding_to_check)

    if len(face_distances) == 0:
        return JSONResponse(
            status_code=200,
            content={"status": "error", "code": "NOT_RECOGNIZED", "message": "Khuôn mặt không khớp với bất kỳ sinh viên nào."}
        )

    best_match_index = int(np.argmin(face_distances))
    best_distance = float(face_distances[best_match_index])
    # Chuyển distance → confidence: distance=0 → confidence=1.0, distance=0.5 → confidence=0.5
    confidence = round(1.0 - best_distance, 4)
    student_id = known_face_names[best_match_index]

    if matches[best_match_index] and confidence >= CONFIDENCE_THRESHOLD:
        # Nhận diện thành công, đủ độ tin cậy
        return JSONResponse(
            status_code=200,
            content={
                "status": "success",
                "id": student_id,
                "confidence": confidence,
                "message": f"Nhận diện thành công: {student_id} (confidence={confidence:.2%})"
            }
        )
    elif confidence >= 0.40:
        # Nhận ra nhưng không đủ chắc chắn → yêu cầu thử lại
        return JSONResponse(
            status_code=200,
            content={
                "status": "low_confidence",
                "id": student_id,
                "confidence": confidence,
                "message": f"Nhận diện không chắc chắn ({confidence:.0%}). Vui lòng thử lại hoặc liên hệ giảng viên."
            }
        )
    else:
        return JSONResponse(
            status_code=200,
            content={
                "status": "error",
                "code": "NOT_RECOGNIZED",
                "confidence": confidence,
                "message": "Khuôn mặt không khớp. Vui lòng thử lại hoặc điểm danh thủ công."
            }
        )


@app.post("/reload-dataset")
async def reload_dataset():
    """Hot-reload dataset mà không cần restart service."""
    load_dataset()
    return {"status": "success", "loaded_encodings": len(known_face_encodings)}


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "loaded_encodings": len(known_face_encodings),
        "dataset_dir": DATASET_DIR,
        "confidence_threshold": CONFIDENCE_THRESHOLD
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
