package result;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import api.Result;
import api.Task;
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
	 * @param coarse
	 *            Coarse flag
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
	 * @param runningTaskMap
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
		for (int i = 0; i < runningTasks.size(); i++) {
			runningTaskMap
					.put(runningTasks.get(i).getID(), runningTasks.get(i));
		}
		space.addSuccessorTask(subTasks.get(0));
		for (int i = 1; i < subTasks.size(); i++) {
			space.addReadyTask(subTasks.get(i));
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
	 */
	@Override
	public void process(UniverseImpl universe,
			Map<String, Task<?>> runningTaskMap) {
		universe.addSuccessorTask(subTasks.get(0));
		for (int i = 1; i < subTasks.size(); i++) {
			universe.addReadyTask(subTasks.get(i));
		}

	}
}
