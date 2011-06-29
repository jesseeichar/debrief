package org.mwc.asset.comms.kryo;

import java.io.IOException;
import java.net.InetAddress;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class AClient implements ASpecs
{
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException
	{
		// try to find a server
		final Client client = new Client();
		client.start();
		InetAddress address = client.discoverHost(UDP_PORT, 2000);
		System.out.println(address);

		if (address == null)
			System.exit(1);

		// did it work?
		client.connect(5000, address.getHostAddress(), TCP_PORT, UDP_PORT);

		ASpecs.Config.init(client.getKryo());

		client.addListener(new Listener()
		{
			public void received(Connection connection, Object object)
			{
				System.out.println("received:" + object);
				if (object instanceof ScenarioList)
				{
					ScenarioList response = (ScenarioList) object;
					System.out.println("rx:" + response.scenarios.size());
					client.stop();
				}
			}
		});

		for (int i = 0; i < 5; i++)
		{
			GetScenarios request = new GetScenarios();
			client.sendTCP(request);
		}

	}
}
