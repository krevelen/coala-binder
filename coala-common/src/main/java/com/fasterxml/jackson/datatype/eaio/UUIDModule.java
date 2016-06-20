package com.fasterxml.jackson.datatype.eaio;

import java.io.IOException;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * {@link UUIDModule}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class UUIDModule extends SimpleModule
{
	private static final long serialVersionUID = 1L;

	/** */
//	private static final Logger LOG = LogUtil.getLogger( UUIDModule.class );

	public UUIDModule()
	{
		super( PackageVersion.VERSION );

		addDeserializer( UUID.class, new UUIDDeserializer() );

		addSerializer( UUID.class, ToStringSerializer.instance );

		addKeyDeserializer( UUID.class, new UUIDKeyDeserializer() );
	}

	// yes, will try to avoid duplicate registrations (if MapperFeature enabled)
	@Override
	public String getModuleName()
	{
		return getClass().getSimpleName();
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}

	@Override
	public boolean equals( Object o )
	{
		return this == o;
	}

	public final static class PackageVersion implements Versioned
	{
		public final static Version VERSION = VersionUtil.parseVersion( "2.7.1",
				"com.fasterxml.jackson.datatype", "jackson-datatype-eaio" );

		@Override
		public Version version()
		{
			return VERSION;
		}
	}

	public static class UUIDDeserializer extends JsonDeserializer<UUID>
	{
		@Override
		public UUID deserialize( final JsonParser p,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			final String v = p.getText();
			return v == null ? null : new UUID( v );
		}
	}

	public static class UUIDKeyDeserializer extends KeyDeserializer
	{
		@Override
		public Object deserializeKey( final String key,
			final DeserializationContext ctxt )
			throws IOException, JsonProcessingException
		{
			return key == null ? null : new UUID( key );
		}
	}
}
