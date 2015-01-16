package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.spacecode.sdk.user.User;
import com.spacecode.sdk.user.data.AccessType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AccessTypeEntity;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.DeviceEntity;
import com.spacecode.smartserver.database.entity.UserEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * Authentication Repository
 */
public class AuthenticationRepository extends Repository<AuthenticationEntity>
{
    protected AuthenticationRepository(Dao<AuthenticationEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Get the list of Authentications created during a certain period.
     *
     * @param from  Period start date.
     * @param to    Period end date.
     * @param de    Device to look inventories for.
     *
     * @return List of Authentications recorded during the given period (empty if no result or error).
     */
    public List<AuthenticationEntity> getAuthentications(Date from, Date to, DeviceEntity de)
    {
        try
        {
            QueryBuilder<AuthenticationEntity, Integer> qb = _dao.queryBuilder();

            qb.orderBy(AuthenticationEntity.CREATED_AT, true);

            return _dao.query(qb
                            .orderBy(AuthenticationEntity.CREATED_AT, true)
                            .where()
                            .eq(AuthenticationEntity.DEVICE_ID, de.getId())
                            .and()
                            .between(AuthenticationEntity.CREATED_AT, from, to)
                            .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting authentications.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * On successful authentication (event raised by Device), persist information in database.
     *
     * @param grantedUser   GrantedUser instance who successfully authenticated.
     * @param accessType    AccessType enum value (fingerprint, badge...).
     *
     * @return True if operation was successful, false otherwise.
     */
    public boolean persist(User grantedUser, AccessType accessType)
    {
        Repository<UserEntity> userRepo = DbManager.getRepository(UserEntity.class);
        Repository accessTypeRepo = DbManager.getRepository(AccessTypeEntity.class);

        UserEntity gue = userRepo.getEntityBy(UserEntity.USERNAME, grantedUser.getUsername());

        if(gue == null)
        {
            // user does not exist in database
            return false;
        }

        AccessTypeEntity ate = ((AccessTypeRepository)accessTypeRepo).fromAccessType(accessType);

        if(ate == null)
        {
            SmartLogger.getLogger().severe("Persisting authentication: unknown access type " + accessType + ".");
            return false;
        }

        return insert(new AuthenticationEntity(DbManager.getDeviceConfiguration(), gue, ate));
    }
}
