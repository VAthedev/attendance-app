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

		if (attendance.get("_id") == null) {
			attendance.put("_id", new ObjectId().toHexString());
		}

		try {
			attendanceCollection.insertOne(attendance);
			return true;
		} catch (MongoWriteException e) {
			if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
				throw new IllegalStateException(e.getMessage(), e);
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
				throw new IllegalStateException(e.getMessage(), e);
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

	public long countBySessionId(String sessionId) {
		return attendanceCollection.countDocuments(Filters.eq("session_id", sessionId));
	}

	public List<Document> findByStudentId(String studentId) {
		if (studentId == null || studentId.isBlank()) {
			return new ArrayList<>();
		}
		return attendanceCollection.find(studentIdentityFilter(studentId)).into(new ArrayList<>());
	}

	public Document findBySessionAndStudent(String sessionId, String studentId) {
		return attendanceCollection.find(
			Filters.and(
				Filters.eq("session_id", sessionId),
				studentIdentityFilter(studentId)
			)
		).first();
	}

	private org.bson.conversions.Bson studentIdentityFilter(String studentId) {
		List<org.bson.conversions.Bson> filters = new ArrayList<>();
		for (String key : studentLookupKeys(studentId)) {
			filters.add(Filters.eq("student_id", key));
			filters.add(Filters.eq("user_id", key));
		}
		if (filters.isEmpty()) {
			return Filters.eq("student_id", "__missing_student_id__");
		}
		return filters.size() == 1 ? filters.get(0) : Filters.or(filters);
	}

	private List<String> studentLookupKeys(String studentId) {
		java.util.LinkedHashSet<String> keys = new java.util.LinkedHashSet<>();
		if (studentId != null && !studentId.isBlank()) {
			keys.add(studentId);
			Document user = findUserByAnyIdentity(studentId);
			if (user != null) {
				addIfPresent(keys, user.get("student_id"));
				addIfPresent(keys, user.get("username"));
				Object legacyId = user.get("id");
				if (legacyId != null) {
					keys.add(legacyId.toString());
				}
				Object mongoId = user.get("_id");
				if (mongoId != null) {
					keys.add(mongoId.toString());
				}
			}
		}
		return new ArrayList<>(keys);
	}

	private Document findUserByAnyIdentity(String value) {
		List<org.bson.conversions.Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("_id", value));
		filters.add(Filters.eq("student_id", value));
		filters.add(Filters.eq("username", value));
		try {
			filters.add(Filters.eq("id", Integer.parseInt(value)));
		} catch (NumberFormatException ignored) {}
		return usersCollection.find(Filters.or(filters)).first();
	}

	private void addIfPresent(java.util.LinkedHashSet<String> keys, Object value) {
		if (value != null && !value.toString().isBlank()) {
			keys.add(value.toString());
		}
	}

	private void validateReferences(Document doc) {
		String studentId = doc.getString("student_id");
		if (studentId != null && !studentId.isBlank()) {
			ensureUserExists(studentId);
		}
	}

	private void ensureUserExists(String userId) {
		try {
			Document user = findUserByAnyIdentity(userId);
			if (user == null) {
				throw new IllegalArgumentException("Khong tim thay user tham chieu: " + userId);
			}
		} catch (MongoException e) {
			throw new RuntimeException("Loi kiem tra tham chieu users: " + e.getMessage(), e);
		}
	}
}
