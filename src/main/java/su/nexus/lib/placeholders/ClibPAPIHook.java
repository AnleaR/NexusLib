package su.nexus.lib.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClibPAPIHook extends PlaceholderExpansion {

	public String onRequest(final OfflinePlayer p, final String placeholder) {
		if (p != null && !(p instanceof Player)) {
			throw new IllegalArgumentException("Requested placeholder " + placeholder + " for non-player " + p.getName());
		}
		final Player player = (Player) p;
		final String proceded = PlaceholderManager.proccessString(player, "(" + placeholder + ")", true, true);
		if (("(" + placeholder + ")").equals(proceded)) {
			return null;
		}
		return proceded;
	}

	public @NotNull String getAuthor() {
		return "AnleaR";
	}

	public @NotNull String getIdentifier() {
		return "clib";
	}

	public @NotNull String getVersion() {
		return "1.0";
	}
}