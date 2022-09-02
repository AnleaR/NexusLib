package su.nexus.lib.event;

import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public abstract class NEvent
		extends Event {
	private static final Map<String, HandlerList> perClassHandlers = new HashMap<String, HandlerList>();

	public NEvent() {
		Class<?> clazz = this.getClass();
		if (NEvent.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
			try {
				clazz.getDeclaredMethod("getHandlerList");
			} catch (NoSuchMethodException ignored) {}
		} else {
			System.out.println("Instantiated " + clazz.getName() + ". Shto...");
		}
	}

	public NEvent(boolean async) {
		super(async);
		Class<?> clazz = ((Object) this).getClass();
		if (NEvent.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
			try {
				clazz.getDeclaredMethod("getHandlerList");
			} catch (NoSuchMethodException ignored) {}
		} else {
			System.out.println("Instantiated " + clazz.getName() + ". Shto...");
		}
	}

	//@CallerSensitive
	protected static HandlerList getOrCreateHandlerList() {
		for (int i = 0; i < 10; ++i) {
			//Class<?> c = Reflection.getCallerClass(i);
			int finalI = i;
			Class<?> c = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(s ->
					s.map(StackWalker.StackFrame::getDeclaringClass).skip(finalI).findFirst().orElse(null));
			if (c.equals(NEvent.class) || !NEvent.class.isAssignableFrom(c)) continue;
			return NEvent.getOrCreateHandlerList(c);
		}
		throw new IllegalStateException("Did not found caller class");
	}

	public static HandlerList getHandlerList() {
		return new HandlerList();
	}

	private static HandlerList getOrCreateHandlerList(Class<?> c) {
		Preconditions.checkArgument((!c.equals(NEvent.class) ? 1 : 0) != 0, "Got CEvent class");
		Preconditions.checkArgument(NEvent.class.isAssignableFrom(c), "Got invalid class " + c.getName());
		HandlerList list = perClassHandlers.get(c.getName());
		if (list == null) {
			list = new HandlerList();
			perClassHandlers.put(c.getName(), list);
		}
		return list;
	}

	public <T extends NEvent> T call() {
		Bukkit.getPluginManager().callEvent(this);
		return (T) this;
	}

	public HandlerList getHandlers() {
		return NEvent.getOrCreateHandlerList(((Object) this).getClass());
	}
}