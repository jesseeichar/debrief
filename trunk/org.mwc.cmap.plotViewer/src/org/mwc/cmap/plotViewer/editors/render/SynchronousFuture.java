package org.mwc.cmap.plotViewer.editors.render;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A future that is already calculated before construction and therefore is always 
 * ready. This class is used when the task is known to be non-blocking (usually 
 * because it was rendered previously).
 * 
 * This can be used for tiles that were previously loaded.
 * 
 * @author Jesse
 */
public class SynchronousFuture implements Future<RenderTaskResult>
{

	private RenderTaskResult _result;

	public SynchronousFuture(AbstractRenderTask task)
	{
		try
		{
			this._result = task.call();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		return false;
	}

	@Override
	public boolean isCancelled()
	{
		return false;
	}

	@Override
	public boolean isDone()
	{
		return true;
	}

	@Override
	public RenderTaskResult get() throws InterruptedException, ExecutionException
	{
		return this._result;
	}

	@Override
	public RenderTaskResult get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		return get();
	}

}
