package result;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import api.Result;
import api.Task;
import config.Config;
import space.SpaceImpl;
import task.SuccessorTask;
import universe.UniverseImpl;

/**
 * 
 * Value Result is to contain value result of a task execution.
 * 
 * @param <ValueType>
 *            is the result value type.
 */
public class ValueResult<ValueType> extends Result {
	private static final long serialVersionUID = -4600193385843282656L;

	/**
	 * Target Successor Task Id of this value result.
	 */
	private String targetTaskId;

	/**
	 * Target Successor Argument Index of this value result.
	 */
	private int targetArgIndex;

	/**
	 * The value result.
	 */
	private final ValueType value;

	/**
	 * Successor's Orgin Task ID.
	 */
	private String orginTaskID;

	/**
	 * Constructor of value result.
	 * 
	 * @param resultId
	 *            Result Id, same as its associated task Id.
	 * @param layer
	 *            layer
	 * @param value
	 *            Result value.
	 * @param TargetTaskId
	 *            Id of target successor task waiting for the result.
	 * @param TargetArgIndex
	 *            Argument Index of the result value for target successor task.
	 * @param taskStartTime
	 *            Task start time.
	 * @param taskEndTime
	 *            Task End time.
	 */
	public ValueResult(String resultId, ValueType value, String targetTaskId,
			int targetArgIndex, boolean coarse, long taskStartTime,
			long taskEndTime) {
		super(resultId, VALUERESULT, coarse, taskStartTime, taskEndTime);
		this.value = value;
		this.targetTaskId = targetTaskId;
		this.targetArgIndex = targetArgIndex;
		this.setOrginTaskID(null);
	}

	/**
	 * Get the target successor task ID.
	 * 
	 * @return the targetTaskId
	 */
	public String getTargetTaskID() {
		return this.targetTaskId;
	}

	/**
	 * Get the target argument index of the target successor task.
	 * 
	 * @return the targetArgIndex
	 */
	public int getTargetArgIndex() {
		return this.targetArgIndex;
	}

	/**
	 * Get the result value.
	 * 
	 * @return result value
	 */
	public ValueType getResultValue() {
		return this.value;
	}

	/**
	 * Check if the value result is the final result. If yes, put the result
	 * into Space's Result Queue.
	 * 
	 * @return True if it is the final result. False otherwise.
	 */
	public boolean isFinal() {
		return this.getID().startsWith("INI:");
	}
	
	/**
	 * Get Successor's Orgin Task ID
	 * 
	 * @return the orginTaskID
	 */
	public String getOrginTaskID() {
		return orginTaskID;
	}

	/**
	 * Set Origin Task ID
	 * 
	 * @param orginTaskID
	 *            the orginTaskID to set
	 */
	public void setOrginTaskID(String orginTaskID) {
		this.orginTaskID = orginTaskID;
	}

	/**
	 * Process the result. Call from Space.
	 * 
	 * @param space
	 *            The Space implementation in which the result is to be
	 *            processed.
	 * @param runningTaskMap
	 *            The Running Task Map in the Computer Proxy, where the
	 *            associated task is stored.
	 * @param intermediateResultQueue
	 *            Intermediate Result Queue in which the result can be stored
	 *            after Space Direct Execution.
	 * @return True if process successfully. False if the target successor is in
	 *         Universe
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean process(SpaceImpl space,
			Map<String, Task<?>> runningTaskMap,
			BlockingQueue<Result> intermediateResultQueue) {

		// Get the result's target successor task.
		SuccessorTask<ValueType> successortask = (SuccessorTask<ValueType>) space
				.getSuccessorTask(targetTaskId);
		if (successortask == null) {
			return false;
		}

		// Set the argument in the target successor task at the target index.
		successortask.setArgAt(targetArgIndex, this.value);

		// Check if the successor task has all needed arguments and ready to
		// run.
		if (successortask.isRunnable()) {
			// Check if the successor task can be run in Space.
			if (Config.SpaceExecutTaskFlag && successortask.isSpaceRunnable()
					&& (Math.random() < 0.5)) {
				// Space execute this successor task and store the result into
				// Computer Proxy Temporary Result Queue.
				space.spaceExecuteTask(successortask, intermediateResultQueue);
			} else {
				// The successor task is moved from Successor Task Queue to
				// Ready Task Queue.
				space.successorToReady(successortask);
			}
		}
		return true;
	}

	/**
	 * Process the result. Call from Universe.
	 * 
	 * @param universe
	 *            The universe implemetation in which the result is to be
	 *            processed.
	 * @param RunningTaskMap
	 *            The Running Task Map in the Space Proxy, where the associated
	 *            task is stored.
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void process(final UniverseImpl universe,
			final Map<String, Task<?>> runningTaskMap) {
		// If the result is final, dispatch it.
		if (isFinal()) {
			universe.dispatchResult(this);
			return;
		}
		// Get the result's target successor task. If the task is unavailabe at
		// the moment, put the result into Temporary Result Queue in Computer
		// Proxy.
		SuccessorTask<ValueType> successortask = (SuccessorTask<ValueType>) universe
				.getSuccessorTask(targetTaskId);
		if (Config.DEBUG) {
			if (successortask == null){
				return;
			}
		}
		// Set the argument in the target successor task at the target index.
		successortask.setArgAt(targetArgIndex, this.value);
		if (successortask.isRunnable()) {
			// The successor task is moved from Successor Task Queue to
			// Ready Task Queue.
			universe.successorToReady(successortask);
		}
	}
}
