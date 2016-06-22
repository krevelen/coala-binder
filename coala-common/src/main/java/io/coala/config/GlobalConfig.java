package io.coala.config;

import java.util.Map;

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
}
