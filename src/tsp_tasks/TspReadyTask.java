package tsp_tasks;

import java.util.*;

import api.Result;
import api.Task;
import result.TaskResult;
import result.ValueResult;
import task.ReadyTask;

/**
 * TspReadyTask is a decomposing task of traveling salesman problem in divide
 * and conquer context.
 */
public class TspReadyTask extends ReadyTask<TspData> {
	private static final long serialVersionUID = 3960813263738212661L;

	/**
	 * The number of the cities
	 */
	private final int numOfCities;

	/**
	 * The two-dimensional array containing the distances
	 */
	private final double[][] distance;

	/**
	 * The constructor of TSP ready task. Call from inside.
	 * 
	 * @param arg
	 *            Argument list
	 * @param targetSuccessorTaskArgIndex
	 *            Target compose task argument index
	 * @param numOfCities
	 *            The number of the cities
	 * @param DISTANCE
	 *            The two-dimensional array containing the distances
	 */
	public TspReadyTask(List<TspData> arg, int targetSuccessorTaskArgIndex,
			int numOfCities, double[][] DISTANCE) {
		super(arg, targetSuccessorTaskArgIndex);

		this.numOfCities = numOfCities;
		distance = new double[numOfCities][numOfCities];
		for (int i = 0; i < numOfCities; ++i)
			for (int j = 0; j < numOfCities; ++j)
				distance[i][j] = DISTANCE[i][j];
	}

	/**
	 * The constructor of TSP ready task. Call from job.
	 * 
	 * @param arg
	 *            Argument list
	 * @param numOfCities
	 *            The number of the cities
	 * @param DISTANCE
	 *            The two-dimensional array containing the distances
	 */
	public TspReadyTask(List<TspData> arg, int numOfCities, double[][] DISTANCE) {
		super(arg);
		this.numOfCities = numOfCities;
		distance = new double[numOfCities][numOfCities];
		for (int i = 0; i < numOfCities; ++i)
			for (int j = 0; j < numOfCities; ++j)
				distance[i][j] = DISTANCE[i][j];
	}

	/**
	 * Checks if a TSP ready task is atomic, it is related to the value set by
	 * <i>stopSign</i>.
	 */
	@Override
	public boolean isAtomic() {
		TspData tspdata = ((ArrayList<TspData>) getArg()).get(0);
		if (tspdata.getUnorderedCities().size() <= TspTaskConfig.stopSign) {
			return true;
		}
		return false;
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
	 * Executes the task and generates the corresponding result. If the task is
	 * atomic, the result is a Value Result. Otherwise, it is a Task Result.
	 */
	@Override
	public Result execute() {
		long taskStartTime = System.nanoTime();
		TspData tspdata = ((ArrayList<TspData>) getArg()).get(0);
		List<Integer> ordered = tspdata.getOrderedCities();
		List<Integer> unordered = tspdata.getUnorderedCities();

		if (isAtomic()) {
			Integer[] citiesToBePermuted = unordered.toArray(new Integer[0]);
			List<List<Integer>> permutation = permute(citiesToBePermuted);

			List<List<Integer>> orders = new ArrayList<List<Integer>>();
			for (List<Integer> li : permutation) {
				List<Integer> tmp = new ArrayList<Integer>();
				tmp.addAll(ordered);
				tmp.addAll(li);
				orders.add(tmp);
			}

			List<Integer> res = orders.get(0);
			double min = calRouteLength(orders.get(0), distance);

			for (int i = 1; i < orders.size(); ++i) {
				double tmp = calRouteLength(orders.get(i), distance);
				if (tmp < min) {
					min = tmp;
					res = orders.get(i);
				}
			}

			TspData solved = new TspData(min, res, new LinkedList<Integer>());

			long taskEndTime = System.nanoTime();
			return new ValueResult<TspData>(this.getID(), solved,
					this.getTargetID(), this.getTargetSuccessorTaskArgIndex(),
					isCoarse(), taskStartTime, taskEndTime);
		} else {
			List<Task<TspData>> subtasks = new ArrayList<Task<TspData>>();
			// Generate successor task.
			List<TspData> args = new ArrayList<TspData>();
			int argNum = unordered.size();
			for (int i = 0; i < argNum; i++) {
				args.add(null);
			}
			TspSuccessorTask successorTask = new TspSuccessorTask(args, argNum,
					this.getTargetID(), this.getTargetSuccessorTaskArgIndex());
			successorTask.setLayer(getLayer()+1);
			successorTask.setSpaceRunnable(true);
			subtasks.add(successorTask);

			for (int i = 0; i < unordered.size(); ++i) {
				List<TspData> arg = new ArrayList<TspData>();

				List<Integer> childOrdered = new ArrayList<Integer>();
				childOrdered.addAll(ordered);
				List<Integer> childUnordered = new ArrayList<Integer>();
				childUnordered.addAll(unordered);

				childOrdered.add(childUnordered.remove(i));

				TspData childData = new TspData(-8, childOrdered,
						childUnordered);
				arg.add(childData);

				TspReadyTask child = new TspReadyTask(arg, i, numOfCities,
						distance);
				child.setLayer(getLayer() + 1);
				subtasks.add(child);
			}
			long taskEndTime = System.nanoTime();
			return new TaskResult<TspData>(this.getID(), subtasks, successorTask.isCoarse(),
					taskStartTime, taskEndTime);
		}
	}

	/**
	 * Calculates the route length according to the order of the cities.
	 * 
	 * @param order
	 *            the order of the cities
	 * @param distance
	 *            the two-dimensional array containing the distances
	 * @return the route length
	 */
	private double calRouteLength(List<Integer> order, double[][] distance) {
		double length = 0;
		for (int i = 0; i < numOfCities - 1; ++i)
			length += distance[order.get(i)][order.get(i + 1)];
		length += distance[order.get(numOfCities - 1)][0];
		return length;
	}

	/**
	 * Calculates all possible permutations of a collection of numbers.
	 * 
	 * @param num
	 *            the collection of numbers
	 * @return all possible permutations
	 */
	private List<List<Integer>> permute(Integer[] num) {
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		result.add(new ArrayList<Integer>());

		for (int i = 0; i < num.length; ++i) {
			List<List<Integer>> current = new ArrayList<List<Integer>>();
			for (List<Integer> l : result) {
				for (int j = 0; j < l.size() + 1; ++j) {
					l.add(j, num[i]);
					List<Integer> temp = new ArrayList<Integer>(l);
					current.add(temp);
					l.remove(j);
				}
			}
			result = new ArrayList<List<Integer>>(current);
		}
		return result;
	}

}
