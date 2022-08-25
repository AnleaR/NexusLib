package su.nexus.commonlib.message;

public interface EnumBasedMessageKey extends MessageKey {

	@Override
	default String getKey() {
		Enum e = (Enum) (this);
		return this.getKeyPrefix() + e.name().toLowerCase().replace('_', '.');
	}

	default String getKeyPrefix() {
		return "";
	}
}
