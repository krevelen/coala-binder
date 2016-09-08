package io.coala.enterprise.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import io.coala.bind.LocalBinder;
import io.coala.exception.Thrower;

/**
 * {@link AbstractLocalEntity} is a data access object for the location
 * dimension
 * <p>
 * TODO: explore inject-persist
 * 
 * @version $Id: ccb850afe9da1c0e05dabbd3374aa241dfa9e0e2 $
 * @author Rick van Krevelen
 */
@Entity
public abstract class AbstractLocalEntity<T, THIS extends AbstractLocalEntity<T, ?>>
	extends AbstractDao<T, THIS>
{

	@Id
	@GeneratedValue( strategy = GenerationType.IDENTITY )
	@Column( name = "PK", unique = true, nullable = false, insertable = false,
		updatable = false )
	protected Long pk;

	/** time stamp of insert, as per http://stackoverflow.com/a/3107628 */
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "CREATED_TS", insertable = false, updatable = false
	/* , columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP" */ )
	protected Date created;

	/** time stamp of last update; should never change */
	@Version
	@Temporal( TemporalType.TIMESTAMP )
	@Column( name = "UPDATED_TS", nullable = false, insertable = false,
		updatable = false )
	protected Date updated;

	@Column( name = "CONTEXT", nullable = true, updatable = false )
	protected String localID;

	@Override
	public T restore( LocalBinder binder )
	{
		// verify context
		if( !binder.id().equals( this.localID ) )
			Thrower.throwNew( IllegalStateException.class,
					"DAO from {} is not meant for {}", this.localID,
					binder.id() );
		return doRestore( binder );
	}

	abstract T doRestore( LocalBinder binder );

}
