package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * DeviceConfiguration Entity
 */
@DatabaseTable(tableName = "sc_device_configuration")
public class DeviceConfigurationEntity
{
    public static final String ID = "id";
    public static final String NB_BADGE_READER = "nb_badge_reader";
    public static final String FP_READER_MASTER = "fp_reader_master";
    public static final String FP_READER_SLAVE = "fp_reader_slave";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = NB_BADGE_READER)
    private int _nbOfBadgeReader;

    @DatabaseField(columnName = FP_READER_MASTER)
    private String _fpReaderMasterSerial;

    @DatabaseField(columnName = FP_READER_SLAVE)
    private String _fpReaderSlaveSerial;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    DeviceConfigurationEntity()
    {
    }

    /**
     * @return ID of the DeviceConfigurationEntity.
     */
    public int getId()
    {
        return _id;
    }

    /**
     * @return Number of Badge Readers to be connected.
     */
    public int getNbOfBadgeReader()
    {
        return _nbOfBadgeReader;
    }

    /**
     * @return Serial Number of the Master Fingerprint Reader.
     */
    public String getFpReaderMasterSerial()
    {
        return _fpReaderMasterSerial;
    }

    /**
     * @return Serial Number of the Slave Fingerprint Reader.
     */
    public String getFpReaderSlaveSerial()
    {
        return _fpReaderSlaveSerial;
    }
}
