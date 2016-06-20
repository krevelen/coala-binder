package io.coala.eve3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentBuilder;
import com.almende.eve.deploy.Boot;
import com.almende.eve.instantiation.InstantiationServiceBuilder;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.scheduling.SimpleSchedulerBuilder;
import com.almende.eve.state.file.FileStateBuilder;
import com.almende.eve.state.memory.MemoryStateBuilder;
import com.almende.eve.transport.http.HttpTransportBuilder;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.coala.agent.AgentID;
import io.coala.agent.AgentStatusUpdate;
import io.coala.bind.Binder;
import io.coala.bind.BinderFactory;
import io.coala.capability.admin.CreatingCapability;
import io.coala.capability.replicate.ReplicationConfig;
import io.coala.json.JsonUtil;
import io.coala.log.LogUtil;
import io.coala.model.ModelComponentIDFactory;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * {@link MessagingTest}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
public class MessagingTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger( MessagingTest.class );

	/** */
	public static AgentID senderAgentID = null;

	/** */
	public static AgentID receiverAgentID = null;

	@Ignore // FIXME !
	@Test
	public void messagingTest() throws Exception
	{
		LOG.trace( "Start eve messaging test" );

		final Binder binder = BinderFactory.Builder.fromFile()
				.withProperty( ReplicationConfig.class,
						ReplicationConfig.MODEL_NAME_KEY,
						"testModel" + System.currentTimeMillis() )
				.build().create( "_unittest_" );

		senderAgentID = binder.inject( ModelComponentIDFactory.class )
				.createAgentID( "senderAgent" );

		receiverAgentID = binder.inject( ModelComponentIDFactory.class )
				.createAgentID( "receiverAgent" );

		final CreatingCapability booterSvc = binder
				.inject( CreatingCapability.class );
		final CountDownLatch latch = new CountDownLatch( 2 );

		booterSvc.createAgent( receiverAgentID, MessagingTestAgent.class )
				.subscribe( new Action1<AgentStatusUpdate>()
				{
					@Override
					public void call( final AgentStatusUpdate update )
					{
						LOG.trace( "Observed status update: " + update );

						if( update.getStatus().isFinishedStatus()
								|| update.getStatus().isFailedStatus() )
							latch.countDown();
					}
				} );

		booterSvc.createAgent( senderAgentID, MessagingTestAgent.class )
				.subscribe( new Action1<AgentStatusUpdate>()
				{
					@Override
					public void call( final AgentStatusUpdate update )
					{
						LOG.trace( "Observed status update: " + update );

						if( update.getStatus().isFinishedStatus()
								|| update.getStatus().isFailedStatus() )
							latch.countDown();
					}
				} );

		latch.await( 20, TimeUnit.SECONDS );
		assertTrue( "Agent(s) did not all finish or fail",
				latch.getCount() == 0 );
	}

	public interface EveAgent
	{

		String PAYLOAD_FIELD_NAME = "payload";

		String ADDRESS_FIELD_NAME = "address";

		String RECEIVE_METHOD_NAME = "doReceive";

		String SEND_METHOD_NAME = "doSend";

		@Access( AccessType.PUBLIC )
			void doReceive( @Name( PAYLOAD_FIELD_NAME ) JsonNode payload);

		@Access( AccessType.SELF )
			void doSend( @Name( PAYLOAD_FIELD_NAME ) JsonNode payload,
				@Name( ADDRESS_FIELD_NAME ) URI receiverURI) throws IOException;

		Observable<JsonNode> receiveStream();

	}

	public static class MyEveAgent extends Agent implements EveAgent
	{
		private final transient Subject<JsonNode, JsonNode> incoming = PublishSubject
				.create();

		@Override
		public void doReceive( final JsonNode payload )
		{
			this.incoming.onNext( payload );
		}

		@Override
		public Observable<JsonNode> receiveStream()
		{
			return this.incoming.asObservable();
		}

		protected void scheduleSend( final JsonNode payload,
			final URI receiverURI )
		{
			final JSONRequest req = new JSONRequest( EveAgent.SEND_METHOD_NAME,
					(ObjectNode) JsonUtil.getJOM().createObjectNode()
							.put( EveAgent.ADDRESS_FIELD_NAME,
									receiverURI.toASCIIString() )
							.set( EveAgent.PAYLOAD_FIELD_NAME, payload ) );
			final String res = getScheduler().schedule( req, 0 );
			LOG.info( "Scheduled " + req + ": " + res );
		}

		@Override
		protected void destroy()
		{
			this.incoming.onCompleted();
			super.destroy();
		}

		@Override
		public final void doSend( final JsonNode payload,
			final URI receiverURI ) throws IOException
		{
			final ObjectNode params = JsonUtil.getJOM().createObjectNode();
			params.set( PAYLOAD_FIELD_NAME,
					JsonUtil.getJOM().valueToTree( payload ) );
			LOG.info( getId() + " calling " + receiverURI + ": " + params );
			call( receiverURI, RECEIVE_METHOD_NAME, params,
					new AsyncCallback<Void>()
					{
						@Override
						public void onFailure( final Exception e )
						{
							LOG.error( getId() + " failed calling "
									+ receiverURI + ": " + params, e );
						}

						@Override
						public void onSuccess( final Void result )
						{
							LOG.info( getId() + " successfully called "
									+ receiverURI + "#" + RECEIVE_METHOD_NAME
									+ params + "=" + result );
						}
					} );
		}
	}

	@Test
	public void myTest() throws InterruptedException
	{
		final ObjectNode eveCfg = (ObjectNode) JOM.createObjectNode().set(
				"instantiationServices",
				JOM.createArrayNode().add( JOM.createObjectNode()
						.put( "class",
								InstantiationServiceBuilder.class.getName() )
						.set( "state",
								JOM.createObjectNode()
										.put( "class",
												FileStateBuilder.class
														.getName() )
										.put( "path", ".wakeservices" )
										.put( "id", "testWakeService" ) ) ) );

		final ObjectNode agCfg = JOM.createObjectNode().put( "id", "my-agent" )
				.put( "class", MyEveAgent.class.getName() );
		agCfg.set( "state", JOM.createObjectNode().put( "class",
				MemoryStateBuilder.class.getName() ) );
		agCfg.set( "scheduler", JOM.createObjectNode().put( "class",
				SimpleSchedulerBuilder.class.getName() ) );
		agCfg.set( "transports",
				JOM.createArrayNode().add( JOM.createObjectNode()
						.put( "class", HttpTransportBuilder.class.getName() )
						.put( "servletUrl", "http://127.0.0.1:8080/agents/" )
						.put( "doAuthentication", "false" )
						.put( "doShortcut", "true" )
						.put( "servletLauncher", "JettyLauncher" ) ) );

		Boot.boot( eveCfg );

		final MyEveAgent ag = (MyEveAgent) new AgentBuilder().with( agCfg )
				.build();
		LOG.info( "Built agent with config: " + ag.getConfig() );

		final CountDownLatch latch = new CountDownLatch( 1 );
		ag.receiveStream().subscribe( new Observer<JsonNode>()
		{
			@Override
			public void onCompleted()
			{
				LOG.info( ag.getId() + " receive stream complete" );
			}

			@Override
			public void onError( final Throwable e )
			{
				LOG.error( ag.getId() + " receive stream fails", e );
			}

			@Override
			public void onNext( final JsonNode payload )
			{
				LOG.info( ag.getId() + " received: " + payload );
				latch.countDown();
			}
		} );

		ag.scheduleSend( JOM.getInstance().valueToTree( "myPayload" ),
				ag.getUrls().get( 0 ) );

		latch.await( 3, TimeUnit.SECONDS );
		assertEquals( "Not all messages received", latch.getCount(), 0 );
		LOG.info( "All messages received" );
	}
}
