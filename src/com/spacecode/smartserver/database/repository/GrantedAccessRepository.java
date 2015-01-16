package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.user.data.GrantType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.GrantTypeEntity;
import com.spacecode.smartserver.database.entity.GrantedAccessEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.util.Iterator;

/**
 * GrantedAccess Repository
 */
public class GrantedAccessRepository extends Repository<GrantedAccessEntity>
{
    protected GrantedAccessRepository(Dao<GrantedAccessEntity, Integer> dao)
    {
        super(dao);
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
        Repository<UserEntity> userRepo = DbManager.getRepository(UserEntity.class);
        Repository grantTypeRepo = DbManager.getRepository(GrantTypeEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, username);

        if(gue == null)
        {
            return false;
        }

        GrantTypeEntity gte = ((GrantTypeRepository) grantTypeRepo).fromGrantType(grantType);

        if(gte == null)
        {
            SmartLogger.getLogger().severe("Persisting permission: unknown grant type " + grantType + ".");
            return false;
        }

        GrantedAccessEntity gae = new GrantedAccessEntity(gue,
                DbManager.getDeviceConfiguration(),
                gte
        );

        Iterator<GrantedAccessEntity> it = gue.getGrantedAccesses().iterator();

        // remove any previous permission on this device
        while(it.hasNext())
        {
            if(it.next().getDevice().getId() == DbManager.getDeviceConfiguration().getId())
            {
                it.remove();
                break;
            }
        }

        // add the new permission
        return gue.getGrantedAccesses().add(gae);
    }
}
