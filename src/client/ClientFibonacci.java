package client;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import fibonacci_tasks.FibonacciReadyTask;
import api.Client;
import api.Server;
import api.Task;
import result.ValueResult;

/**
 * ClientFibonacci is the client to submit Fibonacci job to calculate F(N). It
 * takes Space's domain name as its argument.
 */
public class ClientFibonacci extends Client<Integer, Integer> {
	private static final long serialVersionUID = -6190190465235435463L;

	/**
	 * Constructor of Client Fibonacci.
	 * 
	 * @param clientName
	 *            Client Name
	 */
	public ClientFibonacci(String clientName) {
		super("Fibonacci", clientName);
	}

	/**
	 * Prepare a Fibonacci Ready Task
	 * 
	 * @param N
	 *            Fibonacci Number
	 * @return Fibonacci Ready Task
	 */
	@Override
	public Task<Integer> makeTask(Integer N) {
		ArrayList<Integer> arg = new ArrayList<Integer>();
		arg.add(N);
		return new FibonacciReadyTask(arg);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		System.setSecurityManager(new SecurityManager());

		String serverDomainName = args.length == 0 ? "localhost" : args[0];
		ClientFibonacci client = new ClientFibonacci("F");
		client.begin();
		
		Server server = null;
		try {
			server = client.findServer(serverDomainName);
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Server domain name!");
		} catch (RemoteException e) {
			System.out.println("Cannot connect to the Server!");
			return;
		}
		try {
			if (server.register(client.getName(), null)) {
				int N = 20;
				for (int i = 0; i < 1; i++) {
					Task<Integer> fibTask = client.makeTask(N);
					String taskID = server.submit(fibTask, client.getName());
					Logger.getLogger(Client.class.getCanonicalName()).log(
							Level.INFO, "Task: F({0}) is submitted. ID is {1}",
							new Object[] { N, taskID });
					ValueResult<Integer> result = (ValueResult<Integer>) server
							.getResult(client.getName());
					Logger.getLogger(Client.class.getCanonicalName()).log(
							Level.INFO, "Result: F({0}) is {1}",
							new Object[] { N, result.getResultValue() });
				}
				server.unregister(client.getName());
			} else {
				System.out.println("Failed to register in Server");
			}
		} catch (RemoteException e) {
			System.out.println("Cannot connect to the Server!");
		}
		client.end();
	}

}
