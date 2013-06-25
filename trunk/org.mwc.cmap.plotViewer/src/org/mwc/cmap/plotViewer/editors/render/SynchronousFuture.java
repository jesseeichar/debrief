package org.mwc.cmap.plotViewer.editors.render;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes the task on construction and is always ready.
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
