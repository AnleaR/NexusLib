package su.nexus.commonlib.message;

public class SingleKey implements MessageKey {

	private final String key;
	private final Object def;

	public SingleKey(String key, Object def) {
		this.key = key;
		this.def = def;
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public Object getDefault() {
		return this.def;
	}
}