package com.spacecode.smartserver.command;

import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.channel.ChannelHandlerContext;

/**
 * Command SignInAdmin
 */
public class CmdSignInAdmin extends ClientCommand
{
    private static final String ADMIN_USERNAME = "testrfid";
    private static final String ADMIN_PASSWORD = "testrfid123456";
    
    /**
     * Add the current ChannelHandlerContext to the list of authenticated (administrator) contexts.
     *
     * @param ctx                       Channel between SmartServer and the client.
     * @param parameters                String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException   Invalid number of parameters received.
     */
    @Override
    public void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for 2 parameters: username, password
        if (parameters.length != 2)
        {
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SIGN_IN_ADMIN, FALSE);
            throw new ClientCommandException("Invalid number of parameters [SignInAdmin].");
        }
        
        String username = parameters[0];
        String password = parameters[1];
        
        if(!ADMIN_USERNAME.equals(username) || !ADMIN_PASSWORD.equals(password))
        {
            SmartLogger.getLogger().info(String.format("Authentication Failure! (%s/%s)", username, password));
            SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SIGN_IN_ADMIN, FALSE);
            return;
        }
        
        SmartServer.addAdministrator(ctx.channel().remoteAddress());
        SmartServer.sendMessage(ctx, ClientCommandRegister.AppCode.SIGN_IN_ADMIN, TRUE);
    }
}
