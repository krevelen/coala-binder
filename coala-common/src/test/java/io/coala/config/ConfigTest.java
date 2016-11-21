package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.TreeMap;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.coala.log.LogUtil;

/**
 * {@link ConfigTest} tests {@link GlobalConfig}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class ConfigTest
{
	/** */
	private static final Logger LOG = LogUtil.getLogger( ConfigTest.class );

	@Test
	public void testYamlToOWNER() throws JsonProcessingException, IOException
	{
		LOG.info( "Testing YAML + OWNER config" );
		final File yamlPath = new File( "log4j2-test.yaml" );
		final String baseKey = "base";
		final Properties props = YamlUtil.flattenYaml( yamlPath, baseKey );
		props.setProperty( "base" + GlobalConfig.KEY_SEP + "extra", "new val" );
		LOG.trace( "Flattened {} as: {}", yamlPath, props );

		final GlobalConfig conf = ConfigCache.getOrCreate( GlobalConfig.class,
				props );
		LOG.trace( "Got config: {} -> \n{}", conf,
				YamlUtil.toYAML( "no comment", props, baseKey ) );

		LOG.trace( "System props sorted: {}", new TreeMap<>( System.getProperties() )
				.toString().replaceAll( ", ", "\n\t" ) );
		LOG.trace( "System props as YAML: {}", YamlUtil.toYAML( "no comment",
				ConfigUtil.expand( System.getProperties() ) ) );
	}
}
