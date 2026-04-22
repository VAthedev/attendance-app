package database;

import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class AttendanceRepository {

	private final MongoCollection<Document> attendanceCollection;
	private final MongoCollection<Document> usersCollection;

	public AttendanceRepository() {
		DatabaseHelper db = DatabaseHelper.getInstance();
		this.attendanceCollection = db.getAttendanceCollection();
		this.usersCollection = db.getUsersCollection();
	}

	public Document findById(String id) {
		return attendanceCollection.find(Filters.eq("_id", id)).first();
	}

	public List<Document> findAll() {
		return attendanceCollection.find().into(new ArrayList<>());
	}

	public boolean insert(Document attendance) {
		validateReferences(attendance);

		if (attendance.getString("_id") == null) {
			attendance.put("_id", new ObjectId().toHexString());
		}

		try {
			attendanceCollection.insertOne(attendance);
			return true;
		} catch (MongoWriteException e) {
			if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
				throw new IllegalStateException(
					"Diem danh bi trung: (session_id, student_id) hoac (session_id, device_id).", e
				);
			}
			throw e;
		}
	}

	public boolean update(String id, Document updates) {
		Document safeUpdates = new Document(updates);
		safeUpdates.remove("_id");
		validateReferences(safeUpdates);

		try {
			UpdateResult result = attendanceCollection.updateOne(
				Filters.eq("_id", id),
				new Document("$set", safeUpdates)
			);
			return result.getModifiedCount() > 0;
		} catch (MongoWriteException e) {
			if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
				throw new IllegalStateException(
					"Cap nhat diem danh bi trung: (session_id, student_id) hoac (session_id, device_id).", e
				);
			}
			throw e;
		}
	}

	public boolean delete(String id) {
		DeleteResult result = attendanceCollection.deleteOne(Filters.eq("_id", id));
		return result.getDeletedCount() > 0;
	}

	public List<Document> findBySessionId(String sessionId) {
		return attendanceCollection.find(Filters.eq("session_id", sessionId)).into(new ArrayList<>());
	}

	public List<Document> findByStudentId(String studentId) {
		return attendanceCollection.find(Filters.eq("student_id", studentId)).into(new ArrayList<>());
	}

	public Document findBySessionAndStudent(String sessionId, String studentId) {
		return attendanceCollection.find(
			Filters.and(
				Filters.eq("session_id", sessionId),
				Filters.eq("student_id", studentId)
			)
		).first();
	}

	private void validateReferences(Document doc) {
		String studentId = doc.getString("student_id");
		if (studentId != null && !studentId.isBlank()) {
			ensureUserExists(studentId);
		}
	}

	private void ensureUserExists(String userId) {
		try {
			Document user = usersCollection.find(Filters.eq("_id", userId)).first();
			if (user == null) {
				throw new IllegalStateException("Khong tim thay user tham chieu: " + userId);
			}
		} catch (MongoException e) {
			throw new RuntimeException("Loi kiem tra tham chieu users: " + e.getMessage(), e);
		}
	}
}
