package server;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import api.Result;
import api.Server;
import api.Task;
import api.Universe;
import config.Config;

public class ServerImpl extends UnicastRemoteObject implements Server {
	private static final long serialVersionUID = -7458792337176706359L;

	/**
	 * Server itself. One Server in this JVM.
	 */
	private static ServerImpl server;

	/**
	 * Server ID. Assigned from Universe
	 */
	private int ID;

	/**
	 * Task ID. Assigned from Server.
	 */
	private static final AtomicInteger TaskID = new AtomicInteger();

	/**
	 * Ready Task Queue. Take Tasks from Client and to be taken by Universe
	 */
	private final BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Client Proxies Map. All registered Clients.
	 */
	private final Map<String, ClientProxy> clientProxies;

	/**
	 * Constructor of Server Implementation. Register to the Universe.
	 * 
	 * @param universeDomainName
	 *            Universe Domain Name
	 * @throws NotBoundException
	 *             Bad Universe Domain Name.
	 * @throws MalformedURLException
	 *             Bad Universe Domain Name.
	 * @throws RemoteException
	 *             Cannot connect to the Universe.
	 */
	public ServerImpl(final String universeDomainName)
			throws NotBoundException, MalformedURLException, RemoteException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		clientProxies = Collections.synchronizedMap(new HashMap<>());

		String url = "rmi://" + universeDomainName + ":" + Universe.PORT + "/"
				+ Universe.SERVICE_NAME;
		Universe universe;
		universe = (Universe) Naming.lookup(url);
		universe.register(this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Server {0} started.", ID);
	}

	public static void main(final String[] args) {
		String universeDomainName = args.length == 0 ? "localhost" : args[0];
		System.setSecurityManager(new SecurityManager());
		// Instantiate the Server.
		try {
			server = new ServerImpl(universeDomainName);
		} catch (RemoteException e) {
			System.out.println("Cannot register to the Universe!");
			return;
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Universe domain name!");
			return;
		}
		try {
			LocateRegistry.createRegistry(Server.PORT).rebind(
					Server.SERVICE_NAME, server);
		} catch (RemoteException e) {
			System.out.println("Fail to bind Server!");
			return;
		}

		// Main thread waiting for Key Enter to terminate.
		try {
			System.in.read();
		} catch (Throwable ignored) {

		}
		System.out.println("Server stopped.\n");
		System.exit(-1);
	}

	/**
	 * Set an ID to the Server. Call from Universe.
	 * 
	 * @param serverID
	 *            Server Id.
	 * @throws RemoteException
	 *             Failed to connect to Server.
	 */
	@Override
	public void setID(int serverID) throws RemoteException {
		this.ID = serverID;
	}

	/**
	 * Generate a Task ID.
	 * 
	 * @return Task ID.
	 */
	private int makeTaskID() {
		return TaskID.incrementAndGet();
	}

	/**
	 * Add a Task to Ready Task Queue.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	private void addTask(Task<?> task) {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			System.out
					.println("Interrupted when adding task to ready task queue!");
		}
	}

	/**
	 * Get a Task from Ready Task Queue. Call from Server Proxy in Universe.
	 * 
	 * @return Task
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	@Override
	public Task<?> getTask() throws RemoteException {
		try {
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			System.out
					.println("Server: Interrupted when taking task from Ready Task Queue");
		}
		return null;
	}

	/**
	 * Dispatch the Result to corresponding Client Proxy. Call from Server Proxy
	 * in Universe. If the Client is down, discard the result.
	 * F:1:S1:123:U1:P1:23:C1:W323
	 * 
	 * @param result
	 *            Result to be dispatched.
	 * @throws RemoteExcemption
	 *             Cannot connect with Server.
	 */
	@Override
	public void dispatchResult(final Result result) throws RemoteException {
		if (Config.DEBUG)
			System.out.println("Server wants to dispatch Result " + result.getID() + " to clientProxies.");
		String resultID[] = result.getID().split(":");
		String clientID = resultID[0];
		if (clientProxies.containsKey(clientID)) {
			clientProxies.get(clientID).addResult(result);
		}
	}

