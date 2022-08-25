package su.nexus.lib.placeholders;

import com.google.common.base.Joiner;
import org.bukkit.entity.Player;
import su.nexus.lib.placeholders.custom.LoadablePlaceholder;

import java.util.concurrent.Callable;

public final class ClassPlaceholderInfo extends PlaceholderInfo {
	private final LoadablePlaceholder placeholder;

	public ClassPlaceholderInfo(final LoadablePlaceholder placeholder) {
		super(placeholder.getId(), placeholder.lib(), null, placeholder.canRunAsync());
		this.placeholder = placeholder;
	}

	@Override
	protected void injectHD() {
		if (!this.placeholder.requiresPlayer()) {
			this.placeholder.injectHD();
		}
	}

	public LoadablePlaceholder getHandlingPlaceholder() {
		return this.placeholder;
	}

	@Override
	public Callable<String> getQuery(final Player player, final String message) {
		return () -> this.placeholder.process(player, message);
	}

	@Override
	public String getDescription() {
		return (this.placeholder.getFullDescription() == null) ? null : Joiner.on('\n').join(this.placeholder.getFullDescription());
	}
}