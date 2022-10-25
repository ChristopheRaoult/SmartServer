package com.spacecode.smartserver.database.entity;

import com.j256.ormlite.field.DatabaseField;

/**
 * Base class for all Entities
 */
public abstract class Entity
{
    public static final String ID = "id";

    @DatabaseField(columnName = ID, generatedId = true)
    protected int _id;

    /** @return Id of the entity. */
    public int getId()
    {
        return _id;
    }
}
