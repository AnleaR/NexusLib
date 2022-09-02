package su.nexus.lib.event;

import org.bukkit.event.HandlerList;
import su.nexus.lib.command.CommandArgs;

public class CommandPostExecutionEvent
		extends NEvent {
	private final CommandArgs context;
	private final Result result;

	public CommandPostExecutionEvent(CommandArgs context, Result result) {
		this.context = context;
		this.result = result;
	}

	public static HandlerList getHandlerList() {
		return CommandPostExecutionEvent.getOrCreateHandlerList();
	}

	public Result getResult() {
		return this.result;
	}

	public CommandArgs getContext() {
		return this.context;
	}

	public enum Result {
		COMPLETE,
		HALTED,
		ERROR

	}
}

