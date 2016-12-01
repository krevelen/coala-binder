package io.coala.config;

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

import io.coala.util.FileUtil;

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

	String BASE_KEY = "base";

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

	default <T extends GlobalConfig> T subConfig( final String baseKey,
		final Class<T> type, final Map<?, ?>... imports )
	{
		final Pattern pattern = Pattern.compile(
				"^" + Pattern.quote( baseKey + KEY_SEP ) + "(?<sub>.*)" );
		final Map<String, Object> subMap = ConfigUtil.export( this, pattern,
				"${sub}" );
		final String base = base(),
				sub = base == null ? baseKey : base + KEY_SEP + baseKey;
		subMap.put( BASE_KEY, sub );
		return ConfigFactory.create( type, ConfigUtil.join( subMap, imports ) );
	}

	default <T extends GlobalConfig> Map<String, T> subConfigs(
		final String baseKey, final Class<T> type, final Map<?, ?>... imports )
	{
		final Map<String, T> result = new TreeMap<>();
		for( String key : enumerate( baseKey + KEY_SEP ) )
			result.put( key,
					subConfig( baseKey + KEY_SEP + key, type, imports ) );
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
				ConfigUtil.join(
						YamlUtil.flattenYaml(
								FileUtil.toInputStream( yamlPath ) ),
						imports ) );
	}

	static GlobalConfig openYAML( final Map<?, ?>... imports )
		throws IOException
	{
		return openYAML( ConfigUtil.CONFIG_FILE_YAML_DEFAULT, imports );
	}
}
