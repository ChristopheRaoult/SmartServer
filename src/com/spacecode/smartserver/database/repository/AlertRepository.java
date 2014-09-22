package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

/**
 * Alert Repository
 */
public class AlertRepository extends Repository<AlertEntity>
{
    protected AlertRepository(Dao<AlertEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Return enabled alerts of the given type.
     *
     * @param ate   AlertType entity instance (only used to get Id of the type).
     *
     * @return      List of AlertEntity matching the two conditions (type and enabled).
     */
    public List<AlertEntity> getEnabledAlerts(AlertTypeEntity ate)
    {
        try
        {
            return _dao.query(
                    _dao.queryBuilder()
                            .where()
                            .eq(AlertEntity.ALERT_TYPE_ID, ate.getId())
                            .and()
                            .eq(AlertEntity.ENABLED, true)
                            .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to get enabled alerts id.", sqle);
            return null;
        }
    }
}
