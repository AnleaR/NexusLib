package su.nexus.lib.placeholders;

import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import su.nexus.lib.util.UtilityMethods;

import java.util.concurrent.Callable;
import java.util.logging.Level;

public class PlaceholderInfo {

	@Getter
	private final String id;
	private final Plugin holder;
	@Getter
	private final Placeholder replacer;
	private final boolean mayCallAsync;
	private final boolean playerRequired;

	public PlaceholderInfo(final String id, final Plugin holder, final Placeholder replacer) {
		this(id, holder, replacer, true);
	}

	public PlaceholderInfo(final String id, final Plugin holder, final Placeholder replacer, final boolean allowAsync) {
		this(id, holder, replacer, allowAsync, true);
	}

	public PlaceholderInfo(final String id, final Plugin holder, final Placeholder replacer, final boolean allowAsync, final boolean playerRequired) {
		Preconditions.checkNotNull((Object) holder);
		Preconditions.checkNotNull((Object) id);
		this.id = id;
		this.holder = holder;
		this.replacer = replacer;
		this.mayCallAsync = allowAsync;
		this.playerRequired = playerRequired;
	}

	public Plugin getHolder() {
		return this.holder;
	}

	public boolean mayCallAsync() {
		return this.mayCallAsync;
	}

	public boolean isCustom() {
		return !PlaceholderInfo.class.equals(this.getClass());
	}

	public boolean isPlayerRequired() {
		return this.playerRequired;
	}

	public String getDescription() {
		return null;
	}

	public void injectForeignPlaceholders() {
		this.injectHD();
	}

	public void uninjectForeignPlaceholders() {
		this.uninjectHD();
	}

	protected void injectHD() {
		if (UtilityMethods.has("HolographicDisplays") && !this.playerRequired) {
			HologramsAPI.registerPlaceholder(this.holder, this.getToken(), 5.0, () -> this.proccess(null, this.getToken()));
		}
	}

	protected void uninjectHD() {
		if (UtilityMethods.has("HolographicDisplays") && !this.playerRequired) {
			HologramsAPI.unregisterPlaceholder(this.holder, this.getToken());
		}
	}

	protected String getToken() {
		return "(" + this.id + ")";
	}

	public String proccess(final Player p, final String mes) {
		final Callable<String> callable = this.getQuery(p, mes);
		try {
			if (!Bukkit.isPrimaryThread() && !this.mayCallAsync()) {
				return Bukkit.getScheduler().callSyncMethod(this.holder, callable).get();
			}
			return callable.call();
		} catch (Exception e) {
			this.holder.getLogger().log(Level.SEVERE, "Error handling placeholder " + this.id + " for " + ((p != null) ? p.getName() : "null player"), e);
			return "!" + this.getId() + "_ERROR!";
		}
	}

	public Callable<String> getQuery(final Player p, final String mes) {
		return () -> {
			Preconditions.checkNotNull((Object) mes, "Message cannot be null");
			String token = this.getToken();
			if (this.replacer != null && (mes.equals(token) || mes.contains(token))) {
				Preconditions.checkState(p != null || !this.playerRequired, "Player is required for this placeholder");
				return mes.replace(token, String.valueOf(this.replacer.replace(p)));
			} else {
				return mes;
			}
		};
	}
}