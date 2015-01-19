package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.AlertEntity;
import com.spacecode.smartserver.database.entity.AlertHistoryEntity;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * AlertHistory Repository
 */
public class AlertHistoryRepository extends Repository<AlertHistoryEntity>
{
    protected AlertHistoryRepository(Dao<AlertHistoryEntity, Integer> dao)
    {
        super(dao);
    }

    /**
     * Sort the table by ORDER 'CREATED_AT' DESC and take the first result.
     * Not as efficient as a MAX operator (if the table is big) but OrmLite doesn't propose "MAX".
     *
     * @return Latest AlertHistory for the given device (or null if none).
     */
    public AlertHistoryEntity getLastAlertHistory()
    {
        try
        {
            QueryBuilder<AlertEntity, Integer> alertQb = DbManager.getDao(AlertEntity.class).queryBuilder();
            alertQb.where().eq(AlertEntity.DEVICE_ID, DbManager.getDevEntity().getId());

            return _dao.queryForFirst(_dao.queryBuilder()
                            .join(alertQb)
                            // order DESC
                            .orderBy(AlertHistoryEntity.CREATED_AT, false)
                            .limit(1L)
                            .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting last AlertHistory.", sqle);
            return null;
        }
    }

    /**
     * Get a list of AlertHistoryEntity created during a certain period.
     *
     * @param startDate Period start date.
     * @param endDate   Period end date.
     *
     * @return List of AlertHistoryEntity created during the given period (empty list if none).
     */
    public List<AlertHistoryEntity> getAlertsHistory(Date startDate, Date endDate)
    {
        try
        {
            QueryBuilder<AlertEntity, Integer> alertQb = DbManager.getDao(AlertEntity.class).queryBuilder();
            alertQb.where().eq(AlertEntity.DEVICE_ID, DbManager.getDevEntity().getId());

            return _dao.query(_dao.queryBuilder()
                            .orderBy(AlertHistoryEntity.CREATED_AT, true)
                            .join(alertQb)
                            .where()
                            .between(AlertHistoryEntity.CREATED_AT, startDate, endDate)
                            .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting Alerts history.", sqle);
            return new ArrayList<>();
        }
    }
}
