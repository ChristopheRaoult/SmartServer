package com.spacecode.smartserver.command;

/**
 * Created by Vincent on 30/12/13.
 */

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
