package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic Repository for all entities. To be inherited/specified for more getting methods.
 * @param <TEntity> Generic type for the Entity class.
 */
public abstract class Repository<TEntity>
{
    protected Dao<TEntity, Integer> _dao;

    protected Repository(Dao<TEntity, Integer> dao)
    {
        _dao = dao;
    }

    /**
     * Provide access to an instance of TEntity (entity type) via its identifier.
     * @param value Identifier key value.
     * @return      TEntity instance or null if something went wrong (no result, sql exception).
     */
    public final TEntity getEntityById(int value)
    {
        try
        {
            return _dao.queryForId(value);
        } catch (SQLException e)
        {
            return null;
        }
    }

    /**
     * Provide access to a list of instance of TEntity (entity type) via a field name and value.
     * @param field Name of the field.
     * @param value Value expected.
     * @return  List of TEntity containing all matching results (could be empty).
     */
    public final List<TEntity> getEntitiesBy(String field, Object value)
    {
        try
        {
            return _dao.query(
                    _dao.queryBuilder().where()
                            .eq(field, value)
                            .prepare());
        } catch (SQLException e)
        {
            return new ArrayList<>();
        }
    }
}
