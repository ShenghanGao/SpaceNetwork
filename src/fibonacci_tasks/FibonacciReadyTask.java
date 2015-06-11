package fibonacci_tasks;

import java.util.ArrayList;
import java.util.List;

import api.Result;
import api.Task;
import result.TaskResult;
import result.ValueResult;
import task.ReadyTask;

/**
 * 
 * FibonacciReadyTask is a decompose task of Fibonacci problem in Divide and
 * Conquer context.
 *
 */
public class FibonacciReadyTask extends ReadyTask<Integer> {
	private static final long serialVersionUID = 4457744042261357273L;

	/**
	 * Constructor of Fibonacci ready task. Call from inside.
	 * 
	 * @param arg
	 *            Argument list.
	 * @param targetSuccessorTaskArgIndex
	 *            Target compose task argument index.
	 */
	public FibonacciReadyTask(List<Integer> arg, int targetSuccessorTaskArgIndex) {
		super(arg, targetSuccessorTaskArgIndex);
	}

	/**
	 * Constructor of Fibonacci ready task. Call from Client.
	 * 
	 * @param arg
	 *            Argument list with a single number.
	 */
	public FibonacciReadyTask(List<Integer> arg) {
		super(arg);
	}

	/**
	 * Check if the Task is coarse or not.
	 * 
	 * @return True if the Task is coarse. False otherwise.
	 */
	@Override
	public boolean isCoarse() {
		if (getLayer() <= FibonacciTaskConfig.FibonacciCoarse)
			return true;
		else
			return false;
	}

	/**
	 * A Fibonacci Ready Task is atomic and cannot further decompose when the
	 * argument is less than 2.
	 */
	@Override
	public boolean isAtomic() {
		int n = ((ArrayList<Integer>) getArg()).get(0);
		if (n < 2) {
			return true;
		}
		return false;
	}

	/**
	 * Execute the task and generate the corresponding result. If the task is
	 * atomic, the result is a Value Result. Otherwise it is a Task Result.
	 * 
	 */
	@Override
	public Result execute() {
		long taskStartTime = System.nanoTime();
		// If the number is less than 2, generate Value Result.
		if (isAtomic()) {
			Integer n = ((ArrayList<Integer>) getArg()).get(0);
			long taskEndTime = System.nanoTime();
			return new ValueResult<Integer>(this.getID(), n,
					this.getTargetID(), this.getTargetSuccessorTaskArgIndex(),
					this.isCoarse(), taskStartTime, taskEndTime);
		} else {

			List<Task<Integer>> subtasks = new ArrayList<Task<Integer>>();
			// Generate successor task.
			List<Integer> arg = new ArrayList<Integer>();
			for (int i = 0; i < FibonacciTaskConfig.argNum; i++) {
				arg.add(null);
			}
			FibonacciSuccessorTask successorTask = new FibonacciSuccessorTask(
					arg, FibonacciTaskConfig.argNum, this.getTargetID(),
					this.getTargetSuccessorTaskArgIndex());
			successorTask.setLayer(getLayer() + 1);
			successorTask.setSpaceRunnable(true);
			subtasks.add(successorTask);

			// Generate child ready task.
			Integer n = ((ArrayList<Integer>) getArg()).get(0);
			for (int i = 0; i < FibonacciTaskConfig.argNum; i++) {
				List<Integer> args = new ArrayList<Integer>();
				args.add(n - i - 1);
				int argIndex = i;
				FibonacciReadyTask subtask = new FibonacciReadyTask(args,
						argIndex);
				subtask.setLayer(getLayer() + 1);
				subtasks.add(subtask);
			}

			long taskEndTime = System.nanoTime();
			return new TaskResult<Integer>(this.getID(), subtasks, successorTask.isCoarse(),
					taskStartTime, taskEndTime);
		}
	}

}
