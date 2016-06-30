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
package io.coala.capability.embody;

import rx.Observable;
import io.coala.agent.Agent;
import io.coala.capability.BasicCapabilityStatus;
import io.coala.capability.Capability;
import io.coala.capability.CapabilityFactory;

/**
 * {@link PerceivingCapability} embodies an {@link Agent}s within some
 * environment in order to connect sensors (via publish-subscribe pattern) and
 * effectors
 */
@Deprecated
public interface PerceivingCapability extends Capability<BasicCapabilityStatus>
{

	/**
	 * {@link Factory}
	 */
	interface Factory extends CapabilityFactory<PerceivingCapability>
	{
		// empty
	}

	/**
	 * @return
	 */
	Observable<Percept> perceive();

	//
	// public interface EnvironmentID
	// {
	//
	// }
	//
	// public class SensableID<T extends Comparable<T> & Serializable, THIS
	// extends SensableID<T, THIS>>
	// extends AbstractIdentifier<T, THIS>
	// {
	//
	// /** */
	// private static final long serialVersionUID = 1L;
	//
	// }
	//
	// public interface Sensable<ID extends SensableID<?, ID>, THIS extends
	// Sensable<ID, THIS>>
	// extends Identifiable<ID, THIS>, Serializable
	// {
	// EnvironmentID getEnvironmentID();
	// }
	//
	// public class SensorID<T extends Comparable<T> & Serializable, THIS
	// extends SensorID<T, THIS>>
	// extends AbstractIdentifier<T, THIS>
	// {
	//
	// /** */
	// private static final long serialVersionUID = 1L;
	//
	// }
	//
	// public interface Sensor<ID extends SensorID<?, ID>, S extends Sensable<?,
	// S>, THIS extends Sensor<ID, S, THIS>>
	// extends Identifiable<ID, THIS>, Serializable
	// {
	// Class<S> getSensedType();
	// }
	//
	// public class ActionID<T extends Comparable<T> & Serializable, THIS
	// extends ActionID<T, THIS>>
	// extends AbstractIdentifier<T, THIS>
	// {
	//
	// /** */
	// private static final long serialVersionUID = 1L;
	//
	// }
	//
	// public interface Action<ID extends ActionID<?, ID>, THIS extends
	// Action<ID, THIS>>
	// extends Identifiable<ID, THIS>, Serializable
	// {
	// EnvironmentID getEnvironmentID();
	// }
	//
	// /**
	// * @return
	// */
	// Set<EnvironmentID> getEnvironmentIDs();
	//
	// /**
	// * @return a set of types that the agent is able to sense/observe/perceive
	// */
	// Set<Class<? extends Sensable<?, ?>>> getSensables();
	//
	// /**
	// * @return a set of types that the agent is able to sense/observe/perceive
	// */
	// Map<EnvironmentID, Collection<Class<? extends Sensable<?, ?>>>>
	// getSensablesByEnvironment();
	//
	// /**
	// * @param sensor the {@link Sensor} (a listener for {@link Sensable}
	// * objects) to register
	// * @throws Exception if {@link Sensor}'s sensed {@link Sensable}s do not
	// * occur in its specified {@link EnvironmentID}
	// */
	// void register(Sensor<?, ?, ?> sensor) throws Exception;
	//
	// /**
	// * @return a set of types that the agent is able to
	// attempt/perform/execute
	// */
	// Set<Class<? extends Action<?, ?>>> getActions();
	//
	// /**
	// * @return a set of types that the agent is able to
	// attempt/perform/execute
	// */
	// Map<EnvironmentID, Set<Class<? extends Action<?, ?>>>>
	// getActionsByEnvironment();
	//
	// /**
	// * @param action
	// * @throws Exception
	// */
	// void perform(Action<?, ?> action) throws Exception;
}
