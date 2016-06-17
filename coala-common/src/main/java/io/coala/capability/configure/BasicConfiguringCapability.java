package io.coala.capability.configure;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.coala.bind.Binder;
import io.coala.capability.BasicCapability;
import io.coala.config.CoalaPropertyGetter;
import io.coala.config.PropertyGetter;
import io.coala.log.InjectLogger;
import io.coala.message.Message;

/**
 * {@link BasicConfiguringCapability}
 */
@Deprecated
public class BasicConfiguringCapability extends BasicCapability
	implements ConfiguringCapability
{

	/** */
	private static final long serialVersionUID = 1L;

	/** */
	@InjectLogger
	private Logger LOG;

	/**
	 * {@link BasicConfiguringCapability} CDI constructor
	 * 
	 * @param binder the {@link Binder}
	 */
	@Inject
	private <T extends Message<?>> BasicConfiguringCapability(
		final Binder binder )
	{
		super( binder );
	}

	@Override
	public PropertyGetter getProperty( final String key,
		final String... prefixes )
	{
		return new CoalaPropertyGetter(
				CoalaPropertyGetter.addKeyPrefixes( key, prefixes ) );
	}
}