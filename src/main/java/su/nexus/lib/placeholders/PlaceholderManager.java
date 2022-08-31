package su.nexus.lib.placeholders;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import su.nexus.lib.NexusLib;
import su.nexus.lib.placeholders.custom.LoadablePlaceholder;
import su.nexus.lib.placeholders.custom.PlaceholdersClassLoader;
import su.nexus.lib.util.AutoCatch;
import su.nexus.lib.util.UtilityMethods;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class PlaceholderManager {
	private static Multimap<Plugin, PlaceholderInfo> placeholders;

	static {
		PlaceholderManager.placeholders = MultimapBuilder.linkedHashKeys().arrayListValues().build();
	}

	public static void registerPlaceholder(final Plugin plugin, final String ph, final Placeholder replacer) {
		registerPlaceholder(plugin, ph, replacer, false);
	}

	public static void registerPlaceholder(final Plugin plugin, final String ph, final Placeholder replacer, final boolean forceSync) {
		registerPlaceholder(plugin, ph, replacer, forceSync, true);
	}

	public static void registerPlaceholder(final Plugin plugin, final String ph, final Placeholder replacer, final boolean forceSync, final boolean playerRequired) {
		Preconditions.checkNotNull((Object) plugin);
		Preconditions.checkNotNull((Object) ph);
		Preconditions.checkNotNull((Object) replacer);
		final PlaceholderInfo info = new PlaceholderInfo(ph, plugin, replacer, !forceSync, playerRequired);
		PlaceholderManager.placeholders.put(plugin, info);
	}

	public static void registerCustomPlaceholder(final PlaceholderInfo placeholderInfo) {
		registerCustomPlaceholder(placeholderInfo.getHolder(), placeholderInfo);
	}

	public static void registerCustomPlaceholder(final Plugin plugin, final PlaceholderInfo placeholderInfo) {
		Preconditions.checkNotNull((Object) plugin);
		Preconditions.checkNotNull((Object) placeholderInfo);
		unregister(plugin, placeholderInfo.getId());
		PlaceholderManager.placeholders.put(plugin, placeholderInfo);
	}

	public static Collection<PlaceholderInfo> getRegisteredPlaceholders(final Plugin plugin) {
		return ImmutableList.copyOf(PlaceholderManager.placeholders.get(plugin));
	}

	public static PlaceholderInfo getPlaceholder(final Plugin plugin, final String name) {
		return getRegisteredPlaceholders(plugin).stream().filter(placeholderInfo -> placeholderInfo.getId().equalsIgnoreCase(name)).findAny().orElse(null);
	}

	public static void unregisterAll(final Plugin p) {
		PlaceholderManager.placeholders.removeAll(p);
	}

	public static void unregister(final Plugin plugin, final String pname) {
		for (final PlaceholderInfo placeholderInfo : getRegisteredPlaceholders(plugin)) {
			if (placeholderInfo.getId().equalsIgnoreCase(pname)) {
				PlaceholderManager.placeholders.remove(plugin, placeholderInfo);
			}
		}
	}

	public static void unregisterAll() {
		unloadFromClasses();
		PlaceholderManager.placeholders.clear();
	}

	public static Multimap<Plugin, PlaceholderInfo> getAllPlaceholders() {
		return ImmutableMultimap.copyOf((Multimap) PlaceholderManager.placeholders);
	}

	public static String proccessString(final Player p, final String message) {
		return proccessString(p, message, true);
	}

	public static String proccessString(final Player p, final String message, final boolean andColors) {
		return proccessString(p, message, andColors, false);
	}

	public static String proccessString(final Player player, String message, final boolean andColors, final boolean fromHooks) {
		for (final PlaceholderInfo placeholderInfo : getAllPlaceholders().values()) {
			message = placeholderInfo.proccess(player, message);
		}
		if (!fromHooks && UtilityMethods.has("PlaceholderAPI")) {
			message = PlaceholderAPI.setPlaceholders(player, message);
		}
		return (andColors ? UtilityMethods.color(message) : message).intern();
	}

	public static Collection<String> proccessCollection(final Player p, final Collection<String> mess) {
		return proccessCollection(p, mess, true);
	}

	public static Collection<String> proccessCollection(final Player p, final Collection<String> mess, final boolean andColors) {
		final List<String> newCol = new ArrayList<String>();
		for (final String mes : mess) {
			final String proc = proccessString(p, mes, andColors);
			newCol.add(proc);
		}
		return newCol;
	}

	public static ItemStack proccessItemStack(final Player p, final ItemStack is) {
		return proccessItemStack(p, is, true);
	}

	public static ItemStack proccessItemStack(final Player p, final ItemStack is, final boolean andColors) {
		return proccessItemStack(p, is, andColors, false);
	}

	public static ItemStack proccessItemStack(final Player p, final ItemStack is, final boolean andColors, final boolean checkSkull) {
		if (!is.hasItemMeta()) {
			return is;
		}
		boolean changes = false;
		final ItemMeta im = is.getItemMeta();
		if (checkSkull && im instanceof SkullMeta) {
			final SkullMeta sm = (SkullMeta) im;
			if (sm.hasOwner()) {
				final String owner = proccessString(p, sm.getOwner(), andColors);
				if (!Objects.equals(owner, sm.getOwner())) {
					sm.setOwner(owner);
					changes = true;
				}
			}
		}
		if (im.hasDisplayName()) {
			final String procName = proccessString(p, im.getDisplayName(), andColors);
			if (!Objects.equals(procName, im.getDisplayName())) {
				im.setDisplayName(procName);
				changes = true;
			}
		}
		if (im.hasLore()) {
			final List<String> procLore = (List<String>) proccessCollection(p, im.getLore(), andColors);
			if (!Objects.equals(procLore, im.getLore())) {
				im.setLore(procLore);
				changes = true;
			}
		}
		if (changes) {
			is.setItemMeta(im);
		}
		return is;
	}

	public static void unloadClassPlaceholder(final ClassPlaceholderInfo classPlaceholderInfo) {
		PlaceholderManager.placeholders.remove(classPlaceholderInfo.getHolder(), classPlaceholderInfo);
		AutoCatch.run(classPlaceholderInfo.getHandlingPlaceholder().getLoader()::close);
	}

	public static void loadFromClasses() {
		for (final File file : getPlaceholdersFolder().listFiles((f, n) -> f.isDirectory() || n.endsWith(".class"))) {
			final LoadablePlaceholder loadablePlaceholder = loadPlaceholderFromFile(file);
			if (loadablePlaceholder != null) {
				registerClassPlaceholder(loadablePlaceholder);
			}
		}
	}

	public static void unloadFromClasses() {
		getRegisteredPlaceholders(NexusLib.getInstance()).stream().filter(ClassPlaceholderInfo.class::isInstance)
				.map(ClassPlaceholderInfo.class::cast).forEach(PlaceholderManager::unloadClassPlaceholder);
	}

	public static ClassPlaceholderInfo registerClassPlaceholder(final LoadablePlaceholder loadablePlaceholder) {
		final ClassPlaceholderInfo info = new ClassPlaceholderInfo(loadablePlaceholder);
		loadablePlaceholder.init();
		registerCustomPlaceholder(info);
		return info;
	}

	public static LoadablePlaceholder loadPlaceholderFromFile(final File file) {
		if (!file.exists()) {
			return null;
		}
		final PlaceholdersClassLoader loader = new PlaceholdersClassLoader(NexusLib.getInstance(), file);
		return loader.lookupPlaceholder();
	}

	public static File getPlaceholdersFolder() {
		final File placeholdersFolder = new File(NexusLib.getInstance().getDataFolder(), "placeholders");
		if (!placeholdersFolder.exists()) {
			placeholdersFolder.mkdirs();
		}
		return placeholdersFolder;
	}
}