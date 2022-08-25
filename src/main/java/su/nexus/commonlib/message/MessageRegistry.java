package su.nexus.commonlib.message;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import su.nexus.commonlib.CommonLibPlugin;
import su.nexus.commonlib.util.GsonUtil;

import java.io.File;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

public class MessageRegistry {

	private static final Map<String, Messages> messages = Collections.synchronizedMap(new LinkedHashMap());
	private static final Table<Plugin, Predicate<Object>, TagProcessor<?>> customTags = HashBasedTable.create();
	private static final List<Plugin> usingNewMessagesSystem = new ArrayList<Plugin>();

	public static void registerDefaults() {
		MessageRegistry.unregisterAll();
		MessageRegistry.registerTagAdapter(CommonLibPlugin.getInstance(), JsonElement.class, e -> e);
		MessageRegistry.registerTagAdapter(CommonLibPlugin.getInstance(), Number.class, ((Gson) GsonUtil.GSON)::toJsonTree);
		MessageRegistry.registerTagAdapter(CommonLibPlugin.getInstance(), String.class, ((Gson) GsonUtil.GSON)::toJsonTree);
		MessageRegistry.registerTagAdapter(CommonLibPlugin.getInstance(), Message.class, Message::getCompleteValue);
		MessageRegistry.registerTagAdapter(CommonLibPlugin.getInstance(), MessageKey.class, e -> e.message().getCompleteValue());
		MessageRegistry.registerPrimitiveTagAdapter(CommonLibPlugin.getInstance(), Player.class, OfflinePlayer::getName);
	}

	public static void addMessages(String name, Messages map) {
		Preconditions.checkNotNull((Object) name, "Name cannot be null");
		Preconditions.checkNotNull((Object) map, "Messages cannot be null");
		messages.put(name, map);
	}

	public static Messages removeMessages(String name) {
		return messages.remove(name);
	}

	public static Message message(MessageKey key) {
		Preconditions.checkNotNull((Object) key, "Key cannot be null");
		Message entry = MessageRegistry.message(key.getKey());
		if (!entry.exists()) {
			entry = new Message(key.getKey(), key.getDefault() instanceof JsonElement ? (JsonElement) key.getDefault() : GsonUtil.GSON.toJsonTree(key.getDefault()));
		}
		return entry;
	}

	public static Message message(String key) {
		Preconditions.checkNotNull((Object) key, "Key cannot be null");
		Messages map = null;
		for (Map.Entry<String, Messages> entry : messages.entrySet()) {
			if (!entry.getValue().has(key)) continue;
			map = entry.getValue();
			break;
		}
		if (map == null) {
			return new Message(key, GsonUtil.GSON.toJsonTree("UNKNOWN KEY " + key), Messages.EMPTYMAP);
		}
		return map.get(key);
	}

	public static <T> void registerPrimitiveTagAdapter(Plugin plugin, Class<T> type, Function<T, Object> adapter) {
		MessageRegistry.registerTagProcessor(plugin, type::isInstance, e -> GsonUtil.GSON.toJsonTree(adapter.apply((T) e)));
	}

	public static <T> void registerTagAdapter(Plugin plugin, Class<T> type, TagProcessor<T> adapter) {
		MessageRegistry.registerTagProcessor(plugin, type::isInstance, adapter);
	}

	public static void registerTagProcessor(Plugin plugin, Predicate<Object> condition, TagProcessor processor) {
		Preconditions.checkNotNull(plugin, "Plugin cannot be null");
		Preconditions.checkNotNull(condition, "Condition cannot be null");
		Preconditions.checkNotNull(processor, "TagProcessor cannot be null");
		customTags.put(plugin, condition, processor);
	}

	public static void unregisterTagProcessors(Plugin plugin) {
		ImmutableList.copyOf(customTags.row(plugin).keySet()).forEach(cond -> {
			TagProcessor cfr_ignored_0 = customTags.remove(plugin, cond);
		});
	}

	public static TagProcessor findTagProcessor(Object object) {
		return customTags.columnKeySet().stream().filter(cond -> cond.test(object)).map(customTags::column).flatMap(
				map -> map.values().stream()).findFirst().orElse(((Gson) GsonUtil.GSON)::toJsonTree);
	}

	public static void unregisterAll() {
		customTags.clear();
	}

	public static Messages load(Plugin plugin, String jsonFileName) {
		return MessageRegistry.load(new File(plugin.getDataFolder(), jsonFileName));
	}

	public static Messages load(Plugin plugin, String jsonFileName, MessageKey[] keys) {
		return MessageRegistry.load(plugin, jsonFileName, Arrays.asList(keys));
	}

	public static Messages load(Plugin plugin, String jsonFileName, Collection<MessageKey> keys) {
		return MessageRegistry.load(new File(plugin.getDataFolder(), jsonFileName), keys);
	}

	public static Messages load(File file, Collection<MessageKey> keys) {
		JsonObject jsonObject;

		boolean exists = file.exists();
		if (!exists) {
			file.getParentFile().mkdirs();
			GsonUtil.writeToFile(file, "{}");
		}
		try {
			jsonObject = MessageRegistry.loadFromFile(file);
		} catch (JsonParseException e) {
			CommonLibPlugin.getInstance().getLogger().log(Level.SEVERE, "Cannot parse messages file " + file.getAbsolutePath(), e);
			return new Messages(new JsonObject());
		}
		boolean hasMissedKeys = keys.stream().filter(Objects::nonNull).anyMatch(k -> !jsonObject.has(k.getKey()));
		keys.stream().filter(Objects::nonNull).filter(k -> !jsonObject.has(k.getKey())).forEach(k -> jsonObject.add(k.getKey(), GsonUtil.GSON.toJsonTree(k.getDefault())));
		if (hasMissedKeys) {
			GsonUtil.writeAsJsonToFile(file, new TreeMap(GsonUtil.GSON.fromJson(jsonObject, Map.class)));
		}

		return new Messages(jsonObject);
	}

	public static Messages load(File file) {
		try {
			return new Messages(MessageRegistry.loadFromFile(file));
		} catch (JsonParseException e) {
			CommonLibPlugin.getInstance().getLogger().log(Level.SEVERE, "Cannot parse messages file " + file.getAbsolutePath(), e);
			return new Messages(new JsonObject());
		}
	}

	public static void usingNewMessages(Plugin plugin) {
		if (!usingNewMessagesSystem.contains(plugin)) {
			usingNewMessagesSystem.add(plugin);
		}
	}

	public static boolean isUsingNewMessages(Plugin plugin) {
		return usingNewMessagesSystem.contains(plugin);
	}

	private static JsonObject loadFromFile(File json) {
		if (!json.exists()) {
			GsonUtil.writeToFile(json, "{}");
			return new JsonObject();
		}
		String content = GsonUtil.readFile(json);
		if (content.isEmpty()) {
			return new JsonObject();
		}
		JsonReader reader = new JsonReader(new StringReader(GsonUtil.readFile(json)));
		reader.setLenient(true);
		return GsonUtil.GSON.fromJson(reader, JsonObject.class);
	}

	public static Map<String, Messages> getMessagesMap() {
		return messages;
	}
}
