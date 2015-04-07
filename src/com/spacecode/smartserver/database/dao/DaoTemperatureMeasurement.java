package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
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
public class DaoTemperatureMeasurement extends DaoEntity<TemperatureMeasurementEntity, Integer>
{
    public DaoTemperatureMeasurement(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, TemperatureMeasurementEntity.class);
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
            QueryBuilder<TemperatureMeasurementEntity, Integer> qb = queryBuilder();

            qb.orderBy(TemperatureMeasurementEntity.CREATED_AT, true);

            return query(
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
