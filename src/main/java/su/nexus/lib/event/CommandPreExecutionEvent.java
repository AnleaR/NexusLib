package su.nexus.lib.event;

import com.google.common.base.Preconditions;
import org.bukkit.event.HandlerList;
import su.nexus.lib.command.CommandArgs;

public class CommandPreExecutionEvent
		extends CancellableNEvent {
	private CommandArgs context;

	public CommandPreExecutionEvent(CommandArgs context) {
		this.context = context;
	}

	public static HandlerList getHandlerList() {
		return CommandPreExecutionEvent.getOrCreateHandlerList();
	}

	public CommandArgs getContext() {
		return this.context;
	}

	public void setContext(CommandArgs context) {
		this.context = Preconditions.checkNotNull(context, "Context");
	}
}