package su.nexus.lib.command;

import org.bukkit.plugin.Plugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

public class CommandManager {
	private final Map<String, SubCommand> rootCommands = new HashMap<>();

	public void registerCommands(Plugin plugin, CommandsContainer... containers) {
		for (CommandsContainer container : containers) {
			for (Method method : container.getClass().getMethods()) {
				Command annotation = this.getIfCommandMethod(method);
				if (annotation == null) continue;

				try {
					MethodHandles.lookup().unreflect(method);
				} catch (IllegalAccessException e) {
					plugin.getLogger().log(Level.SEVERE, "Unable to register method " + method.getName() + " from " + container.getClass().getName() + " as command", e);
				}
			}
		}
	}

	private SubCommand createSubCommand(String fullCmd, boolean alias) {
		String[] split = fullCmd.toLowerCase().split(fullCmd.contains(".") ? "\\." : " ");
		return null;
	}

	public SubCommand getCommand(String name) {
		String[] split = name.toLowerCase().split(name.contains(".") ? "\\." : " ");
		SubCommand cmd = this.rootCommands.get(split[0]);
		if (split.length == 1) {
			return cmd;
		}
		for (int i = 1; i < split.length; ++i) {
			if ((cmd = cmd.getChild(split[i])) != null) continue;
			return null;
		}
		return cmd;
	}

	private Consumer<CommandArguments> toConsumer(CommandsContainer container, MethodHandle handle) {
		return args -> {
			try {
				handle.invoke(container, (CommandArguments) args);
			} catch (Throwable e) {
				throw new RuntimeException(e.getCause());
			}
		};
	}

	private Command getIfCommandMethod(Method method) {
		int mod = method.getModifiers();
		if (Modifier.isPublic(mod) && !Modifier.isStatic(mod) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(CommandArguments.class)) {
			return method.getAnnotation(Command.class);
		}
		return null;
	}
}

