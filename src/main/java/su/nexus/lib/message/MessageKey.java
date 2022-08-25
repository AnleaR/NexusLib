/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.chat.ComponentSerializer
 *  org.bukkit.command.CommandSender
 */
package su.nexus.lib.message;

import com.google.gson.JsonElement;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.command.CommandSender;
import su.nexus.lib.util.GsonUtil;

import java.util.Arrays;
import java.util.List;

public interface MessageKey {
	static Object convertDefault(String... strings) {
		if (strings.length > 1) {
			return Arrays.asList(strings);
		}
		return strings.length == 1 ? strings[0] : null;
	}

	String getKey();

	Object getDefault();

	default Message message() {
		return MessageRegistry.message(this);
	}

	default String get() {
		return this.message().getAsString();
	}

	default List<String> getList() {
		return this.message().getAsStringList();
	}

	default void send(CommandSender cs) {
		this.send(cs, MessagePosition.CHAT);
	}

	default void send(CommandSender cs, MessagePosition pos) {
		this.message().send(cs, pos);
	}

	default Message tag(MessageTagger tagger) {
		return this.message().tag(tagger);
	}

	default Message tag(String key, Object value) {
		return this.message().tag(key, value);
	}

	default Object convertArray(String... string) {
		return MessageKey.convertDefault(string);
	}

	default JsonElement toJson(BaseComponent... components) {
		return GsonUtil.GSON.fromJson(ComponentSerializer.toString(components), JsonElement.class);
	}

	default void broadcast() {
		this.broadcast(MessagePosition.CHAT);
	}

	default void broadcast(MessagePosition pos) {
		this.message().broadcast(pos);
	}
}

