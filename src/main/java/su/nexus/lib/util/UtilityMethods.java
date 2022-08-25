package su.nexus.lib.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.netty.WirePacket;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.utility.StreamSerializer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.mineacademy.fo.ChatUtil;
import su.nexus.lib.NexusLibPlugin;
import su.nexus.lib.message.MessagePosition;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UtilityMethods {

	public static String centerString(String message) {
		return ChatUtil.center(message);
	}

	public static List<String> color(Collection<String> s) {
		return s.stream().map(UtilityMethods::color).collect(Collectors.toList());
	}

	public static ItemStack color(ItemStack is) {
		if (!is.hasItemMeta()) {
			return is;
		} else {
			ItemMeta im = is.getItemMeta();
			if (im.hasDisplayName()) {
				im.setDisplayName(color(im.getDisplayName()));
			}

			if (im.hasLore()) {
				im.setLore(color(im.getLore()));
			}

			is.setItemMeta(im);
			return is;
		}
	}

	public static String color(String message) {
		return ColorsUtil.color(message);
	}

	public static boolean has(String string) {
		return Bukkit.getPluginManager().isPluginEnabled(string);
	}

	public static void sendPacket(Player player, PacketContainer packetContainer) {
		try {
			if (player.isOnline()) {
				ProtocolLibrary.getProtocolManager().sendServerPacket(player, packetContainer);
			}
		} catch (IllegalArgumentException var3) {
		}
	}

	public static void sendActionBar(Player p, String legacy) {
		sendRawMessage(p, ComponentSerializer.toString(TextComponent.fromLegacyText(legacy)), MessagePosition.ACTIONBAR);
	}

	public static void sendRawMessage(Player p, String json) {
		sendRawMessage(p, json, MessagePosition.CHAT);
	}

	public static void sendRawMessage(Player p, String json, MessagePosition pos) {
		sendRawMessage(p, json, pos, pos == MessagePosition.ACTIONBAR ? 3 : 0);
	}

	public static void sendRawMessage(Player p, String json, MessagePosition pos, int repeats) {
		if (!json.equals("{}") && p != null && p.isOnline()) {
			if (pos == MessagePosition.ACTIONBAR) {
				json = "{\"text\": \"" + jsonToString(json) + "\"}";
			}

			sendRawFixedMessage(p, json, pos, repeats);
		}
	}

	@SuppressWarnings("notworking")
	private static void sendRawFixedMessage(Player p, String fixedJson, MessagePosition pos, int repeats) {
		if (MinecraftVersion.getCurrentVersion().isAtLeast(MinecraftVersion.COLOR_UPDATE)) {
			/*
			   TODO
			   <- Method is in work ->
			   <- NOT WOTKS ->
			   sendRawDirectly(p, fixedJson, pos);
			 */
		} else {
			sendRawCompatible(p, fixedJson, pos);
		}

		if (repeats > 0) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(NexusLibPlugin.getInstance(), () -> {
				sendRawFixedMessage(p, fixedJson, pos, repeats - 1);
			}, 20L);
		}
	}

	private static void sendRawCompatible(Player player, String json, MessagePosition pos) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
		EnumWrappers.ChatType type = MessagePosition.toChatType(pos);
		packet.getBytes().writeSafely(0, type.getId());
		packet.getChatTypes().writeSafely(0, type);

		packet.getSpecificModifier(BaseComponent[].class).write(0, ComponentSerializer.parse(json));
		sendPacket(player, packet);
	}

	public static String jsonToString(JsonElement el) {
		return el.isJsonObject() && el.getAsJsonObject().entrySet().isEmpty() ? "" : jsonToString(el.toString());
	}

	public static String jsonToString(String json) {
		if (json != null && !json.isEmpty() && !json.equals("{}")) {
			try {
				return BaseComponent.toLegacyText(ComponentSerializer.parse(json));
			} catch (Exception var2) {
				System.out.println("Error ocurred while serializing " + json);
				var2.printStackTrace();
				return null;
			}
		} else {
			return "";
		}
	}

	public static JsonElement replaceValues(JsonElement source, Function<JsonPrimitive, JsonElement> replacer) {
		if (source.isJsonPrimitive()) {
			return replacer.apply(source.getAsJsonPrimitive());
		} else if (source.isJsonArray()) {
			JsonArray jarr = new JsonArray();
			StreamSupport.stream(source.getAsJsonArray().spliterator(), false).map((el) -> {
				return replaceValues(el, replacer);
			}).forEachOrdered(jarr::add);
			return jarr;
		} else if (source.isJsonNull()) {
			return source;
		} else {
			JsonObject obj = new JsonObject();
			source.getAsJsonObject().entrySet().forEach((e) -> {
				obj.add(e.getKey(), replaceValues(e.getValue(), replacer));
			});
			return obj;
		}
	}
}