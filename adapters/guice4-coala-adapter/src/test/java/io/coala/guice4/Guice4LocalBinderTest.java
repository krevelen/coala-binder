package io.coala.guice4;

import java.io.IOException;

import javax.inject.Inject;

import org.aeonbits.owner.ConfigCache;
import org.aeonbits.owner.Mutable;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import io.coala.bind.BinderConfig;
import io.coala.bind.Launcher.LaunchConfig;
import io.coala.bind.LocalBinder;
import io.coala.bind.LocalConfig;
import io.coala.bind.ProviderConfig;
import io.coala.config.InjectConfig;
import io.coala.config.InjectConfig.Scope;
import io.coala.config.YamlUtil;
import io.coala.log.InjectLogger;
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

	interface MyConfig extends Mutable
	{
		String MY_VALUE_KEY = "asd";

		@Key( MY_VALUE_KEY )
		@DefaultValue( "defaultValue" )
		String myValue();
	}

	static class MyInjectable
	{
		/** should be injected */
		private final LocalBinder binder;

		/** should be injected */
		@InjectLogger
		private Logger LOG;

		/** should be injected */
		@InjectConfig( cache = Scope.FIELD )
		private MyConfig config;

		@Inject
		public MyInjectable( final LocalBinder binder )
		{
			this.binder = binder;
		}

		void doLog()
		{
			LOG.trace( "{} instantiated with binder: {} and config: {}",
					getClass().getSimpleName(), this.binder, this.config );
			this.config.setProperty( MyConfig.MY_VALUE_KEY, "newValue" );
		}
	}

	static class MyInjecting
	{
		/** should be injected */
		private final LocalBinder binder;

		/** should be injected */
		@InjectLogger
		private Logger LOG;

		@Inject
		public MyInjecting( final LocalBinder binder )
		{
			this.binder = binder;
			this.binder.inject( MyInjectable.class ).doLog();
			this.binder.context().set( "asd", System.currentTimeMillis() );
//			this.binder.reset( MyInjectable.class, MyInjectable.class );
		}

		void doLog()
		{
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
			yaml.append( tab ).append( LocalConfig.BINDER_KEY ).append( colon )
					.append( nl );
			yaml.append( tab ).append( tab )
					.append( BinderConfig.PROVIDERS_KEY ).append( colon )
					.append( nl );
			// MyInjectable
			yaml.append( tab ).append( tab ).append( item )
					.append( ProviderConfig.IMPLEMENTATION_KEY ).append( colon )
					.append( MyInjectable.class.getName() ).append( nl );
//			yaml.append( tab ).append( tab ).append( tab )
//					.append( ProviderConfig.INITABLE_KEY ).append( colon )
//					.append( false ).append( nl );
			yaml.append( tab ).append( tab ).append( tab )
					.append( ProviderConfig.MUTABLE_KEY ).append( colon )
					.append( true ).append( nl );
			// MyInjecting
			yaml.append( tab ).append( tab ).append( item )
					.append( ProviderConfig.IMPLEMENTATION_KEY ).append( colon )
					.append( MyInjecting.class.getName() ).append( nl );
			yaml.append( tab ).append( tab ).append( tab )
					.append( ProviderConfig.INITABLE_KEY ).append( colon )
					.append( true ).append( nl );
//			yaml.append( tab ).append( tab ).append( tab )
//					.append( ProviderConfig.MUTABLE_KEY ).append( colon )
//					.append( false );
		}
		LOG.trace( "Starting Guice4 test with yaml: {}", yaml );
		final LaunchConfig config = ConfigCache.getOrCreate( LaunchConfig.class,
				YamlUtil.flattenYaml( yaml ) );

		LOG.trace( "Launching config, export: {}", config.toYAML( "to YAML" ) );
		Guice4Launcher.of( config );
	}
}
