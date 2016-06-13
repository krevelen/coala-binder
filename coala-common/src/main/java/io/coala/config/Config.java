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
 * @date $Date: 2014-06-03 14:26:09 +0200 (Tue, 03 Jun 2014) $
 * @version $Id: 60907217eef5350279876f1d64002469ef2132f1 $
 * @author <a href="mailto:Rick@almende.org">Rick</a>
 */
@LoadPolicy( LoadType.MERGE )
@Sources( { "file:${" + Coala.CONFIG_FILE_PROPERTY + "}",
		"classpath:${" + Coala.CONFIG_FILE_PROPERTY + "}",
		"file:${user.dir}/" + Coala.CONFIG_FILE_DEFAULT,
		"file:~/" + Coala.CONFIG_FILE_DEFAULT,
		"classpath:" + Coala.CONFIG_FILE_DEFAULT } )
@Separator( Coala.CONFIG_VALUE_SEP )
public interface Config extends Mutable
{

}
