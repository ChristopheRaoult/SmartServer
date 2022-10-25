package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * AlertType Repository
 */
public class DaoAlertType extends DaoEntity<AlertTypeEntity, Integer>
{
    private static final Map<String, AlertTypeEntity> TYPE_TO_ENTITY = new HashMap<>();

    public DaoAlertType(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, AlertTypeEntity.class);
    }

    /**
     * @param alertType     SDK AlertType value.
     * @return              AlertTypeEntity equivalent (from Db).
     */
    public AlertTypeEntity fromAlertType(AlertType alertType)
    {
        if(alertType == null)
        {
            return null;
        }

        if(!TYPE_TO_ENTITY.containsKey(alertType.name()))
        {
            try
            {
                AlertTypeEntity ate = queryForFirst(queryBuilder().where()
                        .eq(AlertTypeEntity.TYPE, alertType.name())
                        .prepare());

                TYPE_TO_ENTITY.put(alertType.name(), ate);
            } catch (SQLException sqle)
            {
                SmartLogger.getLogger().log(Level.SEVERE, "Unable to get AlertType "+alertType.name()+" from database.", sqle);
                return null;
            }
        }

        return TYPE_TO_ENTITY.get(alertType.name());
    }

    /**
     * Provide an AlertType [SDK] equivalent to the provided AlertTypeEntity.
     *
     * @param ate AlertTypeEntity to be "converted".
     *
     * @return  AlertType [SDK] enum value.
     */
    public static AlertType asAlertType(AlertTypeEntity ate)
    {
        if(ate == null)
        {
            return null;
        }

        try
        {
            return AlertType.valueOf(ate.getType());
        } catch(IllegalArgumentException iae)
        {
            SmartLogger.getLogger().log(Level.WARNING, "Unknown alert type", iae);
            return null;
        }
    }
}
