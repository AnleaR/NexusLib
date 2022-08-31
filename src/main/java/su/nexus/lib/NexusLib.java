package su.nexus.lib;

import lombok.Getter;
import org.mineacademy.fo.plugin.SimplePlugin;
import su.nexus.lib.economy.UniversalEconomyService;

public class NexusLib {

	@Getter
	public static SimplePlugin Instance;

	public static void init(final SimplePlugin plugin) {
		Instance = plugin;
		UniversalEconomyService.start(Instance);
	}
}