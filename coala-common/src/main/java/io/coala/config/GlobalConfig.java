package io.coala.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Separator;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.ConfigFactory;

/**
 * {@link GlobalConfig} by default tries to load from a location specified with
 * System property name {@link ConfigUtil#CONFIG_FILE_PROPERTY}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 * @see ConfigFactory#setProperty(String, String)
 * @see ConfigFactory#create(Class, Map[])
 * @see ConfigCache#getOrCreate(Class, Map[])
 */
@LoadPolicy( LoadType.MERGE )
@Sources( { "file:${" + ConfigUtil.CONFIG_FILE_PROPERTY + "}",
		"classpath:${" + ConfigUtil.CONFIG_FILE_PROPERTY + "}",
		"file:${user.dir}/" + ConfigUtil.CONFIG_FILE_DEFAULT,
		"file:~/" + ConfigUtil.CONFIG_FILE_DEFAULT,
		"classpath:" + ConfigUtil.CONFIG_FILE_DEFAULT } )
@Separator( GlobalConfig.VALUE_SEP )
public interface GlobalConfig extends YamlConfig
{

	String KEY_SEP = ConfigUtil.CONFIG_KEY_SEP;

	String VALUE_SEP = ConfigUtil.CONFIG_VALUE_SEP;

	String BASE_KEY = "path";

	/**
	 * @return the key path or base for relative properties in config extensions
	 */
	@Key( BASE_KEY )
	String base();

	/**
	 * @param prefix the key prefix key {@link String} to match
	 * @return a {@link Collection} of unique {@link String} sub-keys
	 */
	default Collection<String> enumerate( final String prefix )
	{
		return ConfigUtil.enumerate( this, prefix, null );
	}

	default <T extends GlobalConfig> T subConfig( final String subKey,
		final Class<T> type, final Map<?, ?>... imports )
	{
		final Pattern pattern = Pattern.compile(
				"^" + Pattern.quote( subKey + KEY_SEP ) + "(?<sub>.*)" );
		final Map<String, Object> subMap = ConfigUtil.export( this, pattern,
				"${sub}" );
		final String base = base(),
				sub = base == null ? subKey : base + KEY_SEP + subKey;
		subMap.put( BASE_KEY, sub );
		return ConfigCache.getOrCreate( sub, type,
				ConfigUtil.join( subMap, imports ) );
	}

	default <T extends GlobalConfig> Map<String, T> subConfigs(
		final String subKey, final Class<T> type, final Map<?, ?>... imports )
	{
		final Map<String, T> result = new TreeMap<>();
		for( String key : enumerate( subKey + KEY_SEP ) )
			result.put( key,
					subConfig( subKey + KEY_SEP + key, type, imports ) );
//		LogUtil.getLogger( GlobalConfig.class )
//				.trace( "Got subConfigs for {}: {}", subKey, result );
		return result;
	}

	static GlobalConfig create( final Map<?, ?>... imports )
	{
		return ConfigCache.getOrCreate( GlobalConfig.class, imports );
	}

	static GlobalConfig openYAML( final String yamlPath,
		final Map<?, ?>... imports ) throws IOException
	{
		return ConfigFactory.create( GlobalConfig.class,
				ConfigUtil.join( YamlUtil.flattenYaml( new File(yamlPath) ), imports ) );
	}

	static GlobalConfig openYAML( final Map<?, ?>... imports )
		throws IOException
	{
		return openYAML( ConfigUtil.CONFIG_FILE_YAML_DEFAULT, imports );
	}
}
