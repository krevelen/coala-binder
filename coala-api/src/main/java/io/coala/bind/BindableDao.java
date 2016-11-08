package io.coala.bind;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.TypedQuery;

import io.coala.persist.Persistable;

/**
 * {@link BindableDao} links {@link Entity} and {@link Embeddable} data access
 * objects to their referent types
 * <p>
 * TODO: use inject-persist, DAO auto-mapping, {@link TypedQuery} utility, etc.
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public interface BindableDao<T, THIS extends BindableDao<T, ?>>
	extends Persistable.Dao
{
	T restore( LocalBinder binder );
}
