package su.nexus.lib.command;


import su.nexus.lib.message.Message;
import su.nexus.lib.message.MessageKey;

public class CommandException extends RuntimeException {
	private final Message message;

	public CommandException(MessageKey message) {
		this.message = message.message();
	}

	public CommandException(Message message) {
		this.message = message;
	}

	public CommandException(String message) {
		this.message = new Message("command.exception", message);
	}

	public Message getCommandMessage() {
		return this.message;
	}
}

