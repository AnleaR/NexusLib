package su.nexus.lib;

import lombok.Getter;
import lombok.Setter;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.plugin.SimplePlugin;
import su.nexus.lib.economy.UniversalEconomyService;

public final class NexusLibPlugin extends SimplePlugin {

	@Getter @Setter
	public static UniversalEconomyService economyService;

	/**
	* Automatically perform login ONCE when the plugin starts.
	*/
	@Override
	protected void onPluginStart() {

	}

	/**
	 * Automatically perform login when the plugin starts and each time it is reloaded.
	 */
	@Override
	protected void onReloadablesStart() {
		Valid.checkBoolean(HookManager.isVaultLoaded(), "You need to install Vault so that we can work with packets, offline player data, prefixes and groups.");
		UniversalEconomyService.start();

		Valid.checkBoolean(HookManager.isProtocolLibLoaded(), "You need to install ProtocolLib so that we can work with packets and protocols.");
		Valid.checkBoolean(HookManager.isPlaceholderAPILoaded(), "You need to install PlaceholderAPI so that we can work with placeholders.");
	}

	/* ------------------------------------------------------------------------------- */
	/* Static */
	/* ------------------------------------------------------------------------------- */

	/**
	 * Return the instance of this plugin, which simply refers to a static
	 * field already created for you in SimplePlugin but casts it to your
	 * specific plugin instance for your convenience.
	 *
	 * @return Instance of the plugin
	 */
	public static NexusLibPlugin getInstance() {
		return (NexusLibPlugin) SimplePlugin.getInstance();
	}

	public static boolean isEconomyReady() {
		return economyService != null;
	}
}
