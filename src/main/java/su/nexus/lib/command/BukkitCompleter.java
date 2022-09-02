package su.nexus.lib.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BukkitCompleter
		implements TabCompleter {
	private final Map<String, Map.Entry<Method, Object>> completers = new HashMap<String, Map.Entry<Method, Object>>();
	private Plugin plugin;

	public void addCompleter(String label, Method m, Object obj) {
		this.completers.put(label, new AbstractMap.SimpleEntry<>(m, obj));
	}

	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		for (int i = args.length; i >= 0; --i) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(label.toLowerCase());
			for (int x = 0; x < i; ++x) {
				if (args[x].equals("") || args[x].equals(" ")) continue;
				buffer.append(".").append(args[x].toLowerCase());
			}
			String cmdLabel = buffer.toString();
			if (!this.completers.containsKey(cmdLabel)) continue;
			Map.Entry<Method, Object> entry = this.completers.get(cmdLabel);
			try {
				return (List<String>) entry.getKey().invoke(entry.getValue(), new CommandArgs(sender, command, label, args));
			} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
}