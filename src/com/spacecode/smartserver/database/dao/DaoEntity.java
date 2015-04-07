package com.spacecode.smartserver.database.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.spacecode.smartserver.helper.SmartLogger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Intermediate implementation of BaseDaoImpl for DAO's classes: add some useful/generic methods.
 */
public class DaoEntity<T, ID> extends BaseDaoImpl<T, ID>
{
    protected DaoEntity(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException
    {
        super(connectionSource, dataClass);
    }

    /**
     * Look for an instance of E (entity type) via its identifier.
     *
     * @param value Identifier key value.
     *
     * @return      E instance or null if something went wrong (no result, sql exception).
     */
    public final T getEntityById(ID value)
    {
        try
        {
            return queryForId(value);
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
    public final T getEntityBy(String field, Object value)
    {
        try
        {
            return queryForFirst(
                    queryBuilder().where()
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
    public final List<T> getEntitiesBy(String field, Object value)
    {
        try
        {
            return query(
                    queryBuilder().where()
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
    public final T getEntityWhereNotEqual(String field, Object value)
    {
        try
        {
            return queryForFirst(
                    queryBuilder().where()
                            .ne(field, value)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entity where field not equal.", sqle);
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
    public final List<T> getEntitiesWhereNotEqual(String field, Object value)
    {
        try
        {
            return query(
                    queryBuilder().where()
                            .ne(field, value)
                            .prepare());
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting entities with criteria.", sqle);
            return new ArrayList<>();
        }
    }

    /**
     * Default method to insert a new row in the dao table.
     *
     * @param newEntity New entity to be inserted in the table (as a new row).
     *
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean insert(T newEntity)
    {
        try
        {
            return create(newEntity) == 1;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while inserting new entity.", sqle);
            return false;
        }
    }

    /**
     * Default method to insert a collection of new E.
     *
     * @param newEntities   Collection of E to be inserted.
     *
     * @return True if successful, false otherwise (SQLException).
     */
    public boolean insert(Collection<T> newEntities)
    {
        for(T entity : newEntities)
        {
            if(!insert(entity))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Default method to update an entity in the dao table.
     * Update is made according to entity's Id.
     *
     * @param entity    Entity to be updated in the table.
     *
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean updateEntity(T entity)
    {
        try
        {
            return update(entity) > 0;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while updating entity.", sqle);
            return false;
        }
    }

    /**
     * Default method to delete an entity in the dao table.
     * Deletion is made according to entity's Id.
     *
     * @param entity    Entity to be removed from the table.
     *
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean deleteEntity(T entity)
    {
        try
        {
            return delete(entity) > 0;
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Error occurred while deleting an entity.", sqle);
            return false;
        }
    }

    /**
     * Default method to delete a collection of entities in the dao table.
     * Deletion is made according to entity's Id.
     *
     * @param entities  Collection to be removed from the table.
     *
     * @return          True if successful, false otherwise (SQLException).
     */
    public boolean deleteEntity(Collection<T> entities)
    {
        if(entities == null)
        {
            return false;
        }
        
        if(entities.isEmpty())
        {
            return true;
        }
        
        try
        {
            return delete(entities) == entities.size();
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while deleting entities.", sqle);
            return false;
        }
    }

    /** @return List of all entities available in the table (empty if any SQLException occurred). */
    public final List<T> getAll()
    {
        try
        {
            return queryForAll();
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
    public final  List<T> getAllWhereFieldIn(String field, Iterable<?> values)
    {
        try
        {
            return query(queryBuilder()
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
    public final List<T> getAllWhereFieldNotIn(String field, Iterable<?> values)
    {
        try
        {
            return query(queryBuilder()
                    .where()
                    .notIn(field, values)
                    .prepare()
            );
        } catch (SQLException sqle)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "Exception occurred while getting 'NOT IN'.", sqle);
            return new ArrayList<>();
        }
    }
}
