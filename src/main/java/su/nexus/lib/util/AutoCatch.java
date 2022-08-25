package su.nexus.lib.util;

public interface AutoCatch {

	static void run(final AutoCatch r) {
		try {
			r.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	static void suppress(final AutoCatch r) {
		try {
			r.run();
		} catch (Throwable ignored) {
		}
	}

	void run() throws Throwable;
}