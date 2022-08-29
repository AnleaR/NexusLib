package su.nexus.lib.command;

import java.util.Collection;
import java.util.function.Consumer;

public class SubCommand {
	private SubCommand parent;
	private Collection<SubCommand> children;
	private String name;
	private Collection<String> aliases;
	private boolean inGameOnly;
	private Consumer<CommandArguments> executor;

	public String getName() {
		return this.name;
	}

	public SubCommand getParent() {
		return this.parent;
	}

	public boolean isInGameOnly() {
		return this.inGameOnly;
	}

	public Collection<SubCommand> getChildren() {
		return this.children;
	}

	public boolean isRegistered() {
		return this.executor != null;
	}

	public Collection<String> getAliases() {
		return this.aliases;
	}

	public SubCommand getChild(String name) {
		return this.children.stream().filter(sc -> sc.getName().equalsIgnoreCase(name)).findFirst().orElse(this);
	}

	public SubCommand getRoot() {
		if (this.parent == null) {
			return this;
		}
		return this.parent.getRoot();
	}
}

