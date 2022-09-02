package su.nexus.lib.command;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import su.nexus.lib.event.CommandPostExecutionEvent;
import su.nexus.lib.event.CommandPreExecutionEvent;
import su.nexus.lib.message.Message;
import su.nexus.lib.message.MessageRegistry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Executor {
	private final Object container;
	private final MethodHandle method;
	private final Plugin plugin;
	private final String name;
	private final List<String> aliases;
	private final String permission;
	private final String noPermMessage;
	private final boolean inGameOnly;

	public Executor(Plugin p, Object cc, Command ann, Method meth) {
		this(p, cc, meth, ((Command) Preconditions.checkNotNull((Object) ann, "annotation")).name(), Arrays.asList(ann.aliases()), ann.permission(), ann.noPerm(), ann.inGameOnly());
	}

	public Executor(Plugin plugin, Object container, Method method, String name, List<String> aliases, String permission, String noPermMessage, boolean inGameOnly) {
		Preconditions.checkNotNull((Object) plugin, "plugin");
		Preconditions.checkNotNull(container, "container");
		Preconditions.checkNotNull((Object) method, "method");
		Preconditions.checkNotNull((Object) name, "name");
		Preconditions.checkNotNull(aliases, "aliases");
		Preconditions.checkNotNull((Object) permission, "permission");
		Preconditions.checkNotNull((Object) noPermMessage, "noPermMessage");
		this.container = container;
		try {
			method.setAccessible(true);
			this.method = MethodHandles.lookup().unreflect(method);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		this.plugin = plugin;
		this.name = name;
		this.aliases = ImmutableList.copyOf(aliases);
		this.permission = permission;
		this.noPermMessage = noPermMessage;
		this.inGameOnly = inGameOnly;
	}

	public String getName() {
		return this.name;
	}

	public String getMainCommandName() {
		return CommandFramework.DOT_SPLITTER.splitToList(this.name).get(0).toLowerCase();
	}

	public List<String> getAliases() {
		return this.aliases;
	}

	public List<String> getAliasesNames() {
		return this.aliases.stream().map(String::toLowerCase).map(((Splitter) CommandFramework.DOT_SPLITTER)::splitToList).map(l -> l.get(0)).distinct().filter(al -> !al.equalsIgnoreCase(this.getMainCommandName())).collect(Collectors.toList());
	}

	public MethodHandle getMethod() {
		return this.method;
	}

	public boolean canConsoleUse() {
		return !this.inGameOnly;
	}

	public String getMatch(String label, String[] args) {
		label = label.toLowerCase();
		List<String> all = new ArrayList<>();
		all.add(label);
		Arrays.asList(args).stream().map(String::toLowerCase).forEachOrdered(all::add);
		Joiner joiner = Joiner.on('.');
		while (!all.isEmpty()) {
			String joined = joiner.join(all);
			if (this.getName().equals(joined) || this.getAliases().contains(joined)) {
				return joined;
			}
			all.remove(all.size() - 1);
		}
		return null;
	}

	public void execute(CommandArgs context) throws Throwable {
		block5:
		{
			CommandPreExecutionEvent pre = new CommandPreExecutionEvent(context).call();
			if (pre.isCancelled()) {
				return;
			}
			context = pre.getContext();
			try {
				if (context.isPlayer() && !context.getPlayer().isOnline()) {
					return;
				}
				this.getMethod().invoke(this.container, context);
				new CommandPostExecutionEvent(context, CommandPostExecutionEvent.Result.COMPLETE).call();
			} catch (CommandException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				Message message = null;
				if (!(e instanceof CommandException)) {
					new CommandPostExecutionEvent(context, CommandPostExecutionEvent.Result.ERROR).call();
					throw new RuntimeException(e);
				}
				message = ((CommandException) e).getCommandMessage();
				new CommandPostExecutionEvent(context, CommandPostExecutionEvent.Result.HALTED).call();
				if (message == null) break block5;
				message.send(context.getSender());
			}
		}
	}

	public String getPermission() {
		return this.permission;
	}

	public Message getNoPermissionMessage() {
		if (this.noPermMessage.isEmpty()) {
			Message message = MessageRegistry.message(this.getPlugin().getName().toLowerCase() + ".nopermission");
			if (!message.exists()) {
				message = new Message(this.plugin.getName() + "." + this.getName() + ".nopermission", "You do not have permission to perform that action");
			}
			return message;
		}
		return Message.create(this.plugin.getName() + "." + this.getName() + ".nopermission", this.noPermMessage);
	}

	public String getPermissionMessage() {
		return this.noPermMessage;
	}

	public Plugin getPlugin() {
		return this.plugin;
	}

	public boolean hasPermission(CommandSender sender) {
		return StringUtils.isEmpty(this.permission) || sender.hasPermission(this.permission);
	}
}