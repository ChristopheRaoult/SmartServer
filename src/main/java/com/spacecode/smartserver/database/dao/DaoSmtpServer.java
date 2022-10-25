package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.sdk.network.alert.SmtpServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;

import java.sql.SQLException;

/**
 * SmtpServer Repository
 */
public class DaoSmtpServer extends DaoEntity<SmtpServerEntity, Integer>
{
    public DaoSmtpServer(ConnectionSource connectionSource) throws SQLException
    {
        super(connectionSource, SmtpServerEntity.class);
    }

    /**
     * Persist new SMTP server information for the current device.
     *
     * @param smtpServer    SmtpServer instance containing new information.
     *
     * @return              True if operation succeeded, false otherwise.
     */
    public boolean persist(SmtpServer smtpServer)
    {
        SmtpServerEntity currentSse = getSmtpServerConfig();

        if(currentSse == null)
        {            
            return insert(new SmtpServerEntity(smtpServer.getAddress(), smtpServer.getPort(),
                    smtpServer.getUsername(), smtpServer.getPassword(), smtpServer.isSslEnabled()));
        }

        else
        {
            currentSse.updateFrom(smtpServer);
            return updateEntity(currentSse);
        }
    }

    /** @return SmtpServerEntity instance created (if any) for the current Device. */
    public SmtpServerEntity getSmtpServerConfig()
    {
        return getEntityBy(SmtpServerEntity.DEVICE_ID, DbManager.getDevEntity().getId());
    }
}
