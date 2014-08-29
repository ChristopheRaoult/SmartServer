package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;
import com.spacecode.sdk.user.FingerIndex;
import com.spacecode.sdk.user.GrantedUser;

import java.util.Date;

/**
 * GrantedUser Entity
 */
@DatabaseTable(tableName = "sc_granted_user")
public class GrantedUserEntity
{
    public static final String ID = "id";
    public static final String USERNAME = "username";
    public static final String BADGE_NUMBER = "badge_number";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = USERNAME, canBeNull = false, unique = true)
    private String _username;

    @DatabaseField(columnName = BADGE_NUMBER)
    private String _badgeNumber;

    @DatabaseField(columnName = CREATED_AT)
    private Date _createdAt;

    @ForeignCollectionField(eager = false)
    private ForeignCollection<FingerprintEntity> _fingerprints;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    GrantedUserEntity()
    {
    }

    /**
     * Create a GrantedUser entity from username and badge number.
     * @param username      Username.
     * @param badgeNumber   Badge number.
     */
    public GrantedUserEntity(String username, String badgeNumber)
    {
        _username = username;
        _badgeNumber = badgeNumber;

        _createdAt = new Date();
    }

    /**
     * Create a GrantedUser entity from a GrantedUser (SDK) instance.
     * @param newUser   GrantedUser (SDK) instance to get information from.
     */
    public GrantedUserEntity(GrantedUser newUser)
    {
        this(newUser.getUsername(), newUser.getBadgeId());

        for(FingerIndex index : newUser.getEnrolledFingersIndexes())
        {
            _fingerprints.add(new FingerprintEntity(this, index.getIndex(), newUser.getFingerprintTemplate(index)));
        }
    }

    /**
     * @return GrantedUserEntity identifier.
     */
    public int getId()
    {
        return _id;
    }

    /**
     * @return Username.
     */
    public String getUsername()
    {
        return _username;
    }

    /**
     * @return Badge Number
     */
    public String getBadgeNumber()
    {
        return _badgeNumber;
    }

    /**
     * @return FingerprintEntity collection (ForeignCollection).
     */
    public ForeignCollection<FingerprintEntity> getFingerprints()
    {
        return _fingerprints;
    }
}
