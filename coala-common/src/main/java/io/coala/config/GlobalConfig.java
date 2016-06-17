package io.coala.config;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Separator;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.Converter;
import org.aeonbits.owner.Mutable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.coala.log.LogUtil;

/**
 * {@link GlobalConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 * @see ConfigFactory#setProperty(String, String)
 */
@LoadPolicy( LoadType.MERGE )
@Sources( { "file:${" + GlobalConfig.CONFIG_FILE_PROPERTY + "}",
		"classpath:${" + GlobalConfig.CONFIG_FILE_PROPERTY + "}",
		"file:${user.dir}/" + GlobalConfig.CONFIG_FILE_DEFAULT,
		"file:~/" + GlobalConfig.CONFIG_FILE_DEFAULT,
		"classpath:" + GlobalConfig.CONFIG_FILE_DEFAULT } )
@Separator( GlobalConfig.CONFIG_VALUE_SEP )
public interface GlobalConfig extends Mutable
{

	/** regular expression to split values, see {@link String#split(String)} */
	String CONFIG_VALUE_SEP = ",";

	/** regular expression to split values, see {@link String#split(String)} */
	String CONFIG_KEY_SEP = ".";

	/** Property name for setting the (relative) configuration file name */
	String CONFIG_FILE_PROPERTY = "coala.configuration";

	/** Default (relative path) value for the configuration file name */
	String CONFIG_FILE_DEFAULT = "coala.yaml";

	class YamlConverter implements Converter<Properties>
	{
		private static ObjectMapper mapper = null;

		private synchronized static ObjectMapper getOM()
		{
			if( mapper == null ) mapper = new ObjectMapper( new YAMLFactory() );
			return mapper;
		}

		private void store( final JsonNode tree, final Properties props,
			final StringBuilder key )
		{
			switch( tree.getNodeType() )
			{
			case POJO:
			case OBJECT:
				final Iterator<Entry<String, JsonNode>> it = tree.fields();
				while( it.hasNext() )
				{
					final Entry<String, JsonNode> entry = it.next();
					final StringBuilder subKey = (key.length() == 0 ? key
							: key.append( CONFIG_KEY_SEP ))
									.append( entry.getKey() );
					store( entry.getValue(), props, subKey );
				}
				break;
			case ARRAY:
				for( int i = tree.size(); i >= 0; i-- )
				{
					final StringBuilder subKey = (key.length() == 0 ? key
							: key.append( CONFIG_KEY_SEP )).append( i );
					store( tree.get( i ), props, subKey );
				}
				break;
			case BINARY:
			case BOOLEAN:
			case NUMBER:
			case STRING:
				props.setProperty( key.toString(), tree.asText() );
				break;
			case NULL:
				props.setProperty( key.toString(), "null" );
				break;
			case MISSING:
			default:
				props.setProperty( key.toString(), "" );
				break;

			}
		}

		@Override
		public Properties convert( final Method method, final String input )
		{
			final Properties result = new Properties();
			try
			{
				store( getOM().readTree( input ), result, new StringBuilder() );
			} catch( final Exception e )
			{
				LogUtil.getLogger( YamlConverter.class )
						.error( "Problem parsing yaml input: " + input, e );
			}
			return result;
		}
	}
}
