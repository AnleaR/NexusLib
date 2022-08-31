package su.nexus.lib.placeholders.custom;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.bukkit.entity.Player;
import org.mineacademy.fo.plugin.SimplePlugin;
import su.nexus.lib.NexusLib;

import java.io.File;
import java.util.Collection;
import java.util.List;

public abstract class LoadablePlaceholder {
	private PlaceholdersClassLoader loader;
	private String id;
	private String version;
	private String desc;
	private String author;
	private List<String> depend;
	private boolean requiresPlayer;
	private boolean canRunAsync;
	private File file;

	public LoadablePlaceholder() {
		this.check();
	}

	private void check() {
		Preconditions.checkState(this.getClass().getClassLoader() instanceof PlaceholdersClassLoader, "Do not use this class in your plugins.");
	}

	public abstract String process(final Player p0, final String p1);

	public void init() {
	}

	public void injectHD() {
	}

	public final String getAuthor() {
		return this.author;
	}

	public final List<String> getDependencies() {
		return this.depend;
	}

	public final String getDescription() {
		return this.desc;
	}

	public final String getId() {
		return this.id;
	}

	public final File getFile() {
		return this.file;
	}

	public final void setFile(final File file) {
		Preconditions.checkState(this.file == null);
		this.file = file;
	}

	public final SimplePlugin lib() {
		return NexusLib.getInstance();
	}

	public final PlaceholdersClassLoader getLoader() {
		return this.loader;
	}

	public final String getVersion() {
		return this.version;
	}

	public final boolean canRunAsync() {
		return this.canRunAsync;
	}

	public final boolean requiresPlayer() {
		return this.requiresPlayer;
	}

	final void init(final PlaceholdersClassLoader loader, final String id, final String version, final String desc, final String author, final List<String> depend, final boolean requiresPlayer, final boolean canRunAsync) {
		this.check();
		Preconditions.checkArgument(this.loader == null, "Attempting to init already initialized placeholder");
		this.loader = loader;
		this.id = id;
		this.version = version;
		this.desc = desc;
		this.author = author;
		this.depend = (List<String>) ImmutableList.copyOf((Collection) depend);
		this.requiresPlayer = requiresPlayer;
		this.canRunAsync = canRunAsync;
	}

	public List<String> getFullDescription() {
		return null;
	}
}