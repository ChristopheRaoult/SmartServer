package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.smartserver.database.DatabaseHandler;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;
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
     * Remove an alert from the database (including the row in AlertTemperature, if any).
     *
     * @param entity    Alert to be removed from the table.
     *
     * @return          True if successful, false otherwise (SQLException).
     */
    @Override
    public boolean delete(AlertEntity entity)
    {
        try
        {
            // first, remove the attached AlertTemperature, if any.
            if(AlertTypeRepository.toAlertType(entity.getAlertType()) == AlertType.TEMPERATURE)
            {
                Repository<AlertTemperatureEntity> atRepo =
                        DatabaseHandler.getRepository(AlertTemperatureEntity.class);
                AlertTemperatureEntity ate =
                        atRepo.getEntityBy(AlertTemperatureEntity.ALERT_ID, entity.getId());

                atRepo.delete(ate);
            }

            // then, remove the alert
            _dao.delete(entity);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while deleting Alert.", sqle);
            return false;
        }

        return true;
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
