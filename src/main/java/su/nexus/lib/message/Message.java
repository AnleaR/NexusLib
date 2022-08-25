package su.nexus.lib.message;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.janmm14.jsonmessagemaker.api.JsonMessageConverter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nexus.lib.NexusLibPlugin;
import su.nexus.lib.placeholders.PlaceholderManager;
import su.nexus.lib.util.GsonUtil;
import su.nexus.lib.util.UtilityMethods;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Message {

	static Pattern unusedKeyTypePattern = Pattern.compile("(\\[[a-z]*\\])");
	private final String key;
	private final Messages holder;
	private final Map<String, Object> tags = new LinkedHashMap<>();
	private JsonElement value;
	private JsonElement cachedWithTags;

	public Message(String key, List<String> value) {
		this(key, GsonUtil.GSON.toJsonTree(value));
	}

	public Message(String key, String value) {
		this(key, GsonUtil.GSON.toJsonTree(value));
	}

	public Message(String key, JsonElement value) {
		this(key, value, null);
	}

	public Message(String key, JsonElement value, Messages holder) {
		Preconditions.checkNotNull((Object) key, "Key cannot be null");
		this.key = key;
		this.holder = holder;
		this.setValue(value);
	}

	public static JsonPrimitive setPrimitive(JsonPrimitive primitive, Object value) {
		Accessors.getFieldAccessor(JsonPrimitive.class, "value".getClass(), true).set(primitive, value);
		return primitive;
	}

	public static Function<JsonPrimitive, JsonElement> placeholdersReplacer(Player player) {
		return i -> Message.setPrimitive(i, PlaceholderManager.proccessString(player, i.getAsString()));
	}

	public static Message create(String key, String text) {
		return new Message(key, text);
	}

	public void setValue(JsonElement value) {
		Preconditions.checkNotNull((Object) value, "Value cannot be null");
		this.value = UtilityMethods.replaceValues(value, this::replaceNestedMessagesInJsonPrimitive);
		this.cachedWithTags = null;
	}

	public Message tag(String tag, Object value) {
		Preconditions.checkNotNull((Object) tag, "Tag cannot be null");
		this.tags.put(tag, value);
		this.cachedWithTags = null;
		return this;
	}

	public Message tag(String tag, String value) {
		return this.tag(tag, (Object) value);
	}

	public Message tag(MessageTagger tagger) {
		if (tagger == null) {
			return this;
		}
		return tagger.tagMessage(this);
	}

	public void putTags(Map<String, ?> tags) {
		this.tags.putAll(tags);
	}

	public Map<String, Object> getTags() {
		return Collections.unmodifiableMap(this.tags);
	}

	public String getKey() {
		return this.key;
	}

	public JsonElement getRawValue() {
		return this.value;
	}

	public Messages getHolder() {
		return this.holder;
	}

	@Deprecated
	public String get() {
		return this.getAsString();
	}

	@Deprecated
	public List<String> getAsList() {
		return this.getAsStringList();
	}

	@Deprecated
	public JsonElement getAsJsonElement() {
		return this.getCompleteValue();
	}

	public String getAsString() {
		return this.getAsString(this.getCompleteValue());
	}

	public List<String> getAsStringList() {
		ArrayList<String> list = new ArrayList<String>();
		JsonElement tagged = this.getCompleteValue();
		if (tagged.isJsonArray()) {
			StreamSupport.stream(tagged.getAsJsonArray().spliterator(), false).map(this::getAsString).forEach(list::add);
		} else {
			if (tagged.isJsonNull()) {
				return Collections.emptyList();
			}
			list.add(this.getAsString(tagged));
		}
		return list;
	}

	public boolean exists() {
		return this.getHolder() != Messages.EMPTYMAP;
	}

	public MessageType getType() {
		return this.calculateType(this.getCompleteValue());
	}

	public JsonElement getCompleteValue() {
		if (this.cachedWithTags != null) {
			return this.cachedWithTags;
		}
		JsonElement result = this.getRawValue();
		if (this.getRawValue().isJsonNull()) {
			return result;
		}
		for (Map.Entry<String, Object> tag : this.tags.entrySet()) {
			String full = "(" + tag.getKey() + ")";
			Object value = tag.getValue();
			TagProcessor obj = MessageRegistry.findTagProcessor(value);
			try {
				result = UtilityMethods.replaceValues(result, e -> this.replaceTags(full, obj.toElement(value), e));
			} catch (Exception e2) {
				NexusLibPlugin.getInstance().getLogger().log(Level.SEVERE, "Got exception processing tag " + full + " with value " + value + " for message " + this.key, e2);
			}
		}
		this.cachedWithTags = UtilityMethods.replaceValues(result, e -> Message.setPrimitive(e, UtilityMethods.color(e.getAsString())));
		return result;
	}

	private JsonElement replaceTags(String key, JsonElement value, JsonElement source) {
		String str;
		Preconditions.checkNotNull((Object) key, "key");
		if (value.isJsonNull()) {
			value = new JsonPrimitive("");
		}
		if (key.equals(str = source.getAsString())) {
			return value;
		}
		str = str.replace(key, this.getAsString(value));
		return GsonUtil.GSON.toJsonTree(str);
	}

	private String getAsString(JsonElement el) {
		String result;
		if (el.isJsonPrimitive()) {
			result = el.getAsString();
		} else if (el.isJsonArray()) {
			result = Joiner.on('\n').join(StreamSupport.stream(el.getAsJsonArray().spliterator(), false).map(e -> e.isJsonPrimitive() ? e.getAsString() : e.toString()).collect(Collectors.toList()));
		} else {
			if (el.isJsonNull()) {
				return "";
			}
			result = el.toString();
		}
		result = UtilityMethods.color(result);
		if (result.startsWith("$C$")) {
			result = UtilityMethods.centerString(result.substring(3));
		}
		return result;
	}

	private JsonElement replaceNestedMessagesInJsonPrimitive(JsonPrimitive source) {
		Preconditions.checkNotNull((Object) source, "Source cannot be null");
		Preconditions.checkArgument(source.isJsonPrimitive(), "Source is not JsonString");
		try {
			LinkedHashSet<String> keys = new LinkedHashSet<String>();
			String str = source.getAsString();
			Matcher m = Messages.keyPattern.matcher(str);
			while (m.find()) {
				String gr = m.group();
				if (gr.matches(unusedKeyTypePattern.pattern())) {
					gr = gr.replaceAll(unusedKeyTypePattern.pattern(), "");
				}
				keys.add(gr);
			}
			if (keys.isEmpty()) {
				return GsonUtil.GSON.toJsonTree(UtilityMethods.color(str));
			}
			if (keys.size() == 1 && Iterables.getOnlyElement(keys).equals(str)) {
				return this.getOurOrAnother(str.substring(2, str.length() - 1)).getCompleteValue();
			}
			for (String key : keys) {
				String wbkey = key.substring(2, key.length() - 1);
				Message entry = this.getOurOrAnother(wbkey);
				str = str.replace(key, entry.getAsString(entry.getCompleteValue()));
			}
			return GsonUtil.GSON.toJsonTree(UtilityMethods.color(str));
		} catch (Throwable e) {
			e.printStackTrace();
			return source;
		}
	}

	private Message getOurOrAnother(String key) {
		if (this.getHolder() != null && this.getHolder().has(key)) {
			return this.getHolder().get(key);
		}
		return MessageRegistry.message(key);
	}

	public void send(Player p) {
		this.send((CommandSender) p);
	}

	public void broadcast() {
		this.broadcast(MessagePosition.CHAT);
	}

	public void broadcast(MessagePosition pos) {
		this.sendM(pos, Bukkit.getOnlinePlayers());
	}

	public void send(CommandSender cs) {
		this.send(cs, MessagePosition.CHAT);
	}

	public void send(CommandSender cs, MessagePosition pos) {
		JsonElement result = this.getCompleteValue();
		if (cs instanceof Player && !result.isJsonNull()) {
			result = GsonUtil.GSON.toJsonTree(result);
			result = UtilityMethods.replaceValues(result, Message.placeholdersReplacer((Player) cs));
		}

		this.send(cs, result, pos);
	}

	private void send(CommandSender cs, JsonElement el, MessagePosition pos) {
		MessageType type = this.calculateType(el);
		switch (type) {
			case STRING: {
				if (!(cs instanceof Player) || pos == MessagePosition.CHAT) {
					if (getAsString(el).contains("[jmm|")) {
						cs.sendMessage(Arrays.toString(JsonMessageConverter.DEFAULT.convert(getAsString())));
					} else {
						cs.sendMessage(this.getAsString(el));
					}
					break;
				}
				UtilityMethods.sendActionBar((Player) cs, this.getAsString());
				break;
			}
			case JSON: {
				if (el.getAsJsonObject().entrySet().isEmpty()) break;
				if (cs instanceof Player) {
					UtilityMethods.sendRawMessage((Player) cs, el.toString(), pos);
					break;
				}
				cs.sendMessage(UtilityMethods.jsonToString(el));
				break;
			}
			case LIST: {
				el.getAsJsonArray().forEach(e -> this.send(cs, e, pos));
				break;
			}
		}
	}

	public void sendM(Iterable<? extends Player> players) {
		this.sendM(MessagePosition.CHAT, players);
	}

	public void sendM(MessagePosition pos, Iterable<? extends Player> players) {
		players.forEach(p -> this.send(p, pos));
	}

	public void sendM(Player... p) {
		this.sendM(MessagePosition.CHAT, p);
	}

	public void sendM(MessagePosition pos, Player... p) {
		if (p.length > 0) {
			this.sendM(pos, Arrays.asList(p));
		}
	}

	private MessageType calculateType(JsonElement result) {
		if (result.isJsonPrimitive()) {
			return MessageType.STRING;
		}
		if (result.isJsonArray()) {
			return MessageType.LIST;
		}
		if (result.isJsonNull()) {
			return MessageType.NULL;
		}
		return MessageType.JSON;
	}
}