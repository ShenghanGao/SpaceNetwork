package tsp_tasks;

import java.util.*;

import api.Result;
import result.ValueResult;
import task.SuccessorTask;

/**
 * TspSuccessorTask is the composing task of traveling salesman problem in
 * divide and conquer context.
 */
public class TspSuccessorTask extends SuccessorTask<TspData> {
	private static final long serialVersionUID = 663124446461676430L;

	/**
	 * The constructor of TSP successor task. Call from inside.
	 * 
	 * @param arg
	 *            Argument of the task.
	 * @param argNum
	 *            Number of needed arguments by the successor.
	 * @param targetSuccessorTaskId
	 *            Target successor task id, same as its parent target id.
	 * @param targetSuccessorTaskArgIndex
	 *            Target argument index, same as its parent target argument
	 *            index.
	 */
	public TspSuccessorTask(List<TspData> arg, int argNum,
			String targetSuccessorTaskId, int targetSuccessorTaskArgIndex) {
		super(arg, argNum, targetSuccessorTaskId, targetSuccessorTaskArgIndex);
	}

	/**
	 * Check if the Task is coarse or not.
	 * 
	 * @return True if the Task is coarse. False otherwise.
	 */
	@Override
	public boolean isCoarse() {
		if (getLayer() <= TspTaskConfig.TSPCoarse)
			return true;
		else
			return false;
	}

	/**
	 * Executes the task and generates a Value Result. Finds the minimum tour.
	 */
	@Override
	public Result execute() {
		long taskStartTime = System.nanoTime();
		List<TspData> args = this.getArg();

		double min = args.get(0).getDistance();
		List<Integer> res = args.get(0).getOrderedCities();

		for (int i = 1; i < args.size(); ++i) {
			double tmp = args.get(i).getDistance();
			if (tmp < min) {
				min = tmp;
				res = args.get(i).getOrderedCities();
			}
		}

		TspData solved = new TspData(min, res, new LinkedList<Integer>());
		long taskEndTime = System.nanoTime();
		return new ValueResult<TspData>(this.getID(), solved,
				this.getTargetID(), this.getTargetSuccessorTaskArgIndex(),
				this.isCoarse(), taskStartTime, taskEndTime);
	}
}
