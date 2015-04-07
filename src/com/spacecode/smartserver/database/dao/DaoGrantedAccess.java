package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.Iterator;

/**
 * GrantedAccess Repository
 */
public class DaoGrantedAccess extends DaoEntity<GrantedAccessEntity, Integer>
{
    public DaoGrantedAccess(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, GrantedAccessEntity.class);
    }

    /**
     * Persist new permission in database.
     *
     * @param username      User to be updated.
     * @param grantType     New permission.
     *
     * @return              True if success, false otherwise (user not known, SQLException, etc).
     */
    public boolean persist(String username, GrantType grantType)
    {
        DaoUser userRepo = (DaoUser) DbManager.getDao(UserEntity.class);
        DaoGrantType grantTypeRepo = (DaoGrantType) DbManager.getDao(GrantTypeEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        GrantTypeEntity gte = grantTypeRepo.fromGrantType(grantType);

        if(gte == null)
        {
            SmartLogger.getLogger().severe("Persisting permission: unknown grant type " + grantType + ".");
            return false;
        }

        GrantedAccessEntity gae = new GrantedAccessEntity(gue, gte);

        Iterator<GrantedAccessEntity> it = gue.getGrantedAccesses().iterator();

        // remove any previous permission on this device
        while(it.hasNext())
        {
            if(it.next().getDevice().getId() == DbManager.getDevEntity().getId())
            {
                it.remove();
                break;
            }
        }

        // add the new permission
        return gue.getGrantedAccesses().add(gae);
    }
}