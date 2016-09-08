package io.coala.enterprise.dao;

import javax.persistence.Embeddable;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.coala.bind.LocalBinder;
import io.coala.json.JsonUtil;
import io.coala.util.Instantiator;

/**
 * {@link AbstractDao} links {@link Entity} and {@link Embeddable} data access
 * object to their referent types and provides some JSON de/serialization
 * features
 * <p>
 * TODO: explore inject-persist
 * 
 * @version $Id: ccb850afe9da1c0e05dabbd3374aa241dfa9e0e2 $
 * @author Rick van Krevelen
 */
@JsonAutoDetect( fieldVisibility = Visibility.PROTECTED_AND_PUBLIC )
public abstract class AbstractDao<T, THIS extends AbstractDao<T, ?>>
{

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}

	abstract void prepare( EntityManager em, T t );

	public abstract T restore( LocalBinder binder );

	@SuppressWarnings( "unchecked" )
	public static <T extends AbstractDao<S, ?>, S> T store(
		final EntityManager em, final S source, final Class<T> entityType )
	{
		final T result = (T) Instantiator.instantiate( entityType );
		result.prepare( em, source );
		em.persist( result );
		return result;
	}
}
