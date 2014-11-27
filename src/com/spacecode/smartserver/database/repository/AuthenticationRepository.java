package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.spacecode.smartserver.database.entity.AuthenticationEntity;
import com.spacecode.smartserver.database.entity.DeviceEntity;
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
}
