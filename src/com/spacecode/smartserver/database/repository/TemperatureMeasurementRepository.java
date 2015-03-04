package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.TemperatureMeasurementEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * TemperatureMeasurement Repository
 */
public class TemperatureMeasurementRepository extends Repository<TemperatureMeasurementEntity>
{
    protected TemperatureMeasurementRepository(Dao<TemperatureMeasurementEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Get the list of TemperatureMeasurement created during a certain period.
     *
     * @param from  Period start date.
     * @param to    Period end date.
     *
     * @return List of TemperatureMeasurement recorded during the given period (empty if no result or error).
     */
    public List<TemperatureMeasurementEntity> getTemperatureMeasures(Date from, Date to)
    {
        try
        {
            QueryBuilder<TemperatureMeasurementEntity, Integer> qb = _dao.queryBuilder();

            qb.orderBy(TemperatureMeasurementEntity.CREATED_AT, true);

            return _dao.query(
                    qb.where()
                    .eq(TemperatureMeasurementEntity.DEVICE_ID, DbManager.getDevEntity().getId())
                    .and()
                    .between(TemperatureMeasurementEntity.CREATED_AT, from, to)
                    .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting temperature measures.", sqle);
            return new ArrayList<>();
        }
    }
}
