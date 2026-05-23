package database;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import model.ChatMessage;

public class ChatRepository {

	private final MongoCollection<Document> chatMessages;
	private final MongoCollection<Document> subjects;

	public ChatRepository() {
		DatabaseHelper db = DatabaseHelper.getInstance();
		this.chatMessages = db.getChatMessagesCollection();
		this.subjects = db.getSubjectsCollection();
	}

	public boolean saveMessage(ChatMessage message) {
		Document doc = new Document("_id", message.getId())
			.append("room_key", message.getRoomKey())
			.append("room_name", message.getRoomName())
			.append("subject_code", message.getRoomKey())
			.append("subject_name", message.getRoomName())
			.append("sender_id", message.getSender())
			.append("sender_name", message.getSenderName())
			.append("content", message.getContent())
			.append("sent_at", new Date(message.getTimestamp()));
		chatMessages.insertOne(doc);
		return true;
	}

	public List<ChatMessage> findMessagesByRoomSince(String roomKey, LocalDateTime since) {
		List<ChatMessage> result = new ArrayList<>();
		try {
			Date sinceDate = Date.from(since.atZone(ZoneId.systemDefault()).toInstant());
			Bson filter = Filters.and(
				Filters.eq("room_key", roomKey),
				Filters.gte("sent_at", sinceDate)
			);
			FindIterable<Document> cursor = chatMessages.find(filter).sort(Sorts.ascending("sent_at"));
			for (Document doc : cursor) {
				result.add(mapMessage(doc));
			}
		} catch (Exception e) {
			// return empty list if DB is unavailable or schema mismatch
		}
		return result;
	}

	public List<Map<String, Object>> findChatRooms() {
		List<Map<String, Object>> result = new ArrayList<>();
		try {
			FindIterable<Document> cursor = subjects.find().sort(Sorts.ascending("code"));
			for (Document doc : cursor) {
				Map<String, Object> room = new HashMap<>();
				String code = doc.getString("code");
				String name = doc.getString("name");
				String lecturer = doc.getString("lecturer_name");
				room.put("roomKey", code);
				room.put("roomName", (name != null ? name : code) + (lecturer != null && !lecturer.isBlank() ? " • " + lecturer : ""));
				room.put("subjectCode", code);
				room.put("subjectName", name);
				result.add(room);
			}
		} catch (Exception e) {
			// ignore and return empty
		}
		return result;
	}

	public List<ChatMessage> findHistoryLastDays(String roomKey, int days) {
		LocalDateTime since = LocalDateTime.now().minusDays(days);
		return findMessagesByRoomSince(roomKey, since);
	}

	private ChatMessage mapMessage(Document doc) {
		ChatMessage msg = new ChatMessage();
		msg.setId(doc.getString("_id"));
		msg.setRoomKey(doc.getString("room_key") != null ? doc.getString("room_key") : doc.getString("subject_code"));
		msg.setRoomName(doc.getString("room_name") != null ? doc.getString("room_name") : doc.getString("subject_name"));
		msg.setSender(doc.getString("sender_id"));
		msg.setSenderName(doc.getString("sender_name"));
		msg.setContent(doc.getString("content"));
		Date sentAt = doc.getDate("sent_at");
		msg.setTimestamp(sentAt != null ? sentAt.getTime() : Instant.now().toEpochMilli());
		return msg;
	}
}
