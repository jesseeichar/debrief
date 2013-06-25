package org.mwc.cmap.plotViewer.editors.render;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps a future that is responsible for rendering a layer. and keeps track
 * whether or not that image has been previously accessed.
 * 
 * @author Jesse
 */
public class RenderFuture
{
	private final Future<RenderTaskResult> _delegate;
	private final AtomicBoolean _displayed = new AtomicBoolean(false);
	private boolean _disposed = false;

	public RenderFuture(Future<RenderTaskResult> _delegate)
	{
		this._delegate = _delegate;
	}

	/**
	 * Check if future is done.
	 */
	public boolean isDone()
	{
		return _delegate.isDone();
	}

	/**
	 * Check if future has been cancelled.
	 */
	public boolean isCancelled()
	{
		return _delegate.isCancelled();
	}

	/**
	 * See if the image has been previously obtained via {@link #get()}.
	 */
	public boolean hasBeenDisplayed()
	{
		return this._displayed.get();
	}

	/**
	 * Get image if done or block waiting.
	 */
	public synchronized RenderTaskResult get() throws InterruptedException,
			ExecutionException
	{
		RenderTaskResult result = _delegate.get();
		DebugLogger.log("Retrieving result: " + result);
		_displayed.set(true);
		return result;
	}

	/**
	 * Cancel image rendering.
	 */
	public void cancel(boolean mayInterruptIfRunning)
	{
		_delegate.cancel(mayInterruptIfRunning);
	}

	public synchronized void dispose()
	{
		if (!this._disposed && isDone())
		{
			_disposed = true;
			try
			{
				_delegate.get().dispose();
			}
			catch (InterruptedException e)
			{
				// its fine
			}
			catch (ExecutionException e)
			{
				e.printStackTrace();
			}
			catch (CancellationException e)
			{
				// its fine
			}
		}
	}
}
