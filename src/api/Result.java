package api;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import space.SpaceImpl;
import universe.UniverseImpl;

/**
 * Result is for containing result of a task execution, including the result Id,
 * task start time and end time. Each result is associated with its
 * corresponding task, both of them have the same ID.
 * 
 */
public abstract class Result implements Serializable {
	private static final long serialVersionUID = 5197752215627028297L;

	/**
	 * Value Result Type Identifier.
	 */
	public static final Integer VALUERESULT = 0;

	/**
	 * Task Result Type Identifier.
	 */
	public static final Integer TASKRESULT = 1;

	/**
	 * Result ID, same as its associated task ID.
	 */
	private String ID;

	/**
	 * Task start time.
	 */
	private long taskStartTime;

	/**
	 * Task end time.
	 */
	private long taskEndTime;

	/**
	 * Result type.
	 */
	private Integer type;

	/**
	 * Coarse Flag.
	 */
	private Boolean coarse;

	/**
	 * Computer Task Count
	 */
	private Integer ComputerCount;

	/**
	 * Computer is busy
	 */
	private Boolean computerIsBusy;

	/**
	 * Space Task Count
	 */
	private Integer spaceCount;

	/**
	 * Space is busy
	 */
	private Boolean spaceIsBusy;

	/**
	 * Constructor of Result.
	 * 
	 * @param resultId
	 *            Result Id
	 * @param resultType
	 *            Result Type.
	 * @param coarse
	 *            Coarse Flag
	 * @param taskStartTime
	 *            Task start time.
	 * @param taskEndTime
	 *            Task end time.
	 */
	public Result(String resultId, Integer resultType, boolean coarse,
			long taskStartTime, long taskEndTime) {
		this.ID = resultId;
		this.type = resultType;
		this.coarse = coarse;
		this.taskStartTime = taskStartTime;
		this.taskEndTime = taskEndTime;
	}

	/**
	 * Get the result Id.
	 * 
	 * @return the resultId Result Id.
	 */
	public String getID() {
		return this.ID;
	}

	/**
	 * Set Result ID
	 * 
	 * @param resultid
	 *            Result ID
	 */
	public void setID(String resultid) {
		this.ID = resultid;
	}

	/**
	 * Get the result type.
	 * 
	 * @return the resultType 0 if it is Value Result. 1 if it is Task Result.
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * Check if the Result is coarse or not.
	 * 
	 * @return True is the Result is coarse. False otherwise.
	 */
	public boolean isCoarse() {
		return this.coarse;
	}

	/**
	 * Get the task Runtime.
	 * 
	 * @return the taskRuntime The Difference between task end time and task
	 *         start time.
	 */
	public long getTaskRuntime() {
		return this.taskEndTime - this.taskStartTime;
	}

	/**
	 * Process the result. Call from Computer Proxy in Space.
	 * 
	 * @param space
	 *            The Space implementation in which the result is to be
	 *            processed.
	 * @param runningTaskMap
	 *            The Running Task Map in the Computer Proxy, where the
	 *            associated task is stored.
	 * @param resultQueue
	 *            Intermediate Result Queue in which the result can be stored
	 *            after Space Direct Execution.
	 * @return The status of processing. True if processed successfully, false
	 *         otherwise.
	 */
	public abstract boolean process(final SpaceImpl space,
			final Map<String, Task<?>> runningTaskMap,
			final BlockingQueue<Result> resultQueue);

	/**
	 * Process the Result. Call from Space Proxy in Universe.
	 * 
	 * @param universe
	 *            Universe
	 * @param runningTaskMap
	 *            The running Task Map in the Space Proxy, where the associated
	 *            task is stored.
	 */
	public abstract void process(final UniverseImpl universe,
			final Map<String, Task<?>> runningTaskMap);

	/**
	 * Output format of Result runtime.
	 */
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getClass());
		stringBuilder.append("\n\tExecution time:\t").append(getTaskRuntime());
		return stringBuilder.toString();
	}

	/**
	 * Get Computer Count
	 * 
	 * @return the computerCount
	 */
	public Integer getComputerCount() {
		return ComputerCount;
	}

	/**
	 * Set computer count
	 * 
	 * @param computerCount
	 *            the computerCount to set
	 */
	public void setComputerCount(Integer computerCount) {
		ComputerCount = computerCount;
	}

	/**
	 * Get Space Task Count
	 * 
	 * @return the spaceCount
	 */
	public Integer getSpaceCount() {
		return spaceCount;
	}

	/**
	 * Set Space Task Count
	 * 
	 * @param spaceCount
	 *            the spaceCount to set
	 */
	public void setSpaceCount(Integer spaceCount) {
		this.spaceCount = spaceCount;
	}

	/**
	 * Get Computer status
	 * 
	 * @return the computerIsBusy True if computer is busy. False otherwise.
	 */
	public Boolean getComputerIsBusy() {
		return computerIsBusy;
	}

	/**
	 * Set Computer status
	 * 
	 * @param computerIsBusy
	 *            the computerIsBusy to set
	 */
	public void setComputerIsBusy(Boolean computerIsBusy) {
		this.computerIsBusy = computerIsBusy;
	}

	/**
	 * Get Space status
	 * 
	 * @return the spaceIsBusy True if the Space is busy. False otherwise
	 */
	public Boolean getSpaceIsBusy() {
		return spaceIsBusy;
	}

	/**
	 * Set Space status
	 * 
	 * @param spaceIsBusy
	 *            the spaceIsBusy to set
	 */
	public void setSpaceIsBusy(Boolean spaceIsBusy) {
		this.spaceIsBusy = spaceIsBusy;
	}

}
