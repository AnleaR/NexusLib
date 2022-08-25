package su.nexus.commonlib.message;

public interface MessageTagger {
	default Message tagMessage(Message message) {
		this.applyMessageTags(message);
		return message;
	}

	default Message tagMessage(MessageKey key) {
		return this.tagMessage(key.message());
	}

	void applyMessageTags(Message var1);
}