package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.sdk.network.alert.SmtpServer;
import com.spacecode.smartserver.database.DbManager;
import com.spacecode.smartserver.database.entity.SmtpServerEntity;

/**
 * SmtpServer Repository
 */
public class SmtpServerRepository extends Repository<SmtpServerEntity>
{
    protected SmtpServerRepository(Dao<SmtpServerEntity, Integer> dao)
    {
        super(dao);
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
        Repository<SmtpServerEntity> ssRepo = DbManager.getRepository(SmtpServerEntity.class);

        SmtpServerEntity currentSse = getSmtpServerConfig();

        if(currentSse == null)
        {
            return ssRepo.insert(new SmtpServerEntity(smtpServer.getAddress(), smtpServer.getPort(),
                    smtpServer.getUsername(), smtpServer.getPassword(), smtpServer.isSslEnabled()));
        }

        else
        {
            currentSse.updateFrom(smtpServer);
            return ssRepo.update(currentSse);
        }
    }

    /** @return SmtpServerEntity instance created (if any) for the current Device. */
    public SmtpServerEntity getSmtpServerConfig()
    {
        return getEntityBy(SmtpServerEntity.DEVICE_ID, DbManager.getDevEntity().getId());
    }
}
