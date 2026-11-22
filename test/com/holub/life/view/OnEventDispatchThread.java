package com.holub.life.view;

import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.extension.*;

/** JUnit 5 extension: runs each test method on the Swing event-dispatch
 *  thread (EDT), the thread the real application uses for the board and
 *  its view. {@link UniversePanel} refuses notifications from any other
 *  thread, so a test that drives a board with a panel attached needs
 *  this. Use it with {@code @ExtendWith(OnEventDispatchThread.class)}.
 *
 *  <p>Assertion failures and exceptions thrown on the EDT are rethrown on
 *  the test thread, so JUnit reports them as usual.
 */
public final class OnEventDispatchThread implements InvocationInterceptor
{
	@Override public void interceptTestMethod(Invocation<Void> invocation,
											  ReflectiveInvocationContext<Method> invocationContext,
											  ExtensionContext extensionContext) throws Throwable
	{
		Throwable[] failure = new Throwable[1];
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{	invocation.proceed();
			}
			catch (Throwable thrown)
			{	failure[0] = thrown;
			}
		});
		if (failure[0] != null)
			throw failure[0];
	}
}
