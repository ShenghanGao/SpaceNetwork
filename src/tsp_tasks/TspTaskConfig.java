package tsp_tasks;

public abstract class TspTaskConfig {

	/**
	 * The threshold below which the tasks will be no longer divided into
	 * several child tasks, the results of the tasks would be calculated
	 * directly instead
	 */
	public static final int stopSign = 8;

	/**
	 * TSP Task Corase Level
	 */
	public static final int TSPCoarse = 3;
}
