package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.SmartLogger;
import com.spacecode.smartserver.database.entity.Entity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Generic Repository for all entities. To be inherited/specified for more getting methods.
 * @param <E> Generic type for the Entity class.
 */
public abstract class Repository<E extends Entity>
{
    protected Dao<E, Integer> _dao;

    protected Repository(Dao<E, Integer> dao)
    {
        _dao = dao;
    }

    /**
     * @return Dao instance currently used by this repository.
     */
    public final Dao<E, Integer> getDao()
    {
        return _dao;
    }

    /**
     * Provide access to an instance of E (entity type) via its identifier.
     * @param value Identifier key value.
     * @return      E instance or null if something went wrong (no result, sql exception).
     */
    public final E getEntityById(int value)
    {
        try
        {
            return _dao.queryForId(value);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entity with Id.", sqle);
            return null;
        }
    }

    /**
     * Provide access to a single instance of E (entity type) via its field/value.
     * @param field Field filter.
     * @param value Expected value.
     * @return      E instance or null if something went wrong (no result, sql exception).
     */
    public final E getEntityBy(String field, String value)
    {
        try
        {
            return _dao.queryForFirst(
                    _dao.queryBuilder().where()
                            .eq(field, value)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entity with criteria.", sqle);
            return null;
        }
    }

    /**
     * Provide access to a list of instance of E (entity type) via a field name and value.
     * @param field Name of the field.
     * @param value Value expected.
     * @return  List of E containing all matching results (could be empty).
     */
    public final List<E> getEntitiesBy(String field, Object value)
    {
        try
        {
            return _dao.query(
                    _dao.queryBuilder().where()
                            .eq(field, value)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entities with criteria.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Default method to insert a new row in the repository table.
     * @param newEntity New entity to be inserted in the table (as a new row).
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean insert(E newEntity)
    {
        try
        {
            _dao.create(newEntity);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while inserting new entity.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Default method to insert a collection of new E.
     *
     * @param newEntities   Collection of E to be inserted.
     *
     * @return True if successful, false otherwise (SQLException).
     */
    public boolean insert(Collection<E> newEntities)
    {
        for(E entity : newEntities)
        {
            if(!insert(entity))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Default method to update an entity in the repository table.
     * Update is made according to entity's Id.
     * @param entity    Entity to be updated in the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean update(E entity)
    {
        try
        {
            _dao.update(entity);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while updating entity.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Default method to delete an entity in the repository table.
     * Deletion is made according to entity's Id.
     * @param entity    Entity to be removed from the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean delete(E entity)
    {
        try
        {
            _dao.delete(entity);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while deleting an entity.", sqle);
            return false;
        }

        return true;
    }

    /**
     * Default method to delete a collection of entities in the repository table.
     * Deletion is made according to entity's Id.
     * @param entities  Collection to be removed from the table.
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean delete(Collection<E> entities)
    {
        try
        {
            _dao.delete(entities);
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while deleting entities.", sqle);
            return false;
        }

        return true;
    }
}
