package com.spacecode.smartserver.command;

import com.spacecode.sdk.network.communication.RequestCode;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.FingerIndex;
import com.spacecode.smartserver.SmartServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.FingerprintEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.database.repository.UserRepository;
import com.spacecode.smartserver.helper.DeviceHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * AddUser command.
 */
public class CmdAddUser extends ClientCommand
{
    /**
     * Request to add a new User to granted users list. Send (string) "true" if succeed, "false" otherwise.
     *
     * @param ctx           Channel between SmartServer and the client.
     * @param parameters    String array containing parameters (if any) provided by the client.
     *
     * @throws ClientCommandException
     */
    @Override
    public synchronized void execute(ChannelHandlerContext ctx, String[] parameters) throws ClientCommandException
    {
        // waiting for only 1 parameter: serialized User
        if(parameters.length != 1)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            throw new ClientCommandException("Invalid number of parameters [AddUser].");
        }

        if(DeviceHandler.getDevice() == null)
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        User newUser = User.deserialize(parameters[0]);

        if(newUser == null || newUser.getUsername() == null || newUser.getUsername().trim().isEmpty())
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        // check if the user already exist in DB. In that case, we take HIS badge number and fingerprints
        UserRepository userRepository = (UserRepository) DbManager.getRepository(UserEntity.class);
        UserEntity currentUser = userRepository.getByUsername(newUser.getUsername());

        if(currentUser != null)
        {
            Collection<FingerprintEntity> fingers = currentUser.getFingerprints();
            Map<FingerIndex, String> fingersMap = new EnumMap<>(FingerIndex.class);

            for(FingerprintEntity fe : fingers)
            {
                FingerIndex fingerIndex = FingerIndex.getValueByIndex(fe.getFingerIndex());

                if(fingerIndex != null)
                {
                    fingersMap.put(fingerIndex, fe.getTemplate());
                }
            }

            newUser = new User(newUser.getUsername(), newUser.getPermission(), currentUser.getBadgeNumber(), fingersMap);
        }

        if(!DeviceHandler.getDevice().getUsersService().addUser(newUser))
        {
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        if(!userRepository.persist(newUser))
        {
            // if insert in db failed, remove user from local users.
            DeviceHandler.getDevice().getUsersService().removeUser(newUser.getUsername());
            SmartServer.sendMessage(ctx, RequestCode.ADD_USER, FALSE);
            return;
        }

        SmartServer.sendMessage(ctx, RequestCode.ADD_USER, TRUE);
    }
}
