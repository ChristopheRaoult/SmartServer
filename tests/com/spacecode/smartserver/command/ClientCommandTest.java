package com.spacecode.smartserver.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClientCommandTest
{
    @Test
    public void testTrueFalseConstant()
    {
        assertEquals(ClientCommand.FALSE, "false");
        assertEquals(ClientCommand.TRUE, "true");
    }
}