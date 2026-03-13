package com.rreganjr.command;

/**
 * Implemented by commands that can carry API dispatch metadata (command type,
 * input DTO). The API factory stamps this after creation; cross-cutting
 * handlers (e.g. auditing) read it after execution.
 */
public interface CommandMetadataAware {

    CommandMetadata getCommandMetadata();

    void setCommandMetadata(CommandMetadata metadata);
}
