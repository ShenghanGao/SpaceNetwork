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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import result.ValueResult;
import api.Result;
import api.Server;
import api.Space;
import api.Task;
import api.Universe;
import config.Config;

public class UniverseImpl extends UnicastRemoteObject implements Universe,
		Serializable {
	private static final long serialVersionUID = -5110211125190845128L;
	private static UniverseImpl universe;
	private static String recoveryFileName = "recovery.bk";

	/**
	 * Space Id.
	 */
	private static final AtomicInteger SpaceID = new AtomicInteger();

	/**
	 * Server Id.
	 */
	private static final AtomicInteger ServerID = new AtomicInteger();

	/**
	 * Task ID.
	 */
	private static final AtomicInteger TaskID = new AtomicInteger();

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
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Universe started.");
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
		for (int i : serverProxies.keySet()) {
			serverProxies.get(i).start();
		}
		spaceProxies = readUniverse.spaceProxies;
		for (int i : spaceProxies.keySet()) {
			spaceProxies.get(i).start();
		}
		try {
			objectinputstream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Universe recovered.");
	}

	public static void main(final String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		universe = args.length == 0 ? new UniverseImpl() : new UniverseImpl(
				recoveryFileName);
		LocateRegistry.createRegistry(Universe.PORT).rebind(
				Universe.SERVICE_NAME, universe);
		// Take Checkpoint periodically
		while (true) {
			Thread.sleep(10000);
			// universe.checkPoint();
		}
	}

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
			ex.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
		if (Config.DEBUG)
			System.out.println("Universe wants to dispatch Result " + result.getID() + " to serverProxies.");
		String resultID[] = result.getID().split(":");
		int serverID = Integer.parseInt(resultID[2].substring(1));
		synchronized (serverProxies) {
			if (serverProxies.containsKey(serverID)) {
				serverProxies.get(serverID).addResult(result);
			}
		}
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
		synchronized (readyTaskQueue) {
			for (Task<?> task : readyTaskQueue) {
				String taskID[] = task.getID().split(":");
				if (taskID[2].substring(1).equals(serverProxy.ID)) {
					readyTaskQueue.remove(task);
				}
			}
		}
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
					for (String taskID : spaceProxy.runningTaskMap.keySet()) {
						Task<?> task = spaceProxy.runningTaskMap.get(taskID);
						addReadyTask(task);
						if (Config.STATUSOUTPUT || Config.DEBUG) {
							System.out.println("Save Space Task:" + taskID);
						}
					}
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Space {0} is down.", spaceProxy.ID);
	}

	private class ServerProxy {
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
		private final SendService sendService;

		/**
		 * Remote Exception Flag
		 */
		private boolean isInterrupt;

		/**
		 * Receive Service
		 */
		private final ReceiveService receiveService;

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
				e.printStackTrace();
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
				if (Config.DEBUG) {
					System.out.println("Receive Service is interrupted!");
				}
			}
			return null;
		}

		private class ReceiveService extends Thread {
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
						// Potential Problem here. Send Service tries to stop
						// Receive Service
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting tasks from Client to the Server
		 * Ready Task Queue.
		 *
		 */
		private class SendService extends Thread {
			@Override
			public void run() {
				while (true) {
					Task<?> task = null;
					try {
						task = server.getTask();
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
							e1.printStackTrace();
						}
						unregister(ServerProxy.this);
						return;
					}
					// Task ID Reset
					// !F:1:S0:1:U1
					task.setID(task.getID() + ":U" + makeTaskID());
					synchronized (universe.readyTaskQueue) {
						universe.addReadyTask(task);
						if (Config.DEBUG) {
							System.out.println("Universe-Server Proxy: Task "
									+ task.getID()
									+ " is added to Universe ReadyTaskQueue!");
						}
					}

				}
			}
		}
	}

	private class SpaceProxy {
		/**
		 * Associated Space.
		 */
		private final Space space;

		/**
		 * Space ID.
		 */
		private final int ID;

		/**
		 * Task ID.
		 */
		private final AtomicInteger TaskID = new AtomicInteger();

		/**
		 * Running Task Map. The tasks that Space is running.
		 */
		private final Map<String, Task<?>> runningTaskMap;

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
		private final ReceiveService receiveService;

		private SpaceProxy(Space space, int id) {
			this.space = space;
			this.ID = id;
			this.isInterrupt = false;
			this.runningTaskMap = Collections.synchronizedMap(new HashMap<>());
			this.receiveService = new ReceiveService();
			this.sendService = new SendService();
		}

		/**
		 * Start Receive Service thread and Send Service thread
		 */
		private void start() {
			receiveService.start();
			sendService.start();
		}

		/**
		 * Generate a Task ID.
		 * 
		 * @return Task ID.
		 */
		private int makeTaskID() {
			return TaskID.incrementAndGet();
		}

		private class ReceiveService extends Thread {
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
							e1.printStackTrace();
						}
						unregister(SpaceProxy.this);
						return;
					}
					// Result ID !:F:1:S0:1:U1:P1:1 same as Task ID
					synchronized (universe.readyTaskQueue) {
						synchronized (universe.successorTaskMap) {
							synchronized (runningTaskMap) {
								if (Config.DEBUG) {
									System.out
											.println("Universe-Space Proxy: Result "
													+ result.getID()
													+ "-"
													+ result.isCoarse()
													+ " is processing!");
								}
								result.process(universe, runningTaskMap);
								if (!result.isCoarse()) {
									runningTaskMap
											.remove(((ValueResult<?>) result)
													.getOrginTaskID());
									if (Config.DEBUG) {
										System.out.println("Orgin Task ID"
												+ ((ValueResult<?>) result)
														.getOrginTaskID());
									}
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
		private class SendService extends Thread {
			@Override
			public void run() {
				while (!isInterrupt) {
					Task<?> task = null;
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						return;
					}
					synchronized (universe.readyTaskQueue) {
						task = universe.getReadyTask();
						if (task == null) {
							continue;
						}
						// Task ID Reset
						// Space Task Tracking
						// !:F:1:S0:1:U1:P1:1
						//   F:1:S0:1:U1:P0:1:C1:1:W1
						//  F:1:S0:1:U1:P0:1
						if (!task.getID().contains(":P")) {
							task.setID(task.getID() + ":P" + ID + ":"
									+ makeTaskID());
						} else {
			/*				String[] taskids = task.getID().split(":");
							taskids[5] = "P" + ID;
							taskids[6] = Integer.toString(makeTaskID());
							String taskid = Stream.of(taskids).collect(
									Collectors.joining(":"));
							task.setID(taskid);*/
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
							if (Config.DEBUG) {
								System.out
										.println("Universe-Space Proxy: Task "
												+ task.getID()
												+ "-"
												+ task.getLayer()
												+ "-"
												+ task.isCoarse()
												+ " is added to Space ReadyTaskQueue!");
							}
						}
					}
					if (Config.STATUSOUTPUT) {
						System.out.println(task.getID());
					}
				}
				System.out.println("Send Service: Space " + ID + " is Down!");
			}
		}
	}

}
