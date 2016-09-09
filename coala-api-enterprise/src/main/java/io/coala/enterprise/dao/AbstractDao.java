package io.coala.enterprise.dao;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.coala.bind.LocalBinder;
import io.coala.json.JsonUtil;

/**
 * {@link AbstractDao} links {@link Entity} and {@link Embeddable} data access
 * object to their referent types and provides some JSON de/serialization
 * features
 * <p>
 * TODO: use inject-persist, DAO auto-mapping, {@link TypedQuery} utility, etc.
 * 
 * @version $Id: ccb850afe9da1c0e05dabbd3374aa241dfa9e0e2 $
 * @author Rick van Krevelen
 */
@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
public abstract class AbstractDao<T, THIS extends AbstractDao<T, ?>>
{

	@SuppressWarnings( "unchecked" )
	public static <T extends AbstractDao<S, ?>, S> T persist(
		final LocalBinder binder, final EntityManager em, final S source,
		final Class<T> entityType )
	{
		final T result = (T) binder.inject( entityType ).prePersist( source );
		em.persist( result );
		return result;
	}

	abstract THIS prePersist( T source );

	public T restore()
	{
		return doRestore();
	}

	abstract T doRestore();

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}
}
