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
package io.coala.process;

import io.coala.lifecycle.Machine;

/**
 * {@link Processor}
 * 
 * @param <P> the (super)type of {@link Process} to execute
 * @param <THIS> the concrete type of {@link Processor}
 */
@Deprecated
public interface Processor<P extends Process<?>>
	extends Machine<BasicProcessorStatus>
{

	/** @return */
	// Class<P> getProcessType();

	/** @return */
	// ProcessID getCurrentProcess();

	/** @param process */
	void process( P process );

	/**
	 * 
	 * @param process
	 * @return
	 */
	// P cancel(ProcessID processID);

}
