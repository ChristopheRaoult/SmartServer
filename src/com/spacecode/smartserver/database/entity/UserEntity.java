package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.user.User;
import com.spacecode.smartserver.database.dao.DaoUser;

import java.util.Date;

/**
 * User Entity
 */
@DatabaseTable(tableName = UserEntity.TABLE_NAME, daoClass = DaoUser.class)
public final class UserEntity extends Entity
{
    public static final String TABLE_NAME = "sc_user";

    public static final String USERNAME = "username";
    public static final String BADGE_NUMBER = "badge_number";
    public static final String THIEF_FINGER_INDEX = "thief_finger_index";
    public static final String UPDATED_AT = "updated_at";

    @DatabaseField(columnName = USERNAME, canBeNull = false, unique = true, width = 64)
    private String _username;

    @DatabaseField(columnName = BADGE_NUMBER)
    private String _badgeNumber;

    @DatabaseField(columnName = THIEF_FINGER_INDEX)
    private Integer _thiefFingerIndex;

    @DatabaseField(columnName = UPDATED_AT, version = true)
    private Date _updatedAt;

    @ForeignCollectionField(eager = true)
    private ForeignCollection<FingerprintEntity> _fingerprints;

    @ForeignCollectionField(eager = false)
    private ForeignCollection<GrantedAccessEntity> _grantedAccesses;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    UserEntity()
    {
    }

    /**
     * Create a User entity from username and badge number.
     *
     * @param username      Username.
     * @param badgeNumber   Badge number.
     */
    public UserEntity(String username, String badgeNumber)
    {
        _username = username;
        _badgeNumber = badgeNumber;
    }

    /**
     * Create a UserEntity from a User.
     * Only fill Username and Badge Number fields.
     * Fingerprints & Accesses must be created separately.
     *
     * @param newUser   User instance to get information from.
     */
    public UserEntity(User newUser)
    {
        this(newUser.getUsername(), newUser.getBadgeNumber());
    }

    /** @return Username. */
    public String getUsername()
    {
        return _username;
    }

    /** @return Badge Number */
    public String getBadgeNumber()
    {
        return _badgeNumber;
    }

    /**
     * Allow updating badge number.
     *
     * @param badgeNumber New badge number.
     */
    public void setBadgeNumber(String badgeNumber)
    {
        _badgeNumber = badgeNumber;
    }

    /** @return Last Update Date */
    public Date getUpdatedAt()
    {
        return new Date(_updatedAt.getTime());
    }

    /** @return "Thief finger" index (finger used in case of robbery/mugging). */
    public Integer getThiefFingerIndex()
    {
        return _thiefFingerIndex;
    }

    /**
     * Allow setting user's "thief finger" index (finger used in case of robbery/mugging).
     *
     * @param thiefFingerIndex New value.
     */
    public void setThiefFingerIndex(Integer thiefFingerIndex)
    {
        _thiefFingerIndex = thiefFingerIndex;
    }

    /** @return FingerprintEntity collection (ForeignCollection). */
    public ForeignCollection<FingerprintEntity> getFingerprints()
    {
        return _fingerprints;
    }

    /** @return GrantedAccessEntity collection (ForeignCollection). */
    public ForeignCollection<GrantedAccessEntity> getGrantedAccesses()
    {
        return _grantedAccesses;
    }
}
