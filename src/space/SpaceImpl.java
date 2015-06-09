package space;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import api.Computer;
import api.Result;
import api.Space;
import api.Task;
import api.Universe;
import config.Config;
import result.ValueResult;
import task.SuccessorTask;

/**
 * 
 * Implementation of Space, managing task queues and monitoring Computers.
 *
 */
public class SpaceImpl extends UnicastRemoteObject implements Space {
	private static final long serialVersionUID = 8482035337875894981L;

	/**
	 * Space itself. One Space in this JVM.
	 */
	private static SpaceImpl space;

	/**
	 * Space ID.
	 */
	private static int ID;

	/**
	 * Computer Id.
	 */
	private static final AtomicInteger ComputerID = new AtomicInteger();

	/**
	 * Ready Task Queue. Containing tasks ready to run.
	 */
	private final BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Successor Task Map. Containing successor tasks waiting for arguments.
	 */
	private final Map<String, Task<?>> successorTaskMap;

	/**
	 * Result Queue. Containing the final result of the coarse task.
	 */
	private final BlockingQueue<Result> resultQueue;

	/**
	 * Computer Proxies Map.
	 */
	private final Map<Integer, ComputerProxy> computerProxies;

	/**
	 * Constructor of Space Implementation.
	 * 
	 * @throws RemoteException
	 *             Cannot connect to Space.
	 * @throws NotBoundException
	 *             Bad Universe Domain Name
	 * @throws MalformedURLException
	 *             Bad Universe Domain Name
	 */
	public SpaceImpl(String universeDomainName) throws RemoteException,
			MalformedURLException, NotBoundException {
		readyTaskQueue = new LinkedBlockingQueue<>();
		successorTaskMap = Collections.synchronizedMap(new HashMap<>());
		resultQueue = new LinkedBlockingQueue<>();
		computerProxies = Collections.synchronizedMap(new HashMap<>());
		String url = "rmi://" + universeDomainName + ":" + Universe.PORT + "/"
				+ Universe.SERVICE_NAME;
		Universe universe;
		universe = (Universe) Naming.lookup(url);
		universe.register(this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Space {0} started.", ID);
	}

	public static void main(final String[] args) {
		String universeDomainName = args.length == 0 ? "localhost" : args[0];
		System.setSecurityManager(new SecurityManager());
		try {
			space = new SpaceImpl(universeDomainName);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.out.println("Cannot register to the Universe!");
			return;
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Universe domain name!");
			return;
		}

		try {
			LocateRegistry.createRegistry(Space.PORT).rebind(
					Space.SERVICE_NAME, space);
		} catch (RemoteException e) {
			System.out.println("Fail to bind Server!\n");
			return;
		}

		// Main thread waiting for Key Enter to terminate.
		try {
			System.in.read();
		} catch (Throwable ignored) {

		}
		System.out.println("Space stopped.\n");
		System.exit(-1);
	}

	/**
	 * Set an ID to the Space. Call from Universe.
	 * 
	 * @param spaceID
	 *            Space Id.
	 * @throws RemoteException
	 *             Failed to connect to Space.
	 */
	@Override
	public void setID(int spaceID) throws RemoteException {
		ID = spaceID;
	}

	/**
	 * Add a Task to the Ready Task Queue. Call from Space Proxy in Universe.
	 * 
	 * @param task
	 *            The Ready/Successor Task to be submitted.
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	@Override
	public void addTask(final Task<?> task) throws RemoteException {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
			System.out
					.println("Interrupted when adding task to ready task queue!");
		}
	}

	/**
	 * Get a Task from Ready Task Queue.
	 * 
	 * @return Task.
	 */
	public Task<?> getReadyTask() {
		return readyTaskQueue.poll();
	}

	/**
	 * Add a Successor Task to Successor Task Map.
	 * 
	 * @param task
	 *            Task to be added.
	 */
	public void addSuccessorTask(Task<?> task) {
		successorTaskMap.put(task.getID(), task);
	}

	/**
	 * Get a task from the Successor Task Map with Task Id.
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
		if (successorTaskMap.remove(successortask.getID()) != null) {
			try {
				readyTaskQueue.put(successortask);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add the final result into the Result Queue.
	 * 
	 * @param result
	 *            The final result.
	 */
	public void addResult(Result result) {
		try {
			resultQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get Result from Result Queue. Call from Space Proxy in Universe.
	 * 
	 * @return Result
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	@Override
	public Result getResult() throws RemoteException {
		Result result = null;
		try {
			result = resultQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Register a Computer and associate it with a Computer Proxy into Space.
	 * Start the Computer Proxy.
	 */
	@Override
	public void register(final Computer computer) throws RemoteException {
		final ComputerProxy computerproxy = new ComputerProxy(computer,
				ComputerID.getAndIncrement());
		computer.setID(computerproxy.ID);
		computerProxies.put(computerproxy.ID, computerproxy);
		computerproxy.start();
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Computer {0} started with {1} workers!",
				new Object[] { computerproxy.ID, computer.getWorkerNum() });
	}

	/**
	 * Unregister a Computer and remove its associated Computer Proxy.
	 * Processing all unfinished Value Results. Save all the Computer's
	 * unfinished running tasks into Space Ready Task Queue.
	 * 
	 * @param computer
	 */
	private void unregister(ComputerProxy computerProxy) {
		computerProxies.remove(computerProxy.ID);
		Result result = null;
		while ((result = computerProxy.intermediateResultQueue.poll()) != null) {
			if (result.isCoarse()) {
				space.addResult(result);
				computerProxy.runningTaskMap.remove(result.getID());
			} else {
				if (!result.process(space, computerProxy.runningTaskMap,
						computerProxy.intermediateResultQueue)) {
					space.addResult(result);
				}
				computerProxy.runningTaskMap.remove(result.getID());
			}
			if (Config.STATUSOUTPUT || Config.DEBUG) {
				System.out.println("Unregister Result: " + result.getID() + "!"
						+ ((ValueResult<?>) result).getTargetTaskID());
			}
		}
	/*	// Unregister Task ID Reset
		// !:F:1:S0:1:U1:P0:1:C1:1
		// !:F:1:S0:1:U1:P0:1
		//   F:1:S0:1:U1:P0:1:C1:1:W1
		//	 F:1:S0:1:U1:P0:1
*/		if (!computerProxy.runningTaskMap.isEmpty()) {
			for (String taskId : computerProxy.runningTaskMap.keySet()) {
				try {
					Task<?> task = computerProxy.runningTaskMap.get(taskId);
					task.getID().substring(task.getID().indexOf(":C"));
					readyTaskQueue.put(task);
					if (Config.STATUSOUTPUT || Config.DEBUG) {
						System.out.println("Save Task:" + taskId);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
				"Computer {0} failed.", computerProxy.ID);
	}

	/**
	 * Space directly executes the task.
	 * 
	 * @param <T>
	 *            Argument Type of the Task.
	 * @param successortask
	 *            The successor task to be executed.
	 * @param TempResultQueue
	 *            The Temporary Result Queue in Computer Proxy to contain the
	 *            execution result.
	 */
	@SuppressWarnings("unchecked")
	public <T> void spaceExecuteTask(SuccessorTask<T> successortask,
			BlockingQueue<Result> intermediateResultQueue) {
		successorTaskMap.remove(successortask.getID());
		ValueResult<T> result = (ValueResult<T>) successortask.execute();
		try {
			intermediateResultQueue.put(result);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (Config.STATUSOUTPUT || Config.DEBUG) {
			System.out.println("	Space Direct Execution: " + result.getID());
		}
	}

	/**
	 * 
	 * A Computer Proxy is to manage its associated Computer's working status.
	 * It gives the Computer tasks and gets results, meanwhile keeps track of
	 * the tasks the Computer is running, which is responsible for fault
	 * tolerance.
	 *
	 */
	private class ComputerProxy {
		/**
		 * Computer associated with this Computer Proxy.
		 */
		private final Computer computer;

		/**
		 * Computer Id.
		 */
		private final int ID;

		/**
		 * Task ID.
		 */
		private final AtomicInteger TaskID = new AtomicInteger();

		/**
		 * Running Task Map. The tasks that Computer is running.
		 */
		private final Map<String, Task<?>> runningTaskMap;

		/**
		 * Intermediate Result Queue. Store Results of Space Direct Execution
		 */
		private final BlockingQueue<Result> intermediateResultQueue;

		/**
		 * Receive Service thread.
		 */
		private final ReceiveService receiveService;

		/**
		 * Send Service thread.
		 */
		private final SendService sendService;

		/**
		 * Constructor of Computer Proxy.
		 * 
		 * @param computer
		 *            The Computer associated with this Computer Proxy.
		 * @param comptuerid
		 *            Computer ID
		 */
		private ComputerProxy(Computer computer, int computerid) {
			this.computer = computer;
			this.ID = computerid;
			this.runningTaskMap = Collections.synchronizedMap(new HashMap<>());
			this.intermediateResultQueue = new LinkedBlockingQueue<>();
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

		/**
		 * Receive Service is a thread for non-blocking polling results from the
		 * Computer's Result Queue as well as the Temporary Result Queue, and
		 * process the result. If the result is processed successfully, remove
		 * its associated task from Computer Proxy Running Task Map.
		 */
		private class ReceiveService extends Thread {
			@Override
			public void run() {
				while (true) {
					Result computerResult = null;
					try {
						// Get result from Computer Result Queue.
						computerResult = computer.getResult();
					} catch (RemoteException ex) {
						System.out.println("Receive Service: Computer " + ID
								+ " is down!");
						try {
							sendService.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						unregister(ComputerProxy.this);
						return;
					}
					if (computerResult != null) {
						if (Config.DEBUG) {
							System.out.println("Space-Computer Proxy: Result "
									+ computerResult.getID() + "-"
									+ computerResult.isCoarse()
									+ " is processing!");
						}
						synchronized (runningTaskMap) {
							if (computerResult.isCoarse()) {
								// Delete Track
								// F:1:S0:1:U1:P1:1:C1:1:W2

								runningTaskMap.remove(computerResult.getID());
								if (computerResult.getID().charAt(0) == '!') {
									int index = computerResult.getID().indexOf(
											":C");
									String resultid = computerResult.getID()
											.substring(0, index);
									computerResult.setID(resultid);
								}
								// Result ID Reset
								// !:F:1:S0:1:U1:P1:1:C1:1
								// !:F:1:S0:1:U1:P1:1
								space.addResult(computerResult);
								if (Config.DEBUG) {
									System.out
											.println("	Result "
													+ computerResult.getID()
													+ " is coarse, added to Space Result Queue!");
								}
							} else {
								if (!computerResult
										.process(space, runningTaskMap,
												intermediateResultQueue)) {
									runningTaskMap.remove(computerResult
											.getID());
									if (computerResult.getID().charAt(0) == '!') {
										int index = computerResult.getID()
												.indexOf(":C");
										String resultid = computerResult
												.getID().substring(0, index);
										computerResult.setID(resultid);
									}
									space.addResult(computerResult);
									if (Config.DEBUG) {
										System.out
												.println("	Result "
														+ computerResult
																.getID()
														+ " is not coarse but should be added to Space Result Queue!");
									}
								} else {
									runningTaskMap.remove(computerResult
											.getID());
									if (Config.DEBUG) {
										System.out.println("	Result "
												+ computerResult.getID()
												+ " is processed by Space!");
									}
								}
							}
						}
					}
					// Get the result from Intermediate Result Queue.
					Result spaceResult = intermediateResultQueue.poll();
					if (spaceResult != null) {
						synchronized (runningTaskMap) {
							if (spaceResult.isCoarse()) {
								space.addResult(spaceResult);
								runningTaskMap.remove(spaceResult.getID());
							} else {
								if (!spaceResult.process(space, runningTaskMap,
										intermediateResultQueue)) {
									space.addResult(spaceResult);
								}
								runningTaskMap.remove(spaceResult.getID());
							}
						}
					}
				}
			}
		}

		/**
		 * Send Service is a thread for putting task from Space Ready Task Queue
		 * to Computer's Ready Task Queue for Computer to execute it and
		 * Computer Proxy Running Task Map to keep track the task.
		 */
		private class SendService extends Thread {
			@Override
			public void run() {
				while (true) {
					Task<?> task = null;
					try {
						if (computer.isBusy()) {
							try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								break;
							}
							continue;
						}
					} catch (RemoteException e1) {
						System.out.println("Send Service: Computer " + ID
								+ " is down!");
						return;
					}
					task = space.getReadyTask();
					if (task == null) {
						continue;
					}
					// Task ID Reset
					// Computer Task Track
					// !:F:1:S0:1:U1:P1:1
					// !:F:1:S0:1:U1:P1:1:C1:1
					//   F:1:S0:1:U1:P0:1:C2:3:W1
					//   F:1:S0:1:U1:P0:1:C1:1:W1
					// F:1:S0:1:U1:P0:1:C1:W1
					if (task.getID()
							.matches("^(!:)?[^!$:]+:\\d+:S\\d+:\\d+:U\\d+:P\\d+:\\d+$")) {
					//	task.setID(task.getID() + ":C" + ID + ":"
						//		+ makeTaskID());
						task.setID(task.getID() + ":C" + ID);
					} else {
		/*				String[] taskids = task.getID().split(":");
						taskids[7] = "C" + ID;
						taskids[8] = Integer.toString(makeTaskID());
						String taskid = Stream.of(taskids).collect(
								Collectors.joining(":"));
						task.setID(taskid);*/
					}
					synchronized (runningTaskMap) {
						try {
							computer.addTask(task);
						} catch (RemoteException e) {
							System.out.println("Send Service: Computer " + ID
									+ " is down!");
							if (task != null) {
								space.addReadyTask(task);
							}
							return;
						}
						runningTaskMap.put(task.getID(), task);
						if (Config.DEBUG) {
							System.out.println("Space-Computer Proxy: Task "
									+ task.getID() + "-" + task.getLayer()
									+ "-" + task.isCoarse()
									+ " is added to Computer ReadyTaskQueue!");
						}
					}
					if (Config.STATUSOUTPUT) {
						System.out.println(task.getID());
					}
				}
				System.out
						.println("Send Service: Computer " + ID + " is down!");
			}

		}

	}
}
