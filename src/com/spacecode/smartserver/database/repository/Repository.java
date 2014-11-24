package com.spacecode.smartserver.database.repository;

import com.j256.ormlite.dao.Dao;
import com.spacecode.smartserver.database.entity.Entity;
import com.spacecode.smartserver.helper.SmartLogger;

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

    /** @return Dao instance currently used by this repository. */
    public final Dao<E, Integer> getDao()
    {
        return _dao;
    }

    /**
     * Look for an instance of E (entity type) via its identifier.
     *
     * @param value Identifier key value.
     *
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
     * Look for the first instance of E (entity type) having "field" equal to "value".
     *
     * @param field Name of the field.
     * @param value Expected value.
     *
     * @return      Instance of E, or null if not result (or sql exception).
     */
    public final E getEntityBy(String field, Object value)
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
     * Look for a list of instances of E (entity type) via a field name and value.
     *
     * @param field Name of the field.
     * @param value Expected value.
     *
     * @return  List of E containing all matching results (could be empty if no results, or SQL Exception).
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
     * Look for the first instance of E (entity type) having "field" NOT equal to "value".
     *
     * @param field Name of the field.
     * @param value Value not desired.
     *
     * @return      Instance of E, or null if not result (or sql exception).
     */
    public final E getEntityWhereNotEqual(String field, Object value)
    {
        try
        {
            return _dao.queryForFirst(
                    _dao.queryBuilder().where()
                            .ne(field, value)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entity with criteria.", sqle);
            return null;
        }
    }

    /**
     * Look for a list of instances of E (entity type) having "field" NOT equal to "value".
     *
     * @param field Name of the field.
     * @param value Value not desired.
     *
     * @return      List of E provided by the query (could be empty if no results, or SQL Exception).
     */
    public final List<E> getEntitiesWhereNotEqual(String field, Object value)
    {
        try
        {
            return _dao.query(
                    _dao.queryBuilder().where()
                            .ne(field, value)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entities with criteria.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Default method to insert a new row in the repository table.
     *
     * @param newEntity New entity to be inserted in the table (as a new row).
     *
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
     *
     * @param entity    Entity to be updated in the table.
     *
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
     *
     * @param entity    Entity to be removed from the table.
     *
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
     *
     * @param entities  Collection to be removed from the table.
     *
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

    /** @return List of all entities available in the table (empty if any SQLException occurred). */
    public final List<E> getAll()
    {
        try
        {
            return _dao.queryForAll();
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting all entities.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Perform a "IN" query to get all entities which match a given values (provided list).
     *
     * @param field     Column name.
     * @param values    Desired values.
     *
     * @return List of all entities matching the condition (empty if any SQLException occurred).
     */
    public final  List<E> getAllWhereFieldIn(String field, Iterable<?> values)
    {
        try
        {
            return _dao.query(_dao.queryBuilder()
                    .where()
                    .in(field, values)
                    .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting 'IN'.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Perform a "NOT IN" query to get all entities which don't match given values (provided list).
     *
     * @param field     Column name.
     * @param values    Desired values.
     *
     * @return List of all entities returned by NOT IN query (empty if any SQLException occurred).
     */
    public final List<E> getAllWhereFieldNotIn(String field, Iterable<?> values)
    {
        try
        {
            return _dao.query(_dao.queryBuilder()
                    .where()
                    .notIn(field, values)
                    .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting 'IN'.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Sort the table by ORDER 'field' DESC and take the first result.
     * Not as efficient as a MAX operator (if the table is big) but OrmLite doesn't propose "MAX".
     *
     * @param field Field to be used for sorting.
     *
     * @return Entity with the biggest value for the given field.
     */
    public E getEntityByMax(String field)
    {
        try
        {
            return _dao.queryForFirst(_dao.queryBuilder()
                    // order DESC
                    .orderBy(field, false)
                    .limit(1L)
                    .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting 'IN'.", sqle);
            return null;
        }
    }
}
