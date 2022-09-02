package su.nexus.lib;

import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.mineacademy.fo.plugin.SimplePlugin;
import su.nexus.lib.command.CommandFramework;
import su.nexus.lib.economy.UniversalEconomyService;
import su.nexus.lib.placeholders.ClibPAPIHook;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NexusLib {

	@Getter
	public static SimplePlugin Instance;

	private static Map<String, CommandFramework> commandFrameworks;

	static {
		commandFrameworks = Collections.synchronizedMap(new LinkedHashMap<>());
	}

	public static void init(final SimplePlugin plugin) {
		Instance = plugin;
		UniversalEconomyService.start(Instance);
	}

	public void registerCommands(final Object... commandsContainer) {
		Arrays.stream(commandsContainer).forEach(this.commands()::registerCommands);
	}

	public CommandFramework commands() {
		return commands(Instance);
	}

	public static CommandFramework commands(final Plugin p) {
		if (!commandFrameworks.containsKey(p.getName())) {
			commandFrameworks.put(p.getName(), CommandFramework.create(p));
		}
		return commandFrameworks.get(p.getName());
	}
}