package io.coala.bind;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.TypedQuery;

import io.coala.persist.Persistable;

/**
 * {@link BindableDao} links {@link Entity} and {@link Embeddable} data access
 * objects to their referent object types in the O/R or O/G models
 * <p>
 * TODO: use inject-persist, DAO auto-mapping, {@link TypedQuery} utility, etc.
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface BindableDao<T, THIS extends BindableDao<T, ?>>
	extends Persistable.Dao
{
	/**
	 * @param binder an (optional) {@link LocalBinder} for attribute injection
	 * @return the restored instance of this data access object's referent O/R
	 *         or O/G model type
	 */
	T restore( LocalBinder binder );
}
