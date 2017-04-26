package nl.tudelft.simulation.naming;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.spi.InitialContextFactory;

import nl.tudelft.simulation.logger.Logger;

/**
 * A factory for JVMContext instances, automatically invoked by JNDI when the
 * correct jndi.properties file has been used.
 * <p>
 * (c) copyright 2002-2005 <a href="http://www.simulation.tudelft.nl">Delft
 * University of Technology </a>, the Netherlands. <br>
 * See for project information <a href="http://www.simulation.tudelft.nl">
 * www.simulation.tudelft.nl </a> <br>
 * License of use: <a href="http://www.gnu.org/copyleft/lesser.html">Lesser
 * General Public License (LGPL) </a>, no warranty.
 * 
 * @author <a href="https://www.linkedin.com/in/peterhmjacobs">Peter Jacobs</a>
 * @version 1.3
 * @since 1.5
 */
public class JVMContextFactory implements InitialContextFactory
{
	/** context refers to the static JVMContext. */
	private static JVMContext context = null;

	/** {@inheritDoc} */
	@Override
	public synchronized Context
		getInitialContext( final Hashtable<?, ?> environment )
	{
		if( JVMContextFactory.context == null )
		{
			environment.remove( Context.INITIAL_CONTEXT_FACTORY );
			if( environment.size() != 0 )
			{
				Logger.warning( this, "getInitialContext",
						"unused environment variables in jndi.properties: "
								+ environment );
			}
			JVMContextFactory.context = new JVMContext();
		}
		return context;
	}
}
