package su.nexus.lib.util;

import com.google.common.base.Preconditions;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class GsonUtil {
	public static final FilenameFilter jsonFilter;
	public static Gson GSON;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting().disableHtmlEscaping();
		builder.enableComplexMapKeySerialization().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
		GSON = builder.create();
		jsonFilter = (dir, name) -> {
			return name.endsWith(".json");
		};
	}

	public GsonUtil() {
	}

	public static void writeToFile(File file, String str) {
		try {
			if (!file.getAbsoluteFile().getParentFile().isDirectory()) {
				file.getAbsoluteFile().getParentFile().mkdirs();
			}

			Files.write(file.toPath(), str.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException var3) {
			var3.printStackTrace();
		}

	}

	public static void writeAsJsonToFile(File file, Object obj) {
		writeAsJsonToFile(file, obj, Format.JSON);
	}

	public static void writeAsJsonToFile(File file, Object obj, Format format) {
		String json = GSON.toJson(obj);
		writeToFile(file, format == Format.JSON ? json : JsonValue.readJSON(json).toString(Stringify.HJSON));
	}

	public static JsonType getJsonType(JsonElement element) {
		Preconditions.checkNotNull(element);
		if (element.isJsonNull()) {
			return JsonType.NULL;
		} else if (element.isJsonArray()) {
			return JsonType.ARRAY;
		} else if (element.isJsonObject()) {
			return JsonType.OBJECT;
		} else if (element.isJsonPrimitive()) {
			return JsonType.PRIMITIVE;
		} else {
			throw new IllegalStateException("Unknown type of " + element.getClass().getName() + element);
		}
	}

	public static String readFile(File file) {
		try {
			return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		} catch (IOException var2) {
			var2.printStackTrace();
			return null;
		}
	}

	public enum Format {
		JSON,
		HJSON;

		Format() {
		}
	}
}