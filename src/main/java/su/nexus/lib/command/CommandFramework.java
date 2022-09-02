package su.nexus.lib.command;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandFramework implements CommandExecutor, TabCompleter {
	static final Splitter DOT_SPLITTER;
	private static final MethodHandle PLUGINCOMMAND_CONSTRUCTOR;
	private static final MethodHandle REGISTER_ALIAS;

	static {
		DOT_SPLITTER = Splitter.on('.');
		try {
			MethodHandles.Lookup l = MethodHandles.lookup();
			Constructor<PluginCommand> pcCons = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
			pcCons.setAccessible(true);
			PLUGINCOMMAND_CONSTRUCTOR = l.unreflectConstructor(pcCons);
			Method register = SimpleCommandMap.class.getDeclaredMethod("register", String.class, org.bukkit.command.Command.class, Boolean.TYPE, String.class);
			register.setAccessible(true);
			REGISTER_ALIAS = l.unreflect(register);
		} catch (Exception t) {
			throw new RuntimeException(t);
		}
	}

	private final Map<String, Executor> executors;
	private final Plugin plugin;
	private Map<String, org.bukkit.command.Command> knownBukkitCommands;
	private SimpleCommandMap map;
	private SimplePluginManager manager;

	public CommandFramework(Plugin plugin) {
		this(plugin, false);
	}

	public CommandFramework(Plugin plugin, boolean internal) {
		if (!internal) {
			new UnsupportedOperationException(plugin.getName() + " нужно обновить, в нём старый метод регистрации команд. Это просто напоминание").printStackTrace();
		}
		this.plugin = (Plugin) Preconditions.checkNotNull((Object) plugin, "Plugin cannot be null");
		this.executors = new ConcurrentHashMap<String, Executor>();
		if (plugin.getServer().getPluginManager() instanceof SimplePluginManager) {
			this.manager = (SimplePluginManager) plugin.getServer().getPluginManager();
			try {
				Field field = SimplePluginManager.class.getDeclaredField("commandMap");
				field.setAccessible(true);
				this.map = (SimpleCommandMap) field.get(this.manager);
				Field knownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
				knownCommands.setAccessible(true);
				this.knownBukkitCommands = (Map) knownCommands.get(this.map);
			} catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
				plugin.getLogger().log(Level.SEVERE, "Failed to initialize command framework", e);
			}
		}
	}

	public static CommandFramework create(Plugin plugin) {
		return new CommandFramework(plugin, true);
	}

	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		return this.handleCommand(sender, cmd, label, args);
	}

	public boolean handleCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		Executor executor = this.findExecutor(label = label.contains(":") ? label.split(":")[1] : label, args);
		if (executor == null) {
			this.defaultCommand(new CommandArgs(sender, cmd, label, args));
			return true;
		}
		if (sender instanceof ConsoleCommandSender && !executor.canConsoleUse()) {
			sender.sendMessage("[" + this.plugin.getName() + "] Only in-game command!");
			return true;
		}
		if (!executor.hasPermission(sender)) {
			executor.getNoPermissionMessage().tag("permission", executor.getPermission()).send(sender);
			return true;
		}
		String match = executor.getMatch(label, args);
		int dotCount = StringUtils.countMatches(match, ".");
		String[] realArgs = dotCount > args.length ? new String[0] : Arrays.copyOfRange(args, dotCount, args.length);
		try {
			executor.execute(new CommandArgs(sender, cmd, label, realArgs));
		} catch (Throwable e) {
			this.plugin.getLogger().log(Level.SEVERE, "Got exception executing command " + label + " " + Arrays.toString(args), e);
		}
		return true;
	}

	private Executor findExecutor(String label, String[] args) {
		label = label.toLowerCase();
		List<String> all = new ArrayList<>();
		all.add(label);
		Arrays.stream(args).map(String::toLowerCase).forEachOrdered(all::add);
		Joiner joiner = Joiner.on('.');
		while (!all.isEmpty()) {
			String joined = joiner.join(all);
			if (this.executors.containsKey(joined)) {
				return this.executors.get(joined);
			}
			all.remove(all.size() - 1);
		}
		return null;
	}

	public void registerCommands(Object obj) {
		for (Method m : obj.getClass().getMethods()) {
			if (m.getAnnotation(Command.class) == null) continue;
			Command command = m.getAnnotation(Command.class);
			if (m.getParameterTypes().length != 1 || m.getParameterTypes()[0] != CommandArgs.class) {
				System.out.println("Unable to register command " + m.getName() + ". Unexpected method arguments");
				continue;
			}
			Executor exec = new Executor(this.plugin, obj, command, m);
			this.registerExecutor(exec);
		}
	}

	private void registerExecutor(Executor exec) {
		PluginCommand mainCommand;
		String cmdName = exec.getMainCommandName();
		org.bukkit.command.Command cmd = this.map.getCommand(cmdName);
		if (!(cmd == null || cmd instanceof PluginCommand && this.isThisPlugin(((PluginCommand) cmd).getPlugin()))) {
			String holder = cmd instanceof PluginCommand ? ((PluginCommand) cmd).getPlugin().getName() : cmd.getClass().getName();
			this.plugin.getLogger().info(() -> "Unregistering command " + cmdName + " of " + holder);
			this.unregisterCommand(cmd);
			cmd = null;
		}
		if (cmd == null) {
			mainCommand = this.construct(cmdName, this.plugin);
			mainCommand.getAliases().addAll(exec.getAliasesNames());
			if (!this.map.register(this.plugin.getName(), mainCommand)) {
				this.plugin.getLogger().warning(() -> "Failed to register plugin command " + mainCommand);
			}
		} else {
			mainCommand = (PluginCommand) cmd;
			exec.getAliasesNames().stream().filter(al -> !mainCommand.getAliases().contains(al)).forEach(newAlias -> {
				mainCommand.getAliases().add(newAlias);
				try {
					boolean result = (boolean) REGISTER_ALIAS.invoke(this.map, (String) newAlias, mainCommand, true, this.plugin.getName());
					if (!result) {
						this.plugin.getLogger().warning("Failed to register alias " + newAlias + " for command " + cmdName);
					}
				} catch (Throwable e) {
					this.plugin.getLogger().log(Level.SEVERE, "Cannot register alias " + newAlias + " for command " + cmdName, e);
				}
			});
		}
		Stream.concat(Stream.of(exec.getName()), exec.getAliases().stream()).forEach(fullCommand -> this.executors.put(fullCommand, exec));
		mainCommand.setExecutor(this);
		mainCommand.setTabCompleter(this);
	}

	public Map<String, Executor> getExecutors() {
		return this.executors;
	}

	public Executor getExecutor(String cmdName) {
		return this.executors.get(cmdName);
	}

	public void registerCommand(Executor executor, String label, Method m, Object obj) {
		String cmdLabel = label.split("\\.")[0].toLowerCase();
		org.bukkit.command.Command command = this.map.getCommand(cmdLabel);
		PluginCommand pluginCommand = null;
		if (command != null) {
			if (command instanceof PluginCommand) {
				pluginCommand = (PluginCommand) command;
			}
			if (pluginCommand == null || !this.isThisPlugin(pluginCommand.getPlugin())) {
				this.unregisterCommand(command);
				pluginCommand = null;
			}
		}
		if (pluginCommand == null) {
			pluginCommand = this.construct(cmdLabel, this.plugin);
			this.map.register(this.plugin.getName(), pluginCommand);
			this.knownBukkitCommands.put(cmdLabel, pluginCommand);
		}
		this.executors.put(label, executor);
		pluginCommand.setExecutor(this);
		pluginCommand.setTabCompleter(this);
	}

	private boolean isThisPlugin(Plugin plugin) {
		return plugin != null && this.plugin.getName().equals(plugin.getName());
	}

	private PluginCommand construct(String label, Plugin plugin2) {
		try {
			return (PluginCommand) PLUGINCOMMAND_CONSTRUCTOR.invoke(label, plugin2);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public void unregisterCommand(String name) {
		this.executors.keySet().stream().filter(k -> this.executors.get(k).getName().split("\\.")[0].equals(name)).collect(Collectors.toList()).forEach(label -> {
			String cmdName = label.split("\\.")[0];
			PluginCommand c = Bukkit.getPluginCommand(cmdName);
			this.unregisterCommand(c);
			this.executors.remove(label);
		});
	}

	public void unregisterCommand(org.bukkit.command.Command c) {
		if (c == null) {
			return;
		}
		c.unregister(this.map);
		this.knownBukkitCommands.remove(c.getName());
		this.knownBukkitCommands.remove(this.getFallbackPrefix(c) + ":" + c.getName());
		for (String alias : c.getAliases()) {
			this.knownBukkitCommands.remove(alias);
			this.knownBukkitCommands.remove(this.getFallbackPrefix(c) + ":" + alias);
		}
	}

	private String getFallbackPrefix(org.bukkit.command.Command c) {
		if (c instanceof PluginCommand) {
			return ((PluginCommand) c).getPlugin().getName().toLowerCase();
		}
		return "minecraft";
	}

	public void unregisterAll() {
		ImmutableMap.copyOf(this.knownBukkitCommands).values().stream().filter(PluginCommand.class::isInstance).map(PluginCommand.class::cast).filter(cmd -> cmd.getPlugin().getName().equals(this.plugin.getName())).forEach(this::unregisterCommand);
	}

	public void registerHelp() {
	}

	public void registerCompleter(String label, Method m, Object obj) {
		PluginCommand command;
		String cmdLabel = label.split("\\.")[0].toLowerCase();
		if (this.map.getCommand(cmdLabel) == null) {
			command = this.construct(cmdLabel, this.plugin);
			this.map.register(this.plugin.getName(), command);
		}
		if (this.map.getCommand(cmdLabel) instanceof PluginCommand) {
			try {
				command = (PluginCommand) this.map.getCommand(cmdLabel);
				Field field = command.getClass().getDeclaredField("completer");
				field.setAccessible(true);
				if (field.get(command) == null) {
					BukkitCompleter completer = new BukkitCompleter();
					completer.addCompleter(label, m, obj);
					field.set(command, completer);
				} else if (field.get(command) instanceof BukkitCompleter) {
					BukkitCompleter completer = (BukkitCompleter) field.get(command);
					completer.addCompleter(label, m, obj);
				} else {
					this.plugin.getLogger().warning("Unable to register tab completer " + m.getName() + ". A tab completer is already registered for that command!");
				}
			} catch (Exception ex) {
				this.plugin.getLogger().log(Level.SEVERE, "Failed to register completer of " + label, ex);
			}
		}
	}

	private void defaultCommand(CommandArgs args) {
		args.getSender().sendMessage(args.getLabel() + " is not handled! Oh noes!");
	}

	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		String full = Joiner.on('.').join(Stream.concat(Stream.of(label), Stream.of(args)).collect(Collectors.toList())).toLowerCase().trim();
		int playerDotCount = this.countDots(full);
		List<String> result = this.executors.keySet().stream().filter(c -> c.startsWith(full)).sorted(Comparator.naturalOrder()).filter(c -> {
			Executor e = this.executors.get(c);
			if (e.getPermission().isEmpty()) {
				return true;
			}
			return sender.hasPermission(e.getPermission());
		}).map(DOT_SPLITTER::split).filter(c -> Iterables.size(c) >= playerDotCount).map(c -> Iterables.get(c, playerDotCount)).distinct().collect(Collectors.toList());
		return result.isEmpty() ? null : result;
	}

	private int countDots(String str) {
		int count = 0;
		for (int i = 0; i < str.length(); ++i) {
			if (str.charAt(i) != '.') continue;
			++count;
		}
		return count;
	}

	private List<String> getLevels(CommandSender sender, String label, String[] args) {
		ArrayList<String> all = new ArrayList<>();
		all.add(label);
		all.addAll(Arrays.asList(args));
		String joined = Joiner.on('.').join(all).trim().toLowerCase();
		ArrayList<String> matches = new ArrayList<>();
		Set<String> m = new HashSet<>();
		this.executors.values().parallelStream().filter(exec -> exec.getName().startsWith(joined) || exec.getAliases().stream().anyMatch(al -> al.startsWith(joined))).filter(exec -> this.hasPermission(sender, exec)).forEach(exec -> {
			if (exec.getName().startsWith(joined)) {
				m.add(this.getLast(exec.getName()));
			}
			exec.getAliases().stream().filter(alias -> alias.startsWith(joined)).forEach(alias -> m.add(this.getLast(alias)));
		});
		matches.addAll(m);
		return matches.isEmpty() ? null : matches;
	}

	public boolean hasPermission(CommandSender sender, Executor executor) {
		return executor.getPermission().isEmpty() || sender.hasPermission(executor.getPermission());
	}

	private String getLast(String name) {
		String[] spl = name.split("\\.");
		return spl[spl.length - 1];
	}
}