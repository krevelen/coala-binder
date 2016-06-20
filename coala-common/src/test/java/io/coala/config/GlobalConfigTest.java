package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.coala.log.LogUtil;

/**
 * {@link GlobalConfigTest} tests {@link GlobalConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class GlobalConfigTest
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( GlobalConfigTest.class );

	@Test
	public void testYamlToOWNER() throws JsonProcessingException, IOException
	{
		LOG.trace( "Testing YAML + OWNER config" );
		final File yamlPath = new File( "log4j2-test.yaml" );
		final String baseKey = "base";
		final Properties props = ConfigUtil.flattenYaml( yamlPath, baseKey );
		LOG.trace( "Flattened {} as: {}", yamlPath, props );
		final GlobalConfig conf = ConfigCache.getOrCreate( GlobalConfig.class,
				props );
		/*ConfigUtil.getYamlMapper().registerModule( new SimpleModule()
				.addSerializer( String.class, new JsonSerializer<String>()
				{

					@Override
					public void serializeWithType( String value,
						JsonGenerator gen, SerializerProvider serializers,
						TypeSerializer typeSer ) throws IOException
					{
						this.serialize( value, gen, serializers );
					}

					@Override
					public void serialize( String value, JsonGenerator gen,
						SerializerProvider serializers )
						throws IOException, JsonProcessingException
					{
						System.err.println(value);
						gen.writeRawValue( value );
					}
				} ) );*/
		LOG.trace( "Got config: {} -> \n{}", conf,
				ConfigUtil.getYamlMapper().writer().writeValueAsString(
						ConfigUtil.expand( props, baseKey ) ) );
	}
}
