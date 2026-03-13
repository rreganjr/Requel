package com.rreganjr.command;

/**
 * Carries supplemental data about a command that is set by the API dispatch
 * layer (factory/controller) and read by cross-cutting handlers (auditing,
 * logging). Stored on the command via {@link CommandMetadataAware}.
 * <p>
 * Fields:
 * <ul>
 *   <li>{@code commandType} — the URL dispatch string (e.g. "EditProject")</li>
 *   <li>{@code input} — the typed input DTO that was applied to the command</li>
 * </ul>
 */
public class CommandMetadata {

    private String commandType;
    private Object input;

    public CommandMetadata() {}

    public CommandMetadata(String commandType, Object input) {
        this.commandType = commandType;
        this.input = input;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String commandType) {
        this.commandType = commandType;
    }

    public Object getInput() {
        return input;
    }

    public void setInput(Object input) {
        this.input = input;
    }
}
