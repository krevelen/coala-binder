package io.coala.guice4;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalBinder.LauncherConfig;
import io.coala.guice4.Guice4LocalBinder.Guice4Launcher;
import io.coala.log.LogUtil;

/**
 * {@link Guice4LocalBinderTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class Guice4LocalBinderTest
{
	/** */
	private static final Logger LOG = LogUtil
			.getLogger( Guice4LocalBinderTest.class );

	static class MyInjectable
	{

		/** */
		private static final Logger LOG = LogUtil
				.getLogger( Guice4LocalBinderTest.MyInjectable.class );

		/** should be injected */
		private final LocalBinder binder;

		@Inject
		public MyInjectable( final LocalBinder binder )
		{
			this.binder = binder;
			LOG.trace( "Instantiated {} with binder: {}", getClass(), binder );
		}

		@Override
		public String toString()
		{
			return super.toString() + this.binder.toString();
		}
	}

	@Test
	public void testLauncher() throws JsonProcessingException
	{
		final String[] agentNames = { "agent1", "agent2" };

		final Map<String, String> imports = new HashMap<>();
		imports.put( LauncherConfig.LAUNCH_IDENTIFIERS_KEY,
				String.join( LauncherConfig.VALUE_SEP, agentNames ) );
		for( String id : agentNames )
			imports.put(
					LauncherConfig.keyFor( id,
							LocalBinder.Config.LAUNCH_TYPES_KEY ),
					String.join( LauncherConfig.VALUE_SEP,
							MyInjectable.class.getName(),
							MyInjectable.class.getName() ) );

		LOG.trace( "Starting Guice4 test with imports: {}", imports );
		final LauncherConfig config = LauncherConfig.getOrCreate( imports );

		LOG.trace( "Using launcher config: {}", config );
		Guice4Launcher.of( config );
	}
}
