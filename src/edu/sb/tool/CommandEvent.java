package edu.sb.tool;


/**
 * Instances of this class represent events caused
 * by command shell interactions.
 */
@Copyright(year=2021, holders="Sascha Baumeister")
public class CommandEvent {
	private final String command;
	private final String arguments;


	/**
	 * Initializes a new instance.
	 * @param command the command
	 * @param arguments the arguments
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	public CommandEvent (final String command, final String arguments) throws NullPointerException {
		if (command == null | arguments == null) throw new NullPointerException();
		this.command = command;
		this.arguments = arguments;
	}


	/**
	 * Returns the command.
	 * @return the command
	 */
	public String command () {
		return this.command;
	}


	/**
	 * Returns the arguments.
	 * @return the arguments
	 */
	public String arguments () {
		return this.arguments;
	}
}
