package com.holub.life.control;

import javax.swing.*;
import com.holub.tools.Publisher;

/***
 * The <code>Clock</code> class handles the timing of gameboard
 * updates and sends notifications off to any observers every time the
 * clock "ticks." Its speed menu is installed by {@link ClockMenu}.
 *
 * <p>Ticks are delivered on the Swing event-dispatch thread
 * (javax.swing.Timer), the same thread that handles clicks and
 * painting, so the board needs no locking.
 *
 * <h2>Revisions</h2>
 * <p>
 * 12-8-2004	AIH	Added a kludge to the clock-tick handler that
 * 				checks whether any menu item is active before it
 * 				allows the clock to tick. This mod fixes a bug
 * 				that caused the running game to overwrite any
 * 				displayed menus. See {@link #menuIsActive} for
 * 				details.
 *
 * @include /etc/license.txt
 */

public class Clock
{	private final Timer clock = new Timer( 0, e -> tick() );

	public Clock()
	{	clock.setCoalesce( true );		// drop ticks we can't keep up with
	}

	/** Start up the clock.
	 *  @param millisecondsBetweenTicks The number of milliseconds between
	 *  					 ticks. A value of 0 indicates that
	 *  					 the clock should be stopped.
	 */

	public void startTicking( int millisecondsBetweenTicks )
	{	clock.stop();

		if( millisecondsBetweenTicks > 0 )
		{	clock.setInitialDelay( 0 );
			clock.setDelay( millisecondsBetweenTicks );
			clock.start();
		}
	}

	/** Stop the clock
	 */

	public void stop()
	{	startTicking( 0 );
	}

	boolean isRunning()
	{	return clock.isRunning();
	}

	int getTickDelay()
	{	return clock.getDelay();
	}

	/** Stop automatic ticks and advance exactly one generation. */
	void stepOnce()
	{	stop();
		publishTick();
	}

	private Publisher publisher = new Publisher();

	/** Add a listener that's notified every time the clock ticks:
	 *  <PRE>
	 *  clock.addClockListener
	 *  (	new Clock.Listener()
	 *  	{	public void tick()
	 *  		{	System.out.println("tick!");
	 *  		}
   	 *		}
	 *  );
	 *  </PRE>
	 */
	public void addClockListener( Listener observer )
	{	publisher.subscribe(observer);
	}

	/** Implement this interface to be notified about clock ticks.
	 *  @see Clock
	 */
	public interface Listener
	{	void tick();
	}

	/** Advance one tick unless a menu is open. */
	public void tick()
	{	if( !menuIsActive() )
			publishTick();
	}

	private void publishTick()
	{	publisher.publish( subscriber -> ((Listener)subscriber).tick() );
	}

	/** Check if any item on the menu bar has been selected.
	 *  This is an incredible kluge. The problem is that Swing draws the
	 *  menus on the same "canvas" as the main frame. As a consequence,
	 *  displayed menus are overwritten by a running game. Moreover, Swing
	 *  provides no notification on the order of "some menu item has been
	 *  selected," so the only way to detect menu-bar activity is to poll
	 *  it. This method does just that, and returns true if some menu
	 *  is being displayed. The {@link #tick} method effectively suspends
	 *  clock ticks as long as a menu is displayed, (which slows down
	 *  the clock, unfortunately).
	 *  <p>
	 *  From a design-patterns perspective, this method demonstrates that
	 *  a facade (the MenuSite) does not provide strict isolation from
	 *  the underlying subsystem. I can't immagine another application
	 *  that would need to know whether or not the menu bar is active,
	 *  so adding this menu to the MenuSite would be "noise." Since
	 *  the method does something that's of no relevance to other
	 *  MenuSite users, it makes an "end run" around the facade.
	 */

	private boolean menuIsActive()
	{	MenuElement[] path =
					MenuSelectionManager.defaultManager().getSelectedPath();
		return ( path != null && path.length > 0 );
	}
}
