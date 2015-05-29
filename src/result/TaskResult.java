package result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import api.Result;
import api.Task;
import config.Config;
import space.SpaceImpl;
import universe.UniverseImpl;

/**
 * 
 * Task Result is to contain task result of a task execution.
 *
 */
public class TaskResult<T> extends Result {
	private static final long serialVersionUID = -6074269436373736220L;

	/**
	 * Subtasks.
	 */
	private List<Task<T>> subTasks;

	/**
	 * Running Tasks.
	 */
	private List<Task<T>> runningTasks;

	/**
	 * Constructor of task result.
	 * 
	 * @param resultId
	 *            Result Id, same as its associated task Id.
	 * @param subTasks
	 *            Subtasks to be stored in Space. First task is a successor task
	 *            to be stored in Successor Task Queue, following by child ready
	 *            tasks to be stored in Ready Task Queue.
	 * @param runningTasks
	 *            The tasks to be cached in computer's local Ready Task Queue
	 *            and in Computer Proxy's Runnning Task Map.
	 * @param taskStartTime
	 *            Task start time.
	 * @param taskEndTime
	 *            Task end time.
	 */
	public TaskResult(String resultId, List<Task<T>> subTasks, boolean coarse,
			long taskStartTime, long taskEndTime) {
		super(resultId, TASKRESULT, coarse, taskStartTime, taskEndTime);
		this.subTasks = subTasks;
		this.runningTasks = new ArrayList<Task<T>>();
	}

	/* Orginal code */
	public TaskResult(String resultId, List<Task<T>> subTasks,
			List<Task<T>> runningTasks, boolean coarse, long taskStartTime,
			long taskEndTime) {
		super(resultId, TASKRESULT, coarse, taskStartTime, taskEndTime);
		this.subTasks = subTasks;
		this.runningTasks = runningTasks;
	}

	/**
	 * Get the subtasks.
	 * 
	 * @return the Subtasks
	 */
	public List<Task<T>> getSubTasks() {
		return subTasks;
	}

	/**
	 * Get the Running Tasks.
	 * 
	 * @return the runningTask
	 */
	public List<Task<T>> getRunningTasks() {
		return runningTasks;
	}

	/**
	 * Set some of child tasks to be running tasks.
	 * 
	 * @param num
	 *            Number of child tasks to be running tasks.
	 */
	public void setRunningTasks(int num) {
		if (num < 0 || num > subTasks.size()) {
			num = 0;
		}
		for (int i = 1; i <= num; i++) {
			Task<T> task = subTasks.remove(i);
			runningTasks.add(task);
		}
	}

	/**
	 * Process the result. Call from Space. Put Successor Task in the Successor
	 * Task Map, put Ready Task into the Ready Task Queue.
	 * 
	 * @param space
	 *            The Space implemetation in which the result is to be
	 *            processed.
	 * @param RunningTaskMap
	 *            The Running Task Map in the Computer Proxy, where the
	 *            associated task is stored.
	 * @param intermediateResultQueue
	 *            Intermediate Result Queue in which the result can be stored if
	 *            result processing failed.
	 */
	@Override
	public boolean process(final SpaceImpl space,
			final Map<String, Task<?>> runningTaskMap,
			final BlockingQueue<Result> intermediateResultQueue) {
		if (Config.DEBUG) {
			if (runningTasks.size() == 0) {
				System.out.println("	Result: RuningTask is empty!");
			}
		}
		for (int i = 0; i < runningTasks.size(); i++) {
			if (Config.DEBUG) {
				System.out.println("	Result: RunningTask "
						+ runningTasks.get(i).getID() + "-"
						+ runningTasks.get(0).getLayer() + "-"
						+ runningTasks.get(i).isCoarse());
			}
			runningTaskMap
					.put(runningTasks.get(i).getID(), runningTasks.get(i));
		}
		if (Config.DEBUG) {
			System.out.println("	Result: Successor " + subTasks.get(0).getID()
					+ "-" + subTasks.get(0).getLayer() + "-"
					+ subTasks.get(0).isCoarse());
		}
		space.addSuccessorTask(subTasks.get(0));
		for (int i = 1; i < subTasks.size(); i++) {
			space.addReadyTask(subTasks.get(i));
			if (Config.DEBUG) {
				System.out.println("	Result: Subtask "
						+ subTasks.get(i).getID() + "-"
						+ subTasks.get(i).getLayer() + "-"
						+ subTasks.get(i).isCoarse());
			}
		}
		return true;
	}

	/**
	 * Process the Result. Call from Space Proxy in Universe. Put Successor Task
	 * in the Successor Task Map, put Ready Task into the Ready Task Queue.
	 * 
	 * @param universe
	 *            Universe
	 * @param runningTaskMap
	 *            The running Task Map in the Space Proxy.
	 * @return The status of processing. True if processed successfully, false
	 *         otherwise.
	 */
	@Override
	public void process(UniverseImpl universe,
			Map<String, Task<?>> runningTaskMap) {
		if (Config.DEBUG) {
			System.out.println("	Result: Successor " + subTasks.get(0).getID()
					+ "-" + subTasks.get(0).getLayer() + "-"
					+ subTasks.get(0).isCoarse());
		}
		universe.addSuccessorTask(subTasks.get(0));
		for (int i = 1; i < subTasks.size(); i++) {
			universe.addReadyTask(subTasks.get(i));
			if (Config.DEBUG) {
				System.out.println("	Result: Subtask "
						+ subTasks.get(i).getID() + "-"
						+ subTasks.get(0).getLayer() +"-"
						+ subTasks.get(i).isCoarse());
			}
		}

	}
}