	/**
	 * Register a Client in Server. Call from Client.
	 * 
	 * @param client
	 *            Client to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	@Override
	public boolean register(final String clientName, final String duration)
			throws RemoteException {
		if (clientName == null || clientName.contains("!")
				|| clientName.contains("$")
				|| clientName.contains(":")
				|| clientProxies.containsKey(clientName)) {
			System.out.println("Client Name is invalid!");
			return false;
		}
		int timelimit;
		if (duration == null) {
			timelimit = Config.ClientTimeDefault;
		} else {
			String time[] = duration.split(":");
			if (time.length == 1) {
				timelimit = Integer.parseInt(time[0]);
			} else {
				timelimit = Integer.parseInt(time[0]) * 60
						+ Integer.parseInt(time[1]);
			}
		}
		if (timelimit > Config.ClientTimeLimit) {
			return false;
		}
		final ClientProxy clientProxy = new ClientProxy(clientName, timelimit);
		clientProxies.put(clientName, clientProxy);
		clientProxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Client {0} starts!", clientName);
		return true;
	}

	/**
	 * Unregister a Client in Server. Remove submitted Tasks in the Ready Task
	 * Queue.
	 * 
	 * @param clientProxy
	 *            Associated Cient Proxy.
	 */
	private void unregister(ClientProxy clientProxy) {
		clientProxies.remove(clientProxy.name);
		synchronized (readyTaskQueue) {
			for (Task<?> task : readyTaskQueue) {
				String taskID[] = task.getID().split(":");
				if (taskID[1].equals(clientProxy.name)) {
					readyTaskQueue.remove(task);
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Client {0} is down!", clientProxy.name);
	}

	@Override
	public boolean unregister(String clientname) throws RemoteException {
		if (clientname == null || !clientProxies.containsKey(clientname)) {
			System.out.println("Client is not registered in the Server");
			return false;
		}
		clientProxies.remove(clientname).stop();
		System.out.println("Client is unregistered in the Server.");
		return true;
	}

	@Override
	public String submit(Task<?> task, String clientname)
			throws RemoteException {
		if (task == null) {
			System.out.println("This Task is unacceptable!");
			return null;
		}
		if (clientname == null || !clientProxies.containsKey(clientname)) {
			System.out.println("Client is not registered in the Server");
			return null;
		}
		return clientProxies.get(clientname).submitTask(task).substring(2);
	}

	@Override
	public Result getResult(String clientname) throws RemoteException {
		if (clientname == null || !clientProxies.containsKey(clientname)) {
			System.out.println("Client is not registered in the Server");
			return null;
		}
		return clientProxies.get(clientname).getResult();
	}

	/**
	 * 
	 * A Client Proxy is to manage its associated Client. It takes Tasks from
	 * the Client and retrieves Results to the Client.
	 *
	 */
	private class ClientProxy {
		/**
		 * Client associated with the Client Proxy
		 */
		private final String name;

		/**
		 * Client Task ID
		 */
		private int taskID;

		/**
		 * Result Queue
		 */
		private final LinkedBlockingQueue<Result> resultQueue;

		/**
		 * Max run time.
		 */
		private final int timeLimit;

		/**
		 * Client Proxy start time.
		 */
		private long startTime;

		/**
		 * Timer of the client
		 */
		private final Timer timer;

		/**
		 * Constructor of Client Proxy
		 * 
		 * @param client
		 *            Client to be associated with the Client Proxy.
		 * @param ID
		 *            Client Proxy ID
		 */
		public ClientProxy(String clientName, int timeLimit) {
			this.name = clientName;
			this.timeLimit = timeLimit;
			this.timer = new Timer();
			resultQueue = new LinkedBlockingQueue<Result>();
		}

		/**
		 * Start the timer.
		 */
		private void start() {
			startTime = System.currentTimeMillis();
			this.timer.start();
		}

		/**
		 * Start the timer.
		 */
		private double stop() {
			this.timer.interrupt();
			return getRunTime();
		}

		/**
		 * Make Task ID
		 * 
		 * @return Task ID
		 */
		private int makeTaskID() {
			return ++this.taskID;
		}

		/**
		 * Get run time.
		 */
		private double getRunTime() {
			double runTime = (System.currentTimeMillis() - startTime) / 1000 / 60;
			return runTime;
		}

		/**
		 * Add Result to Result Queue.
		 * 
		 * @param result
		 *            Result to be added.
		 */
		private void addResult(Result result) {
			try {
				resultQueue.put(result);
			} catch (InterruptedException e) {
				System.out.println("Interrupted when taking result!");
			}
		}

		/**
		 * Get Result from Result Queue.
		 * 
		 * @return Result
		 */
		private Result getResult() {
			try {
				return resultQueue.take();
			} catch (InterruptedException e) {
			}
			return null;
		}

		/**
		 * Assign Task ID and Target ID to the task to be added into Server's
		 * Ready Task Queue. TaskID is ServerID:ClientID:TaskID
		 * 
		 * @param task
		 *            Task to be submitted.
		 * @return Task ID
		 */
		private String submitTask(Task<?> task) {
			// Task ID Reset
			// !:F:1:S0:1
			String taskID = this.name + ":" + makeTaskID() + ":S" + server.ID
					+ ":" + server.makeTaskID();
			task.setID("!:" + taskID);
			task.setTargetID("$:" + taskID);
			server.addTask(task);
			if (Config.DEBUG) {
				System.out.println("Server-Client Proxy: Task " + task.getID()
						+ " is added to Server ReadyTaskQueue.");
			}
			return task.getID();
		}

		private class Timer extends Thread {
			@Override
			public void run() {
				try {
					Thread.sleep(timeLimit * 60 * 1000);
				} catch (InterruptedException e) {
				} finally {
					unregister(ClientProxy.this);
				}
			}
		}

	}

}
