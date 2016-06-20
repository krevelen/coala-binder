package io.coala.config;

import io.coala.Coala;

import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;
import org.aeonbits.owner.Config.Separator;
import org.aeonbits.owner.Config.Sources;
import org.aeonbits.owner.Mutable;

/**
 * {@link Config}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@LoadPolicy( LoadType.MERGE )
@Sources( { "file:${" + Coala.CONFIG_FILE_PROPERTY + "}",
		"classpath:${" + Coala.CONFIG_FILE_PROPERTY + "}",
		"file:${user.dir}/" + Coala.CONFIG_FILE_DEFAULT,
		"file:~/" + Coala.CONFIG_FILE_DEFAULT,
		"classpath:" + Coala.CONFIG_FILE_DEFAULT } )
@Separator( Coala.CONFIG_VALUE_SEP )
@Deprecated
public interface Config extends Mutable
{
}
