package io.coala.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;

import io.coala.exception.ExceptionFactory;
import io.coala.log.LogUtil;

/**
 * {@link SerializableUtil}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class SerializableUtil
{

	/** used for de/serialization to {@link String} */
	private static final Base64 CODER = new Base64();

	/** */
	private static final Logger LOG = LogUtil
			.getLogger( SerializableUtil.class );

	/**
	 * {@link SerializableUtil} singleton constructor
	 */
	private SerializableUtil()
	{
		// empty
	}

	/**
	 * @param serializable the {@link String} representation of the
	 *            {@link Serializable} object
	 * @param valueType the type of value to deserialize
	 * @return the deserialized {@link Object}
	 */
	public static <T extends Serializable> T
		deserialize( final String serializable, final Class<T> valueType )
	{
		try( final ObjectInputStream in = new ObjectInputStream(
				new ByteArrayInputStream(
						(byte[]) CODER.decode( serializable ) ) ) )
		{
			return valueType.cast( in.readObject() );
		} catch( final Throwable e )
		{
			throw ExceptionFactory.createUnchecked( e,
					"Problem unmarshalling {} from {}", valueType,
					serializable );
		}
	}

	/**
	 * Write the object to a Base64 string
	 * 
	 * @param object the {@link Serializable} object to serialize
	 * @return the {@link String} representation of the {@link Serializable}
	 *         object
	 */
	public static String serialize( final Serializable object )
	{
		try( final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				final ObjectOutputStream oos = new ObjectOutputStream( baos ) )
		{
			oos.writeObject( object );
			final byte[] data = baos.toByteArray();
			LOG.trace( "Serialized:\n\n" + new String( data ) );
			return new String( CODER.encode( data ) );
		} catch( final Throwable e )
		{
			throw ExceptionFactory.createUnchecked( e, "Problem marshalling {}",
					object.getClass() );
		}
	}
}
