package io.coala.time;

/**
 * {@link NanoInstant}
 * 
 * @version $Id$
 * @author Rick van Krevelen
 */
@Deprecated
public class NanoInstant extends AbstractInstant<NanoInstant>
{

	/** the serialVersionUID */
	private static final long serialVersionUID = 1L;

	public static NanoInstant ZERO = new NanoInstant( 0 );

	public NanoInstant( final Number value )
	{
		super( null, value, TimeUnit.NANOS );
	}

	@Override
	public NanoInstant plus( final Number value )
	{
		return new NanoInstant(
				this.getValue().doubleValue() + value.doubleValue() );
	}

	@Override
	public NanoInstant toUnit( TimeUnit unit )
	{
		throw new IllegalArgumentException();
	}

}
