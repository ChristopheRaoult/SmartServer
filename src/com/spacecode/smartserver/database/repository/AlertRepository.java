package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.network.alert.Alert;
import com.spacecode.sdk.network.alert.AlertTemperature;
import com.spacecode.sdk.network.alert.AlertType;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertTemperatureEntity;
import com.spacecode.smartserver.database.entity.AlertTypeEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Alert Repository
 */
public class AlertRepository extends Repository<AlertEntity>
{
    AlertRepository(Dao<AlertEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Remove an alert from the database (including the attached AlertTemperature, if any).
     *
     * @param entity    Alert to be removed from the table.
     *
     * @return          True if successful, false otherwise (SQLException).
     */
    @Override
    public boolean delete(AlertEntity entity)
    {
        if(entity == null)
        {
            return false;
        }

        try
        {
            // first, remove the attached AlertTemperature, if any.
            if(AlertTypeRepository.asAlertType(entity.getAlertType()) == AlertType.TEMPERATURE)
            {
                Repository<AlertTemperatureEntity> atRepo =
                        DbManager.getRepository(AlertTemperatureEntity.class);
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
     * @return      List of AlertEntity matching the conditions.
     *              Could be empty (no result, or SQL Exception).
     */
    public List<AlertEntity> getEnabledAlerts(AlertTypeEntity ate)
    {
        if(ate == null)
        {
            return new ArrayList<>();
        }

        try
        {
            return _dao.query(
                    _dao.queryBuilder()
                            .where()
                            .eq(AlertEntity.ALERT_TYPE_ID, ate.getId())
                            .and()
                            .eq(AlertEntity.DEVICE_ID, DbManager.getDevEntity().getId())
                            .and()
                            .eq(AlertEntity.ENABLED, true)
                            .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Unable to get enabled alerts id.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Insert or Update AlertEntity (from Alert instance [SDK]) in database.
     *
     * @param alert Alert instance coming from SDK client.
     *
     * @return      True if successful, false otherwise.
     */
    public boolean persist(Alert alert)
    {
        AlertTypeRepository aTypeRepo = (AlertTypeRepository) DbManager.getRepository(AlertTypeEntity.class);

        AlertTypeEntity ate = aTypeRepo.fromAlertType(alert.getType());

        if(ate == null)
        {
            return false;
        }

        // create an AlertEntity from the given Alert. All data is copied (including Id).
        AlertEntity newAlertEntity = new AlertEntity(ate, alert);

        if(alert.getId() == 0)
        {
            if(!insert(newAlertEntity))
            {
                // if we fail inserting the new alert
                return false;
            }
        }

        else
        {
            if(!update(newAlertEntity))
            {
                // if we fail updating the alert
                return false;
            }
        }

        if(alert.getType() != AlertType.TEMPERATURE)
        {
            // successfully inserted/updated the alert and we don't need to insert/update an AlertTemperature...
            return true;
        }

        // get the AlertTemperature [SDK] instance
        if(!(alert instanceof AlertTemperature))
        {
            SmartLogger.getLogger().severe("Trying to persist an Alert as an AlertTemperature whereas it is not.");
            return false;
        }

        AlertTemperature alertTemperature = (AlertTemperature) alert;

        AlertTemperatureRepository aTempRepo =
                (AlertTemperatureRepository) DbManager.getRepository(AlertTemperatureEntity.class);

        // if the Alert is already known: update the attached AlertTemperature
        if(alert.getId() != 0)
        {
            AlertTemperatureEntity atEntity = aTempRepo.getEntityBy(AlertTemperatureEntity.ALERT_ID, alert.getId());
            atEntity.setTemperatureMin(alertTemperature.getTemperatureMin());
            atEntity.setTemperatureMax(alertTemperature.getTemperatureMax());
            return aTempRepo.update(atEntity);
        }

        // else create a new AlertTemperature
        else
        {
            return aTempRepo.insert(new AlertTemperatureEntity(newAlertEntity, alertTemperature));
        }
    }

    /**
     * Start the alert deletion process (AlertEntity, plus AlertTemperatureEntity if required).
     *
     * @param alert Alert [SDK] instance to be deleted from Database.
     *
     * @return      True if successful, false otherwise (unknown alert, SQLException...).
     */
    public boolean delete(Alert alert)
    {
        if(alert == null)
        {
            return false;
        }

        AlertEntity gue = getEntityById(alert.getId());

        return gue != null && delete(gue);
    }
}
