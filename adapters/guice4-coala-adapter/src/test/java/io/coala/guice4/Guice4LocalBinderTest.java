package io.coala.guice4;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import org.aeonbits.owner.ConfigCache;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.LocalBinder;
import io.coala.bind.LocalBinder.Config;
import io.coala.bind.LocalBinder.LaunchConfig;
import io.coala.config.YamlConfig;
import io.coala.config.YamlUtil;
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
		/** should be injected */
		private final LocalBinder binder;

		@Inject
		public MyInjectable( final LocalBinder binder )
		{
			this.binder = binder;
			LOG.trace( "{} instantiated with binder: {}",
					getClass().getSimpleName(), this.binder );
		}
	}

	static class MyInjecting
	{
		private final LocalBinder binder;

		@Inject
		public MyInjecting( final LocalBinder binder )
		{
			this.binder = binder;
			this.binder.inject( MyInjectable.class );
			this.binder.context().set( "asd", System.currentTimeMillis() );
			this.binder.reset( MyInjectable.class, MyInjectable.class );
			LOG.trace( "{} instantiated with binder: {}",
					getClass().getSimpleName(), this.binder );
		}
	}

	@Test
	public void testLauncher() throws IOException
	{
		final String[] agentNames = { "agent1", "agent2" };
//		imports.put( LauncherConfig.LAUNCHABLES_KEY,
//				String.join( LauncherConfig.VALUE_SEP, agentNames ) );
		final StringBuilder yaml = new StringBuilder( "# from YAML" );
		final String nl = "\n", colon = ": ", tab = "  ", item = "- ";
		for( String id : agentNames )
		{
			yaml.append( nl ).append( id ).append( colon ).append( nl );
			yaml.append( tab ).append( LaunchConfig.LAUNCH_KEY ).append( colon )
					.append( true ).append( nl );
			yaml.append( tab ).append( Config.BINDER_KEY ).append( colon )
					.append( nl );
			yaml.append( tab ).append( tab ).append( Config.BINDING_KEY )
					.append( colon ).append( nl );
			// MyInjectable
			yaml.append( tab ).append( tab ).append( item )
					.append( Config.IMPLEMENTATION_KEY ).append( colon )
					.append( MyInjectable.class.getName() ).append( nl );
//			yaml.append( tab ).append( tab ).append( tab )
//					.append( Config.INITABLE_KEY ).append( colon )
//					.append( false ).append( nl );
			yaml.append( tab ).append( tab ).append( tab )
					.append( Config.MUTABLE_KEY ).append( colon ).append( true )
					.append( nl );
			// MyInjecting
			yaml.append( tab ).append( tab ).append( item )
					.append( Config.IMPLEMENTATION_KEY ).append( colon )
					.append( MyInjecting.class.getName() ).append( nl );
			yaml.append( tab ).append( tab ).append( tab )
					.append( Config.INITABLE_KEY ).append( colon )
					.append( true ).append( nl );
//			yaml.append( tab ).append( tab ).append( tab )
//					.append( Config.MUTABLE_KEY ).append( colon )
//					.append( false );
		}
		LOG.trace( "Starting Guice4 test with yaml: {}", yaml );
		final Map<?, ?> imports = new TreeMap<>( YamlUtil.flattenYaml( yaml ) );
		final LaunchConfig config = ConfigCache.getOrCreate( LaunchConfig.class,
				imports );

		LOG.trace( "Launcher config export: {}",
				YamlConfig.toYAML( config, "to YAML" ) );
		Guice4Launcher.of( config );
	}
}
