package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * GrantedUser Entity
 */
@DatabaseTable(tableName = "sc_granted_user")
public class GrantedUser
{
    public static final String ID = "id";
    public static final String USERNAME = "username";
    public static final String BADGE_NUMBER = "badge_number";
    public static final String CREATED_AT = "created_at";

    @DatabaseField(generatedId = true, columnName = ID)
    private int _id;

    @DatabaseField(columnName = USERNAME, canBeNull = false)
    private String _username;

    @DatabaseField(columnName = BADGE_NUMBER)
    private String _badgeNumber;

    @DatabaseField(columnName = CREATED_AT)
    private Date _createdAt;

    /**
     * No-Arg constructor (with package visibility) for ORMLite
     */
    GrantedUser()
    {
    }

    /**
     * Default constructor
     * @param username
     * @param badgeNumber
     */
    public GrantedUser(String username, String badgeNumber)
    {
        _username = username;
        _badgeNumber = badgeNumber;
    }
}
