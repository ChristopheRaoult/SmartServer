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
     * @param address       Server address.
     * @param port          Server TCP port number.
     * @param username      Username to connect to the SMTP server.
     * @param password      Password to connect to the SMTP server.
     * @param sslEnabled    If true, will use SSL for authentication.
     *
     * @return              True if operation succeeded, false otherwise.
     */
    public boolean persist(String address, int port, String username,
                                            String password, boolean sslEnabled)
    {
        Repository<SmtpServerEntity> ssRepo = DbManager.getRepository(SmtpServerEntity.class);

        SmtpServerEntity currentSse = getSmtpServerConfig();

        if(currentSse == null)
        {
            return ssRepo.insert(new SmtpServerEntity(address, port, username, password, sslEnabled));
        }

        else
        {
            currentSse.updateFrom(new SmtpServer(address, port, username, password, sslEnabled));
            return ssRepo.update(currentSse);
        }
    }

    /** @return SmtpServerEntity instance created (if any) for the current Device. */
    public SmtpServerEntity getSmtpServerConfig()
    {
        return getEntityBy(SmtpServerEntity.DEVICE_ID, DbManager.getDevEntity().getId());
    }
}
