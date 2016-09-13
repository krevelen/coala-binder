package io.coala.bind;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.coala.json.JsonUtil;

/**
 * {@link BindableDao} links {@link Entity} and {@link Embeddable} data access
 * object to their referent types and provides some JSON de/serialization
 * features
 * <p>
 * TODO: use inject-persist, DAO auto-mapping, {@link TypedQuery} utility, etc.
 * 
 * @version $Id: ccb850afe9da1c0e05dabbd3374aa241dfa9e0e2 $
 * @author Rick van Krevelen
 */
@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
public abstract class BindableDao<T, THIS extends BindableDao<T, ?>>
{

	@SuppressWarnings( "unchecked" )
	public static <T extends BindableDao<S, ?>, S> T persist(
		final LocalBinder binder, final EntityManager em, final S source,
		final Class<T> entityType )
	{
		final T result = (T) binder.inject( entityType ).prePersist( source );
		em.persist( result );
		return result;
	}

	abstract protected THIS prePersist( T source );

	public abstract T restore( LocalBinder binder );

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}
}
