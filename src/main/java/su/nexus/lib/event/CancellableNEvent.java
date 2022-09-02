package su.nexus.lib.event;

import org.bukkit.event.Cancellable;

public abstract class CancellableNEvent
		extends NEvent
		implements Cancellable {
	private boolean cancelled = false;

	public CancellableNEvent() {
	}

	public CancellableNEvent(boolean async) {
		super(async);
	}

	public boolean isCancelled() {
		return this.cancelled;
	}

	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}
}
