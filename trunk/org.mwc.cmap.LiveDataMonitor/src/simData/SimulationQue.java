/**
 * 
 */
package simData;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Vector;

/**
 * maintain a que of simulations
 * 
 * @author ianmayo
 * 
 */
public class SimulationQue
{

	/**
	 * the que we manage
	 * 
	 */
	Vector<ISimulation> _mySimulations;

	/**
	 * the thread that fires off the simulations
	 * 
	 */
	private runThread runThread;

	public SimulationQue(Vector<ISimulation> simulations)
	{
		_mySimulations = simulations;
	}

	/**
	 * access my collection of simulations
	 * 
	 * @return
	 */
	public Vector<ISimulation> getSimulations()
	{
		return _mySimulations;
	}

	/**
	 * start the que running
	 * 
	 */
	public void startQue()
	{
		runThread = new runThread();
		runThread.start();
	}

	/**
	 * whether the que is running
	 * 
	 * @return yes/no
	 */
	public boolean isRunning()
	{
		return runThread.isAlive();
	}

	/**
	 * stop the que
	 * 
	 */
	public void stopQue()
	{
		runThread.interrupt();
	}

	/**
	 * class that handles running the simulations
	 * 
	 */
	private class runThread extends Thread
	{

		@Override
		public void run()
		{
			boolean worthRunning = true;

			// is it worth us bothering?
			while (worthRunning)
			{
				Iterator<ISimulation> iter = _mySimulations.iterator();
				while (iter.hasNext())
				{
					worthRunning = false;

					// get the next simulation
					ISimulation thisS = iter.next();

					// what's its state?
					String thisState = (String) thisS.getState().getCurrent().getValue();

					// check the state
					if (thisState == MockSimulation.RUNNING)
					{
						worthRunning = true;
						// right there's one running, let's just leave it
						break;
					}
					else if (thisState == MockSimulation.WAITING)
					{
						worthRunning = true;
						// right this one's waiting to start - get it going
						thisS.start();

						// done. start from the beginning of the scenarios again
						break;
					}

				}

				// give ourselves a rest
				try
				{
					Thread.sleep(200);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}

	}

	private static class StateListener implements PropertyChangeListener
	{
		private ISimulation _thisSim;

		public StateListener(ISimulation thisSim)
		{
			_thisSim = thisSim;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			DataDoublet data = (DataDoublet) evt.getNewValue();
			System.out.println(_thisSim.getName() + " is now " + data.getValue());

		}

	}

	private static void dumpThis(IAttribute theAttribute)
	{
		System.out.println("================");
		Vector<DataDoublet> list = theAttribute.getHistoricValues();
		for (Iterator<DataDoublet> iterator = list.iterator(); iterator.hasNext();)
		{
			DataDoublet thisOne = iterator.next();
			if (thisOne != null)
				System.out.println(" at " + thisOne.getTime() + " value of "
						+ theAttribute.getName() + " is " + thisOne.getValue());
		}
	}

	/**
	 * sample implementation of simulation que
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		System.out.println("Working");

		long runTime = 6000;

		Vector<ISimulation> shortQue = new Vector<ISimulation>();
		for (int i = 0; i < 5; i++)
		{
			MockSimulation m1 = MockSimulation.createShort("sim_" + i, runTime);
			shortQue.add(m1);
			m1.getState().addPropertyChangeListener(new StateListener(m1));
		}

		SimulationQue que = new SimulationQue(shortQue);
		que.startQue();

		// wait until the simulation is complete
		while (que.isRunning())
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}

		// have a look at the data

		IAttribute theAttribute = shortQue.elementAt(1).getAttributes()
				.elementAt(1);
		dumpThis(theAttribute);
		theAttribute = shortQue.elementAt(0).getAttributes().elementAt(1);
		dumpThis(theAttribute);
	}

}