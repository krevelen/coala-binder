/* $Id$
 * 
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.coala.akka;

import io.coala.log.LogUtil;

import org.apache.log4j.Logger;
import org.junit.Test;

import akka.Main;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.kernel.Bootable;

import com.typesafe.config.ConfigFactory;

/**
 * {@link AkkaTest} implements examples from the original documentation found at
 * <a href="http://doc.akka.io/docs/akka/2.2.3/java/">doc.akka.io</a>
 */
public class AkkaTest
{

	/** */
	private static final Logger LOG = LogUtil.getLogger(AkkaTest.class);

	/**
	 * {@link Msg}
	 */
	public static enum Msg
	{
		/** */
		GREET,

		/** */
		DONE,

		;
	}

	/**
	 * {@link Greeter}
	 */
	public static class Greeter extends UntypedActor
	{

		@Override
		public void onReceive(final Object msg)
		{
			if (msg == Msg.GREET)
			{
				LOG.trace("Hello World!");
				getSender().tell(Msg.DONE, getSelf());
			} else
				unhandled(msg);
		}

	}

	/**
	 * {@link HelloWorld}
	 */
	public static class HelloWorld extends UntypedActor
	{

		@Override
		public void preStart()
		{
			// create the greeter actor
			final ActorRef greeter = getContext().actorOf(
					Props.create(Greeter.class), "greeter");
			// tell it to perform the greeting
			greeter.tell(Msg.GREET, getSelf());
		}

		@Override
		public void onReceive(final Object msg)
		{
			if (msg == Msg.DONE)
			{
				// when the greeter is done, stop this actor and with it the
				// application
				getContext().stop(getSelf());
			} else
				unhandled(msg);
		}
	}

	/**
	 * {@link HelloActor}
	 */
	public static class HelloActor extends UntypedActor
	{
		final ActorRef worldActor = getContext().actorOf(
				Props.create(WorldActor.class));

		@Override
		public void onReceive(final Object message)
		{
			if (message == "start")
				this.worldActor.tell("Hello", getSelf());
			else if (message instanceof String)
				LOG.trace(String.format("Received message '%s'", message));
			else
				unhandled(message);
		}
	}

	/**
	 * {@link WorldActor}
	 */
	public static class WorldActor extends UntypedActor
	{
		@Override
		public void onReceive(final Object message)
		{
			if (message instanceof String)
				getSender().tell(((String) message).toUpperCase() + " world!",
						getSelf());
			else
				unhandled(message);
		}
	}

	/**
	 * {@link HelloKernel}
	 */
	public static class HelloKernel implements Bootable
	{
		final ActorSystem system = ActorSystem.create("hellokernel",
				ConfigFactory.load().getConfig("akka-hellokernel"));

		@Override
		public void startup()
		{
			this.system.actorOf(Props.create(HelloActor.class)).tell("start",
					null);
		}

		@Override
		public void shutdown()
		{
			this.system.shutdown();
		}
	}

	// @Test
	public void helloWorld()
	{
		Main.main(new String[] { HelloWorld.class.getName() });
	}

	@Test
	public void helloKernel()
	{
		final Bootable kernel = new HelloKernel();
		kernel.startup();

		// kernel.shutdown();
	}
}
