package fibonacci_tasks;

import java.util.List;

import api.Result;
import config.Config;
import result.ValueResult;
import task.SuccessorTask;

/**
 * 
 * Fibonacci Successor Task is the compose task of Fibonacci problem in Divide
 * and Conquer context.
 *
 */
public class FibonacciSuccessorTask extends SuccessorTask<Integer> {
	private static final long serialVersionUID = -4763943868568664027L;

	/**
	 * Constructor of Fibonacci successor task. Call from inside.
	 * 
	 * @param arg
	 *            Argument of the task. A single integer.
	 * @param argNum
	 *            Number of needed argument. 2
	 * @param targetSuccessorTaskId
	 *            Target successor task id, same as its parent target id.
	 * @param targetSuccessorTaskArgIndex
	 *            Target argument index, same as its parent target argument
	 *            index.
	 */
	public FibonacciSuccessorTask(List<Integer> arg, int argNum,
			String targetSuccessorTaskId, int targetSuccessorTaskArgIndex) {
		super(arg, argNum, targetSuccessorTaskId, targetSuccessorTaskArgIndex);
		this.setSpaceRunnable(true);
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
	 * Execute the task and generate a Value Result. Calculate the sum of its
	 * arguments.
	 */
	@Override
	public Result execute() {
		long taskStartTime = System.nanoTime();
		List<Integer> list = this.getArg();
		Integer sum = 0;
		for (Integer i : list) {
			if (i == null) {
				if (Config.DEBUG) {
					System.out.println("Successor " + this.getID()
							+ " has null arg!");
				}
			}
			sum += i;
		}
		long taskEndTime = System.nanoTime();
		return new ValueResult<Integer>(this.getID(), sum, this.getTargetID(),
				this.getTargetSuccessorTaskArgIndex(), isCoarse(),
				taskStartTime, taskEndTime);
	}

}
