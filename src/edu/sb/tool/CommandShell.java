package edu.sb.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;


/**
 * Instances of this class generate command events during execution of interactive command-line driven text applications.
 */
@Copyright(year = 2021, holders = "Sascha Baumeister")
public class CommandShell {

	/**
	 * This functional interface defines an operation that takes one
	 * text parameter, and may throw both checked and unchecked
	 * exceptions while processing said text.
	 */
	@FunctionalInterface
	static public interface EventListener {
		void execute (CommandEvent event) throws Exception;
	}

	/**
	 * This exception is thrown by event listeners to
	 * signal that command processing shall cease.
	 */
	static public class AbortException extends IOException {
		private static final long serialVersionUID = 1L;
	}


	private final Map<String,List<EventListener>> eventListeners;
	private EventListener defaultEventListener;
	private Consumer<Exception> exceptionHandler;
	private String prompt;


	/**
	 * Initializes this instance.
	 */
	public CommandShell () {
		this.eventListeners = new ConcurrentHashMap<>();
		this.defaultEventListener = event -> System.err.println("unsupported command: \"" + event.command() + "\"");
		this.exceptionHandler = exception -> System.err.println(exception.getClass().getSimpleName() + (exception.getMessage() == null ? "" : ": " + exception.getMessage()));
		this.prompt = "> ";
	}


	/**
	 * Returns the default event listener.
	 * @return the default event listener
	 */
	public EventListener getDefaultEventListener () {
		return this.defaultEventListener;
	}


	/**
	 * Sets the default event listener.
	 * @param defaultEventListener the default event listener
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	public void setDefaultEventListener (final EventListener defaultEventListener) throws NullPointerException {
		this.defaultEventListener = Objects.requireNonNull(defaultEventListener);
	}


	/**
	 * Returns the exception handler.
	 * @return the exception handler
	 */
	public Consumer<Exception> getExceptionHandler () {
		return this.exceptionHandler;
	}


	/**
	 * Sets the exception handler.
	 * @param exceptionHandler the exception handler
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	public void setExceptionHandler (final Consumer<Exception> exceptionHandler) throws NullPointerException {
		this.exceptionHandler = Objects.requireNonNull(exceptionHandler);
	}


	/**
	 * Returns the command prompt.
	 * @return the command prompt
	 */
	public String getPrompt () {
		return this.prompt;
	}


	/**
	 * Sets the command prompt.
	 * @param prompt the command prompt
	 * @throws NullPointerException if the given argument is {@code null}
	 */
	public void setPrompt (final String prompt) throws NullPointerException {
		if (prompt == null) throw new NullPointerException();
		this.prompt = prompt;
	}


	/**
	 * Registers the given event listener for the given kind of command events.
	 * @param command the (lower-case) command event type
	 * @param eventListener the event listener
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	public void addEventListener (final String command, final EventListener eventListener) throws NullPointerException {
		if (command == null | eventListener == null) throw new NullPointerException();
		this.eventListeners.computeIfAbsent(command.toLowerCase(), key -> new CopyOnWriteArrayList<>()).add(eventListener);
	}


	/**
	 * Unregisters the given event listener for the given kind of command events.
	 * @param command the (lower-case) command event type
	 * @param eventListener the event listener
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 */
	public void removeEventListener (final String command, final EventListener eventListener) throws NullPointerException {
		if (command == null | eventListener == null) throw new NullPointerException();
		this.eventListeners.getOrDefault(command.toLowerCase(), Collections.emptyList()).remove(eventListener);
	}


	/**
	 * Generates command events by repeatedly displaying a command prompt, waiting for a command input, and subsequently
	 * signaling a command event for said command input. This operation returns after an event listener throws a
	 * {@link AbortException} instance.
	 * @throws IOException if there is an I/O related problem reading from the console
	 */
	public void processCommands () throws IOException {
		final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

		while (true) {
			System.out.print(this.prompt);
			final String consoleLine = consoleReader.readLine().trim();

			final int delimiterPosition = consoleLine.indexOf(' ');
			final String command = (delimiterPosition == -1 ? consoleLine : consoleLine.substring(0, delimiterPosition)).trim().toLowerCase();
			final String arguments = (delimiterPosition == -1 ? "" : consoleLine.substring(delimiterPosition + 1)).trim();
			final CommandEvent event = new CommandEvent(command, arguments);

			final List<EventListener> commandListeners = this.eventListeners.getOrDefault(command, Collections.singletonList(this.defaultEventListener));
			for (final EventListener eventListener : commandListeners) {
				try {
					eventListener.execute(event);
				} catch (final AbortException e) {
					return;
				} catch (final Exception e) {
					this.exceptionHandler.accept(e);
				}
			}

			System.out.println();
		}
	}
}