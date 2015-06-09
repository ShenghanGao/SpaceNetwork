package computer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import api.Computer;
import api.Result;
import api.Space;
import api.Task;
import result.TaskResult;
import config.Config;

/**
 * Implementation of Computer, generating Task Proxies to execute the tasks in
 * its Ready Task Queue and put the results into Result Queue. The Computer
 * assigns each task with an task ID.
 *
 */
public class ComputerImpl extends UnicastRemoteObject implements Computer {
	private static final long serialVersionUID = 4049656705569742423L;

	/**
	 * Computer ID.
	 */
	private int ID;

	/**
	 * Task ID.
	 */
	private final static AtomicInteger TaskID = new AtomicInteger();

	/**
	 * Ready Task Queue.
	 */
	private final BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Result Queue.
	 */
	private final BlockingQueue<Result> resultQueue;

	/**
	 * Number of Workers
	 */
	private final int workerNum;

	/**
	 * Task Proxy Threads.
	 */
	private final Worker[] workers;

	/**
	 * Space
	 */
	private static Space space;

	/**
	 * Constructor of Computer Implementation. Generate and stat Task Proxies.
	 * 
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 * @throws NotBoundException
	 * @throws MalformedURLException
	 */
	public ComputerImpl(String spaceDomainName) throws RemoteException,
			MalformedURLException, NotBoundException {
		resultQueue = new LinkedBlockingQueue<>();
		readyTaskQueue = new LinkedBlockingQueue<>();
		if (Config.ComputerMultithreadFlag) {
			// Get available processors in JVM
			workerNum = Runtime.getRuntime().availableProcessors();
		} else {
			workerNum = 1;
		}
		workers = new Worker[workerNum];
		for (int i = 0; i < workerNum; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		final String url = "rmi://" + spaceDomainName + ":" + Space.PORT + "/"
				+ Space.SERVICE_NAME;
		space = (Space) Naming.lookup(url);
		space.register(this);
		Logger.getLogger(ComputerImpl.class.getName())
				.log(Level.INFO,
						"Computer {0} started with " + workerNum + " workers.",
						this.ID);
	}

	public static void main(String[] args) {
		System.setSecurityManager(new SecurityManager());
		final String spaceDomainName = args.length == 0 ? "localhost" : args[0];

		ComputerImpl computer;
		try {
			computer = new ComputerImpl(spaceDomainName);
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Space domain name!");
			return;
		} catch (RemoteException e) {
			System.out.println("Cannot register to the Space!");
			return;
		}

		// Main thread waiting for Key Enter to terminate.
		try {
			System.in.read();
		} catch (Throwable ignored) {

		}
		Logger.getLogger(ComputerImpl.class.getName()).log(Level.INFO,
				"Computer " + computer.ID + " exited.");
		System.exit(-1);
	}

	/**
	 * Set an ID to the computer. Call from Space.
	 * 
	 * @param computerID
	 *            Computer ID.
	 * @throws RemoteException
	 *             Failed to connect computer.
	 */
	@Override
	public void setID(int computerID) throws RemoteException {
		this.ID = computerID;
	}

	/**
	 * Get the number of Workers running in the Computer. Call from Space.
	 * 
	 * @return Number of Workers.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	@Override
	public int getWorkerNum() throws RemoteException {
		return workerNum;
	}

	/**
	 * Check if the Computer is busy. Call from Computer Proxy in Space.
	 * 
	 * @return True if Computer is busy. False otherwise.
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 */
	@Override
	public boolean isBusy() throws RemoteException {
		if (readyTaskQueue.size() > Config.ComputerWorkload * this.workerNum) {
			return true;
		}
		return false;
	}

	/**
	 * Add a task to Ready Task Queue. Call from Computer Proxy in Space.
	 * 
	 * @param task
	 *            The Task to be added.
	 * @throws RemoteException
	 *             Failed to connect to Computer.
	 */
	@Override
	public void addTask(Task<?> task) throws RemoteException {
		try {
			readyTaskQueue.put(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get a Result from Result Queue. Call from Computer Proxy in Space.
	 * 
	 * @return The execution Result.
	 * @throws RemoteException
	 *             Failed to connect to computer.
	 */
	@Override
	public Result getResult() throws RemoteException {
		Result result = null;
		result = resultQueue.poll();
		return result;
	}

	/**
	 * Get a Task from the Ready Task Queue.
	 * 
	 * @return Task
	 */
	private Task<?> getReadyTask() {
		try {
			return readyTaskQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
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
	 * Cache the subtasks in Task Result into Ready Task Queue.
	 * 
	 * @param result
	 *            Task Result
	 */
	private <T> void cacheTasks(TaskResult<T> result) {
		List<Task<T>> runningtasks = result.getRunningTasks();
		for (int i = 0; i < runningtasks.size(); i++) {
			try {
				readyTaskQueue.put(runningtasks.get(i));
				if (Config.DEBUG) {
					System.out
							.println("	Cache: " + runningtasks.get(i).getID());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
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
	 * Generate a list of Task ID. ClientName:Num:S:Num:U:P:Num:C:W
	 * F:1:S0:1:U1:P1:1:C1:W1
	 * 
	 * @param oldID
	 *            Old ID
	 * @param num
	 *            Number of new Tasks
	 * @return List of Task ID
	 */
	private String[] makeTaskIDList(String oldID, int num) {
		String[] IDs = new String[num];
		/*
		 * int index = oldID.indexOf(":W"); String prefix = oldID.substring(0,
		 * index + 2); for (int i = 0; i < num; i++) { String taskid = prefix +
		 * makeTaskID(); IDs[i] = taskid; } return IDs;
		 */
		int index = oldID.indexOf(":C");
		String prefix = oldID.substring(0, index + 2);
		for (int i = 0; i < num; i++) {
			String taskid = prefix + ID + ":W" + makeTaskID();
			IDs[i] = taskid;
		}
		return IDs;
	}

	/**
	 * 
	 * A Worker is a thread to get tasks from Computer Ready Queue and execute
	 * them, put the results into Computer Result Task Queue.
	 *
	 */
	private class Worker extends Thread {
		@Override
		public void run() {
			while (true) {
				// Debug Only
				/*
				 * try { Thread.sleep(1000); } catch (InterruptedException e) {
				 * e.printStackTrace(); }
				 */
				Task<?> task = getReadyTask();
				// Task ID Reset
				// !:F:1:S0:1:U1:P1:1:C1:1:W1
				// F:1:S0:1:U1:P0:1:C1:1:W1
				if (!task.getID().contains(":W")) { // only for the virgin
					task.setID(task.getID() + ":W" + makeTaskID());
				}
				if (Config.DEBUG) {
					System.out.println("Worker: Task " + task.getID() + "-"
							+ task.getLayer() + "-" + task.isCoarse()
							+ " is running!");
				}
				Result result = execute(task);
				if (!result.isCoarse()) {
					if (Config.AmeliorationFlag
							&& result.getType() == Result.TASKRESULT) {
						((TaskResult<?>) result)
								.setRunningTasks(Config.CacheTaskNum);
					}
				}
				addResult(result);
				if (Config.DEBUG) {
					System.out.println("Worker: Result " + result.getID() + "-"
							+ result.isCoarse()
							+ " is added to Computer ResultQueue!");
				}
				if (!result.isCoarse()) {
					if (Config.AmeliorationFlag
							&& result.getType() == Result.TASKRESULT) {
						if (Config.DEBUG) {
							List<?> runningTasks = ((TaskResult<?>) result)
									.getRunningTasks();
							System.out.println("	Running task setted "
									+ ((Task<?>) runningTasks.get(0)).getID()
									+ "-"
									+ ((Task<?>) runningTasks.get(0))
											.getLayer()
									+ "-"
									+ ((Task<?>) runningTasks.get(0))
											.isCoarse() + "-"
									+ ((Task<?>) runningTasks.get(0)).getArg());
						}
						cacheTasks((TaskResult<?>) result);
					}
				}
				if (Config.STATUSOUTPUT) {
					System.out.println(result.getID());
				}
			}
		}
	}

	/**
	 * Execute the task and generate the result. Assign every subtask with an
	 * task ID. Root !:F:1:S0:1:U1:P1:1:C1:1:W1 Result !:F:1:S0:1:U1:P1:1:C1:1
	 * STask F:1:S0:1:U1:P1:1:C1:1:W1
	 * 
	 * NormalT F:1:S0:1:U1:P1:1:C1:1:W2 NormalR F:1:S0:1:U1:P1:1:C1:1:W2 STask
	 * F:1:S0:1:U1:P1:1:C1:1:W1
	 */
	private <T> Result execute(Task<T> task) {
		final Result result = task.execute();
		final int resultType = result.getType();
		if (result.getID().charAt(0) == '!') {
			// Only the result of the root task is able to reach here.
			int index = result.getID().indexOf(":W");
			String resultid = result.getID().substring(2, index);
			result.setID(resultid);
			String taskid = task.getID().substring(2);
			task.setID(taskid);
		}
		if (resultType == Result.VALUERESULT) {
			return result;
		} else {
			// If the result is Task Result, assign subtasks with Task ID.
			@SuppressWarnings("unchecked")
			List<Task<T>> subtasks = (List<Task<T>>) ((TaskResult<T>) result)
					.getSubTasks();
			String[] taskIDs = makeTaskIDList(task.getID(), subtasks.size());
			// Assign Successor Task with an Task ID
			Task<?> successor = subtasks.get(0);
			successor.setID(taskIDs[0]);
			if (Config.DEBUG) {
				System.out.println("	Successor: " + successor.getID() + "-"
						+ successor.getLayer() + "-" + successor.isCoarse());
			}

			// Assign other Ready Task with Task IDs
			for (int i = 1; i < subtasks.size(); i++) {
				Task<?> subtask = subtasks.get(i);
				subtask.setID(taskIDs[i]);
				subtask.setTargetID(taskIDs[0]);
				if (Config.DEBUG) {
					System.out.println("	Subtask: " + subtask.getID() + "-"
							+ subtask.getLayer() + "-" + subtask.isCoarse()
							+ "-" + subtask.getArg().get(0));
				}
			}
			return result;
		}
	}

	/**
	 * Exit
	 */
	@Override
	public void exit() throws RemoteException {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"Computer: exiting.");
		System.exit(0);
	}

}
