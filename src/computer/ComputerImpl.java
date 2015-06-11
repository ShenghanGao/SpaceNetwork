package computer;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.UUID;
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
	private static AtomicInteger TaskCount = new AtomicInteger();

	/**
	 * Ready Task Queue.
	 */
	private BlockingQueue<Task<?>> readyTaskQueue;

	/**
	 * Result Queue.
	 */
	private BlockingQueue<Result> resultQueue;

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

	@SuppressWarnings("unused")
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
			System.out.println("Cannot regiseter to the Space!");
			return;
		}
		int lastCount = 0;
		while (true) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
			}
			if (lastCount != TaskCount.get()) {
				lastCount = TaskCount.get();
				System.out.println("Task Count: " + lastCount);
			}
		}
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
			System.out.println("Interrupted!");
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
		return readyTaskQueue.poll();
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
			} catch (InterruptedException e) {
				System.out.println("Interrupted!");
			}
		}
	}

	/**
	 * Counter tasks
	 * 
	 * @return number of tasks.
	 */
	private int taskCount() {
		return TaskCount.incrementAndGet();
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
		for (int i = 0; i < num - 1; i++) {
			IDs[i] = UUID.randomUUID().toString();
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
				Task<?> task = getReadyTask();
				if (task == null) {
					continue;
				}
				Result result = execute(task);
				if (!result.isCoarse()) {
					if (Config.AmeliorationFlag
							&& result.getType() == Result.TASKRESULT) {
						((TaskResult<?>) result)
								.setRunningTasks(Config.CacheTaskNum);
					}
				}
				result.setComputerCount(taskCount());
				if (readyTaskQueue.size() > workerNum * 4) {
					result.setComputerIsBusy(true);
				} else {
					result.setComputerIsBusy(false);
				}
				addResult(result);
				if (!result.isCoarse()) {
					if (Config.AmeliorationFlag
							&& result.getType() == Result.TASKRESULT) {
						cacheTasks((TaskResult<?>) result);
					}
				}
			}
		}
	}

	/**
	 * Execute the task and generate the result. Assign every subtask with an
	 * task ID.
	 */
	private <T> Result execute(Task<T> task) {
		final Result result = task.execute();
		final int resultType = result.getType();
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
			successor.setID(task.getID());

			// Assign other Ready Task with Task IDs
			for (int i = 1; i < subtasks.size(); i++) {
				Task<?> subtask = subtasks.get(i);
				subtask.setID(taskIDs[i - 1]);
				subtask.setTargetID(successor.getID());
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

	@SuppressWarnings("deprecation")
	@Override
	public void restart() throws RemoteException {
		System.out.println("Computer restart");
		TaskCount = new AtomicInteger();
		for (Worker w : workers) {
			w.interrupt();
			w.stop();
			w = null;
		}
		resultQueue = new LinkedBlockingQueue<>();
		readyTaskQueue = new LinkedBlockingQueue<>();
		for (int i = 0; i < workerNum; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
	}

}
