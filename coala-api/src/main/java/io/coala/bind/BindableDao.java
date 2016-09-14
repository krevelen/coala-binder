package io.coala.bind;

import java.util.stream.Stream;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.coala.exception.Thrower;
import io.coala.json.JsonUtil;
import io.coala.util.Instantiator;
import io.coala.util.TypeArguments;

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
	public static <R extends BindableDao<T, ?>, T> R persist(
		final LocalBinder binder, final EntityManager em, final T source,
		final Class<R> entityType )
	{
		final R result = (R) binder.inject( entityType ).prePersist( source );
		em.persist( result );
		return result;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + JsonUtil.stringify( this );
	}

	public TypedQuery<THIS> query( final EntityManager em, final String query )
	{
		return em.createQuery( query, daoType() );
	}

	public Stream<T> find( final EntityManager em, final LocalBinder binder,
		final String query )
	{
		return query( em, query ).getResultList().stream().map( dao ->
		{
			return dao.restore( binder );
		} );
	}

	public Stream<T> findAll( final EntityManager em, final LocalBinder binder )
	{
		return find( em, binder, "SELECT dao FROM " + daoName() + " dao" );
	}

	public T persist( final EntityManager em, final T source )
	{
		@SuppressWarnings( "unchecked" )
		final T result = (T) Instantiator.instantiate( daoType() )
				.prePersist( source );
		em.persist( result );
		return result;
	}

	private Class<THIS> daoType = null;

	private String daoName = null;

	@SuppressWarnings( "unchecked" )
	protected Class<THIS> daoType()
	{
		if( this.daoType == null )
		{
			this.daoType = (Class<THIS>) TypeArguments
					.of( BindableDao.class, getClass() ).get( 1 );
		}
		return this.daoType;
	}

	protected String daoName()
	{
		if( this.daoName == null )
		{
			Entity annot;
			for( Class<?> cls = getClass(); cls != BindableDao.class; cls = cls
					.getSuperclass() )
				if( (annot = cls.getAnnotation( Entity.class )) != null )
				{
					this.daoName = annot.name() == null ? cls.getSimpleName()
							: annot.name();
					break;
				}
			Thrower.throwNew( IllegalStateException.class, "{} is not an @{} ",
					getClass(), Entity.class );
		}
		return this.daoName;
	}

	protected abstract THIS prePersist( T source );

	public abstract T restore( LocalBinder binder );
}
