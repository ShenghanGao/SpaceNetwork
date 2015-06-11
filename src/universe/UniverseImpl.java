package universe;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import result.ValueResult;
import api.Result;
import api.Server;
import api.Space;
import api.Task;
import api.Universe;
import config.Config;

/**
 * Universe implementation
 * 
 * @author user
 *
 */
public class UniverseImpl extends UnicastRemoteObject implements Universe,
		Serializable {
	private static final long serialVersionUID = -5110211125190845128L;
	private static UniverseImpl universe;
	private static String recoveryFileName = "recovery.bk";

	/**
	 * Space Id.
	 */
	private AtomicInteger SpaceID = new AtomicInteger();

	/**
	 * Space Num.
	 */
	private AtomicInteger SpaceNum = new AtomicInteger();

	/**
	 * Server Id.
	 */
	private AtomicInteger ServerID = new AtomicInteger();

	/**
	 * Check Status Thread
	 */
	private CheckStatus checkStatus;

	/**
	 * Ready Task Queue. Containing tasks ready to run.
	 */
	private final BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Successor Task Map. Containing successor tasks waiting for arguments.
	 */
	private final Map<String, Task<?>> successorTaskMap;

	/**
	 * Server Proxies Map. Containing all registered Server Proxy with
	 * associated Server.
	 */
	private final Map<Integer, ServerProxy> serverProxies;

	/**
	 * Space Proxies Map. Containing all registered Space Proxy with associated
	 * Space.
	 */
	private final Map<Integer, SpaceProxy> spaceProxies;

	/**
	 * Normal Mode Constructor.
	 * 
	 * @throws RemoteException
	 */
	public UniverseImpl() throws RemoteException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		successorTaskMap = Collections.synchronizedMap(new HashMap<>());
		serverProxies = Collections.synchronizedMap(new HashMap<>());
		spaceProxies = Collections.synchronizedMap(new HashMap<>());
		checkStatus = new CheckStatus();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Universe started.");
	}

	/**
	 * Check Status Thread
	 *
	 */
	private class CheckStatus extends Thread implements Serializable {
		private static final long serialVersionUID = 5128515434969596899L;

		@Override
		public void run() {
			while (true) {
				@SuppressWarnings("resource")
				Scanner scanner = new Scanner(System.in);
				if (scanner.next().equals("check")) {
					check();
				}
			}
		}
	}

	/**
	 * Recovery Mode Constructor
	 * 
	 * @param recoveryFileName
	 *            Recovery File name
	 * @throws RemoteException
	 */
	public UniverseImpl(String recoveryFileName) throws RemoteException {
		System.out.println("Universe is recovering...");
		UniverseImpl readUniverse = null;
		ObjectInputStream objectinputstream = null;
		try {
			objectinputstream = new ObjectInputStream(new FileInputStream(
					recoveryFileName));
			readUniverse = (UniverseImpl) objectinputstream.readObject();
		} catch (Exception e) {
			System.out.println("Universe failed to recover. Relaunching...");
			readyTaskQueue = new LinkedBlockingQueue<>();
			successorTaskMap = Collections.synchronizedMap(new HashMap<>());
			serverProxies = Collections.synchronizedMap(new HashMap<>());
			spaceProxies = Collections.synchronizedMap(new HashMap<>());
			Logger.getLogger(this.getClass().getName()).log(Level.INFO,
					"Universe started.");
			return;
		}
		readyTaskQueue = readUniverse.readyTaskQueue;
		successorTaskMap = readUniverse.successorTaskMap;
		serverProxies = readUniverse.serverProxies;
		spaceProxies = readUniverse.spaceProxies;
		checkStatus = new CheckStatus();

		SpaceID = readUniverse.SpaceID;
		SpaceNum = readUniverse.SpaceNum;
		ServerID = readUniverse.ServerID;
		for (int i : spaceProxies.keySet()) {
			if (!spaceProxies.get(i).runningTaskMap.isEmpty()) {
				for (String taskID : spaceProxies.get(i).runningTaskMap
						.keySet()) {
					Task<?> task = spaceProxies.get(i).runningTaskMap
							.get(taskID);
					addReadyTask(task);
				}
			}
		}
		System.out.println("readyTaskQueue  :" + readyTaskQueue.size());
		System.out.println("successor map :" + successorTaskMap.size());
		System.out.println("spaceProxies map :" + spaceProxies.size());

		try {
			objectinputstream.close();
		} catch (IOException e) {
			System.out.println("IO Exception!");
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Universe recovered.");
	}

	public static void main(final String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		universe = args.length == 0 ? new UniverseImpl() : new UniverseImpl(
				recoveryFileName);
		for (int i : universe.serverProxies.keySet()) {
			universe.serverProxies.get(i).restart();
		}
		for (int i : universe.spaceProxies.keySet()) {
			universe.spaceProxies.get(i).restart();
		}
		LocateRegistry.createRegistry(Universe.PORT).rebind(
				Universe.SERVICE_NAME, universe);
		universe.checkStatus.start();
		while (true) {
			Thread.sleep(10000);
			universe.checkPoint();
		}
	}

	/**
	 * Take Checkpoint
	 */
	private void checkPoint() {
		try {
			FileOutputStream fout = new FileOutputStream(recoveryFileName);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			synchronized (readyTaskQueue) {
				synchronized (successorTaskMap) {
					oos.writeObject(this);
				}
			}
			oos.close();
			Logger.getLogger(this.getClass().getName()).log(Level.INFO,
					"Checkpoint is taken.");
		} catch (Exception ex) {
			System.out.println("Check point failed!");
		}
	}

	/**
	 * Add a Task to Ready Task Queue. Call from Result.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	public void addReadyTask(Task<?> task) {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			System.out.println("Interrupted!");
		}
	}

	/**
	 * Add a Successor Task to Successor Task Map. Call from Result.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	public void addSuccessorTask(Task<?> task) {
		successorTaskMap.put(task.getID(), task);
	}

	/**
	 * Get a Task from the Ready Task Queue.
	 * 
	 * @return Task
	 */
	private Task<?> getReadyTask() {
		return readyTaskQueue.poll();
	}

	/**
	 * Get a task from the Successor Task Map with Task Id. Call from Result.
	 * 
	 * @param TaskId
	 *            Task Id.
	 * @return A Successor Task.
	 */
	public Task<?> getSuccessorTask(String TaskId) {
		return successorTaskMap.get(TaskId);
	}

	/**
	 * 
	 * Remove a successor task from Successor Task Map and put it into Ready
	 * Task Queue, when this successor task has all needed arguments and ready
	 * to run.
	 * 
	 * @param successortask
	 *            The ready-to-run successor task.
	 */
	public void successorToReady(Task<?> successortask) {
		successorTaskMap.remove(successortask.getID());
		try {
			readyTaskQueue.put(successortask);
		} catch (InterruptedException e) {
			System.out.println("Interrupted!");
		}
	}

	/**
	 * Dispatch the Result to corresponding Server Proxy. If the Server is down,
	 * discard the result. F:1:S1:123:U1:P1:23:C1:W323
	 * 
	 * @param result
	 *            Result to be dispatched.
	 */
	public void dispatchResult(final Result result) {
		String resultID[] = ((ValueResult<?>) result).getTargetTaskID().split(
				":");
		int serverID = Integer.parseInt(resultID[3].substring(1));
		System.out.println("Universe sends final Result " + result.getID()
				+ " to server " + serverID);
		synchronized (serverProxies) {
			if (serverProxies.containsKey(serverID)) {
				serverProxies.get(serverID).addResult(result);
			}
		}
	}

	/**
	 * Register a Server in Universe. Call from Server.
	 * 
	 * @param server
	 *            Server to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Universe.
	 */
	@Override
	public void register(Server server) throws RemoteException {
		final ServerProxy serverProxy = new ServerProxy(server,
				ServerID.getAndIncrement());
		server.setID(serverProxy.ID);
		serverProxies.put(serverProxy.ID, serverProxy);
		serverProxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Server {0} started!", serverProxy.ID);
	}

	/**
	 * Unregister a Server. Remove submitted Tasks in the Ready Task Queue.
	 * F:1:S0:2:U2:P0:2:C1:W177
	 * 
	 * @param serverProxy
	 *            Server associated ServerProxy to be unregistered.
	 */
	private void unregister(ServerProxy serverProxy) {
		serverProxies.remove(serverProxy.ID);
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Server {0} is down.", serverProxy.ID);
	}

	/**
	 * Register a Space in Universe. Call from Space.
	 * 
	 * @param Space
	 *            Space to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Universe.
	 */
	@Override
	public void register(Space space) throws RemoteException {
		final SpaceProxy spaceProxy = new SpaceProxy(space,
				SpaceID.getAndIncrement());
		space.setID(spaceProxy.ID);
		SpaceNum.incrementAndGet();
		spaceProxies.put(spaceProxy.ID, spaceProxy);
		spaceProxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Space {0} started!", spaceProxy.ID);
	}

	/**
	 * Unregister a Space and remove its associated Space Proxy. Processing all
	 * unfinished Value Results. Save all the Space's unfinished running tasks
	 * into Universe Ready Task Queue.
	 * 
	 * @param spaceProxy
	 *            Space associated Space Proxy
	 */
	private void unregister(SpaceProxy spaceProxy) {
		spaceProxies.remove(spaceProxy.ID);
		synchronized (readyTaskQueue) {
			synchronized (spaceProxy.runningTaskMap) {
				if (!spaceProxy.runningTaskMap.isEmpty()) {
					System.out.println("running task map size"
							+ spaceProxy.runningTaskMap.size());
					for (String taskID : spaceProxy.runningTaskMap.keySet()) {
						Task<?> task = spaceProxy.runningTaskMap.get(taskID);
						addReadyTask(task);
						if (Config.STATUSOUTPUT || Config.DEBUG) {
							System.out.println("Save Space Task:" + taskID
									+ " " + task.getLayer() + " "
									+ task.isCoarse() + " "
									+ task.getArg().get(0));
						}
					}
				}
			}
		}
		SpaceNum.decrementAndGet();
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Space {0} is down.", spaceProxy.ID);
	}

	/**
	 * Check all Spaces and Servers Status
	 */
	private static void check() {
		for (int i : universe.serverProxies.keySet()) {
			ArrayList<String> status;
			try {
				status = universe.serverProxies.get(i).server.check();
			} catch (Exception e) {
				continue;
			}
			System.out.println("Server " + i + ": " + status.get(0)
					+ " clients, total " + status.get(1) + " tasks.");
			for (int j = 2; j < status.size(); j++) {
				String[] client = status.get(j).split(":");
				System.out.println("         " + "Client " + client[0]
						+ " submitted " + client[1] + " jobs.");
			}
		}
		int totalTask = 0;
		for (int i : universe.spaceProxies.keySet()) {
			ArrayList<String> status;
			try {
				status = universe.spaceProxies.get(i).space.check();
			} catch (Exception e) {
				continue;
			}
			totalTask += Integer.parseInt(status.get(1));
			System.out.println("Space " + i + ": " + status.get(0)
					+ " computers, total " + status.get(1) + " tasks.");
			for (int j = 2; j < status.size(); j++) {
				String[] computer = status.get(j).split(":");
				System.out.println("         " + "Computer " + computer[0]
						+ " with " + computer[1] + " workers, " + computer[2]
						+ " tasks.");
			}
		}
		System.out.println("Total " + totalTask + " tasks.");
	}

	/**
	 * Server proxy to manage server
	 *
	 */
	private class ServerProxy implements Serializable {
		private static final long serialVersionUID = -6762820061270809812L;
		/**
		 * Associated Server
		 */
		private final Server server;

		/**
		 * Server ID
		 */
		private final int ID;

		/**
		 * Result Queue.
		 */
		private final BlockingQueue<Result> resultQueue;

		/**
		 * Send Service
		 */
		private SendService sendService;

		/**
		 * Remote Exception Flag
		 */
		private boolean isInterrupt;

		/**
		 * Receive Service
		 */
		private ReceiveService receiveService;

		private ServerProxy(Server server, int id) {
			this.server = server;
			this.ID = id;
			isInterrupt = false;
			this.resultQueue = new LinkedBlockingQueue<>();
			receiveService = new ReceiveService();
			sendService = new SendService();
		}

		/**
		 * Start Receive Service thread and Send Service thread
		 */
		private void start() {
			receiveService.start();
			sendService.start();
		}

		@SuppressWarnings("deprecation")
		private void restart() {
			isInterrupt = false;
			receiveService.interrupt();
			receiveService.stop();
			receiveService = null;
			sendService.interrupt();
			sendService.stop();
			sendService = null;
			receiveService = new ReceiveService();
			sendService = new SendService();
			start();
		}

		/**
		 * Add a Result to Result Queue.
		 * 
		 * @param result
		 *            Result to be added.
		 */
		private void addResult(Result result) {
			try {
				resultQueue.put(result);
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
			}
		}

		/**
		 * Get a Result from Result Queue.
		 * 
		 * @return Result
		 */
		private Result getResult() {
			try {
				return resultQueue.take();
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
			}
			return null;
		}

		/**
		 * Receive Service.
		 *
		 */
		private class ReceiveService extends Thread implements Serializable {
			private static final long serialVersionUID = 7243273067156782355L;

			@Override
			public void run() {
				while (!isInterrupt) {
					Result result = getResult();
					try {
						server.dispatchResult(result);
					} catch (RemoteException e) {
						System.out.println("Receive Service: Server " + ID
								+ " is Down!");
						return;
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting tasks from Client to the Server
		 * Ready Task Queue.
		 *
		 */
		private class SendService extends Thread implements Serializable {
			private static final long serialVersionUID = -3664570163433948598L;

			@Override
			public void run() {
				while (true) {
					Task<?> task = null;
					try {
						task = server.getTask();
						if (task == null) {
							try {
								Thread.sleep(1000);
							} catch (Exception e) {
							}
							continue;
						}
						System.out.println("Take task from server: "
								+ task.getID());
					} catch (RemoteException e) {
						System.out.println("Send Service: Server " + ID
								+ " is Down!");
						isInterrupt = true;
						if (ServerProxy.this.receiveService.isAlive()) {
							ServerProxy.this.receiveService.interrupt();
						}
						try {
							receiveService.join();
						} catch (InterruptedException e1) {
							System.out.println("Interrupted!");
						}
						unregister(ServerProxy.this);
						return;
					}

					synchronized (universe.readyTaskQueue) {
						universe.addReadyTask(task);
					}

				}
			}
		}
	}

	/**
	 * Space Proxy
	 *
	 */
	private class SpaceProxy implements Serializable {
		private static final long serialVersionUID = 533389829029728826L;

		/**
		 * Associated Space.
		 */
		private final Space space;

		/**
		 * Space ID.
		 */
		private final int ID;

		/**
		 * Task Count
		 */
		private int taskCount;

		/**
		 * Busy Flag
		 */
		private volatile boolean isBusy;

		/**
		 * Running Task Map. The tasks that Space is running.
		 */
		private Map<String, Task<?>> runningTaskMap;

		/**
		 * Send Service
		 */
		private SendService sendService;

		/**
		 * Remote Exception flag
		 */
		private boolean isInterrupt;

		/**
		 * Receive Service
		 */
		private ReceiveService receiveService;

		private SpaceProxy(Space space, int id) {
			this.space = space;
			this.ID = id;
			this.isInterrupt = false;
			this.isBusy = false;
			this.taskCount = 0;
			this.runningTaskMap = Collections.synchronizedMap(new HashMap<>());
			this.receiveService = new ReceiveService();
			this.sendService = new SendService();
		}

		@SuppressWarnings("deprecation")
		public void restart() {
			System.out.println("space proxy restart");
			this.isInterrupt = false;
			this.isBusy = false;
			this.taskCount = 0;
			receiveService.interrupt();
			receiveService.stop();
			receiveService = null;
			sendService.interrupt();
			sendService.stop();
			sendService = null;
			this.runningTaskMap = Collections.synchronizedMap(new HashMap<>());
			try {
				space.restart();
			} catch (RemoteException e) {
				System.out.println("Space failed to restart!");
				unregister(this);
				return;
			}
			this.receiveService = new ReceiveService();
			this.sendService = new SendService();
			start();
		}

		/**
		 * Start Receive Service thread and Send Service thread
		 */
		private void start() {
			receiveService.start();
			sendService.start();
		}

		private class ReceiveService extends Thread implements Serializable {
			private static final long serialVersionUID = -855043841330311213L;

			@Override
			public void run() {
				while (true) {
					Result result = null;
					try {
						result = space.getResult();
					} catch (RemoteException e) {
						System.out.println("Receive Servcie: Space " + ID
								+ " is Down!");
						isInterrupt = true;
						try {
							sendService.join();
						} catch (InterruptedException e1) {
							System.out.println("Interrupted!");
						}
						unregister(SpaceProxy.this);
						return;
					}
					taskCount = result.getSpaceCount() > taskCount ? result
							.getSpaceCount() : taskCount;
					if (result.getSpaceIsBusy()) {
						isBusy = true;
					} else {
						isBusy = false;
					}
					synchronized (universe.readyTaskQueue) {
						synchronized (universe.successorTaskMap) {
							synchronized (runningTaskMap) {
								result.process(universe, runningTaskMap);
								if (!result.isCoarse()) {
									runningTaskMap
											.remove(((ValueResult<?>) result)
													.getID());
								} else {
									runningTaskMap.remove(result.getID());
								}
							}
						}
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting tasks from Client to the Server
		 * Ready Task Queue.
		 *
		 */
		private class SendService extends Thread implements Serializable {
			private static final long serialVersionUID = 1499104632932748878L;
			@Override
			public void run() {
				int busyCount = 0;
				while (!isInterrupt) {
					Task<?> task = null;
					if (isBusy) {
						busyCount++;
						if (busyCount > 3) {
							busyCount = 0;
							isBusy = false;
						}
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							return;
						}
						continue;
					}
					synchronized (universe.readyTaskQueue) {
						task = universe.getReadyTask();
						if (task == null) {
							continue;
						}
						synchronized (runningTaskMap) {
							try {
								space.addTask(task);
							} catch (RemoteException e) {
								System.out.println("Send Service: Space " + ID
										+ " is Down!");
								universe.addReadyTask(task);
								return;
							}
							runningTaskMap.put(task.getID(), task);
						}
					}

				}
				System.out.println("Send Service: Space " + ID + " is Down!");
			}
		}
	}
}
