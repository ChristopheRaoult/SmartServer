package com.spacecode.smartserver.command;

/**
 * Raised when any exception occurs using ClientCommand(s).
 */
public class ClientCommandException extends Exception
{
    /**
     * Call Exception constructor with given error message.
     * @param errorMessage Description of the encountered error.
     */
    public ClientCommandException(String errorMessage)
    {
        super(errorMessage);
    }
}
