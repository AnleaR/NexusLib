/*
 * Decompiled with CFR 0.150.
 *
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  javax.annotation.Nonnull
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package su.nexus.lib.command;

import com.google.common.collect.Iterables;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nexus.lib.message.Message;
import su.nexus.lib.message.MessageKey;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Supplier;

public class CommandArgs {

	@Getter
	private final CommandSender sender;
	@Getter
	private final Command command;
	@Getter
	private final String label;
	@Getter
	private final String[] args;

	protected CommandArgs(CommandSender sender, Command command, String label, String[] args) {
		this.sender = sender;
		this.command = command;
		this.label = label;
		this.args = args;
	}

	public static CommandArgs create(CommandSender sender, Command command, String label, String[] args) {
		return new CommandArgs(sender, command, label, args);
	}

	public String getArgs(int index) {
		return this.args[index];
	}

	public int length() {
		return this.args.length;
	}

	public String getName() {
		return this.sender.getName();
	}

	public boolean isPlayer() {
		return this.sender instanceof Player;
	}

	public void sendMessage(String message) {
		this.sender.sendMessage(message);
	}

	public void sendMessage(String format, Object... values) {
		this.sendMessage(this.format(format, values));
	}

	private String format(String format, Object... values) {
		for (int i = 0; i < values.length; ++i) {
			format = format.replace("$" + (i + 1), String.valueOf(values[i]));
		}
		return format;
	}

	public void sendMessage(Message message) {
		message.send(this.sender);
	}

	public void sendMessage(MessageKey messageKey) {
		this.sendMessage(messageKey.message());
	}

	public Player getPlayer() {
		if (this.sender instanceof Player) {
			return (Player) this.sender;
		}
		return null;
	}

	public void halt() {
		this.halt((Message) null);
	}

	public void halt(String message) {
		throw new CommandException(message);
	}

	public void halt(String format, Object... values) {
		this.halt(this.format(format, values));
	}

	public void halt(Message message) {
		throw new CommandException(message);
	}

	public void halt(Supplier<Message> message) {
		this.halt(message.get());
	}

	public void halt(MessageKey messageKey) {
		throw new CommandException(messageKey);
	}

	public void checkState(boolean state, String message) {
		if (!state) {
			this.halt(message);
		}
	}

	public void checkState(boolean state, String format, Object... values) {
		if (!state) {
			this.halt(format, values);
		}
	}

	public void checkState(boolean state, Message message) {
		if (!state) {
			this.halt(message);
		}
	}

	public void checkState(boolean state, Supplier<Message> message) {
		if (!state) {
			this.halt(message.get());
		}
	}

	public void checkState(boolean state, MessageKey message) {
		if (!state) {
			this.halt(message);
		}
	}

	@Nonnull
	public <T> T checkNotNull(T obj, String message) {
		if (obj == null) {
			this.halt(message);
			throw new NullPointerException();
		}
		return obj;
	}

	@Nonnull
	public <T> T checkNotNull(T obj, String format, Object... values) {
		if (obj == null) {
			this.halt(format, values);
			throw new NullPointerException();
		}
		return obj;
	}

	@Nonnull
	public <T> T checkNotNull(T obj, Message message) {
		if (obj == null) {
			this.halt(message);
			throw new NullPointerException();
		}
		return obj;
	}

	@Nonnull
	public <T> T checkNotNull(T obj, Supplier<Message> message) {
		if (obj == null) {
			this.halt(message);
			throw new NullPointerException();
		}
		return obj;
	}

	@Nonnull
	public <T> T checkNotNull(T obj, MessageKey message) {
		if (obj == null) {
			this.halt(message);
			throw new NullPointerException();
		}
		return obj;
	}

	public <T> Optional<T> checkPresent(Optional<T> opt, String message) {
		this.checkState(opt.isPresent(), message);
		return opt;
	}

	public <T> Optional<T> checkPresent(Optional<T> opt, String format, Object... values) {
		this.checkState(opt.isPresent(), format, values);
		return opt;
	}

	public <T> Optional<T> checkPresent(Optional<T> opt, Message message) {
		this.checkState(opt.isPresent(), message);
		return opt;
	}

	public <T> Optional<T> checkPresent(Optional<T> opt, Supplier<Message> message) {
		this.checkState(opt.isPresent(), message);
		return opt;
	}

	public <T> Optional<T> checkPresent(Optional<T> opt, MessageKey message) {
		this.checkState(opt.isPresent(), message);
		return opt;
	}

	public void checkNotPresent(Optional opt, String message) {
		this.checkState(!opt.isPresent(), message);
	}

	public void checkNotPresent(Optional opt, String format, Object... values) {
		this.checkState(!opt.isPresent(), format, values);
	}

	public void checkNotPresent(Optional opt, Message message) {
		this.checkState(!opt.isPresent(), message);
	}

	public void checkNotPresent(Optional opt, Supplier<Message> message) {
		this.checkState(!opt.isPresent(), message);
	}

	public void checkNotPresent(Optional opt, MessageKey message) {
		this.checkState(!opt.isPresent(), message);
	}

	public void checkNull(Object obj, String message) {
		if (obj != null) {
			this.halt(message);
		}
	}

	public void checkNull(Object obj, String format, Object... values) {
		if (obj != null) {
			this.halt(format, values);
		}
	}

	public void checkNull(Object obj, Message message) {
		if (obj != null) {
			this.halt(message);
		}
	}

	public void checkNull(Object obj, Supplier<Message> message) {
		if (obj != null) {
			this.halt(message);
		}
	}

	public void checkNull(Object obj, MessageKey message) {
		if (obj != null) {
			this.halt(message);
		}
	}

	public <T extends Iterable<V>, V> T checkNotEmpty(T obj, String message) {
		this.checkState(!Iterables.isEmpty(obj), message);
		return obj;
	}

	public <T extends Iterable<V>, V> T checkNotEmpty(T obj, String format, Object... values) {
		this.checkState(!Iterables.isEmpty(obj), format, values);
		return obj;
	}

	public <T extends Iterable<V>, V> T checkNotEmpty(T obj, Message message) {
		this.checkState(!Iterables.isEmpty(obj), message);
		return obj;
	}

	public <T extends Iterable<V>, V> T checkNotEmpty(T obj, Supplier<Message> message) {
		this.checkState(!Iterables.isEmpty(obj), message);
		return obj;
	}

	public <T extends Iterable<V>, V> T checkNotEmpty(T obj, MessageKey message) {
		this.checkState(!Iterables.isEmpty(obj), message);
		return obj;
	}

	public void checkEmpty(Iterable<?> obj, String message) {
		this.checkState(Iterables.isEmpty(obj), message);
	}

	public void checkEmpty(Iterable<?> obj, String format, Object... values) {
		this.checkState(Iterables.isEmpty(obj), format, values);
	}

	public void checkEmpty(Iterable<?> obj, Message message) {
		this.checkState(Iterables.isEmpty(obj), message);
	}

	public void checkEmpty(Iterable<?> obj, Supplier<Message> message) {
		this.checkState(Iterables.isEmpty(obj), message);
	}

	public void checkEmpty(Iterable<?> obj, MessageKey message) {
		this.checkState(Iterables.isEmpty(obj), message);
	}

	public void checkArgs(int min, String message) {
		this.checkState(this.length() >= min, message);
	}

	public void checkArgs(int min, String format, Object... values) {
		this.checkState(this.length() >= min, format, values);
	}

	public void checkArgs(int min, Message message) {
		this.checkState(this.length() >= min, message);
	}

	public void checkArgs(int min, Supplier<Message> message) {
		this.checkState(this.length() >= min, message);
	}

	public void checkArgs(int min, MessageKey message) {
		this.checkState(this.length() >= min, message);
	}

	public Player checkOnline(String playerName, String message) {
		return this.checkNotNull(Bukkit.getPlayerExact(playerName), message);
	}

	public Player checkOnline(String playerName, String format, Object... values) {
		return this.checkNotNull(Bukkit.getPlayerExact(playerName), format, values);
	}

	public Player checkOnline(String playerName, Message message) {
		return this.checkNotNull(Bukkit.getPlayerExact(playerName), message);
	}

	public Player checkOnline(String playerName, Supplier<Message> message) {
		return this.checkNotNull(Bukkit.getPlayerExact(playerName), message);
	}

	public Player checkOnline(String playerName, MessageKey message) {
		return this.checkNotNull(Bukkit.getPlayerExact(playerName), message);
	}
}

