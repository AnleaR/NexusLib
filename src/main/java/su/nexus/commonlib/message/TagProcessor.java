package su.nexus.commonlib.message;

import com.google.gson.JsonElement;

import java.util.function.Function;

public interface TagProcessor<T> extends Function<T, JsonElement> {
	JsonElement toElement(T var1);

	@Override
	default JsonElement apply(T t) {
		return this.toElement(t);
	}
}
