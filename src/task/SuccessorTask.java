package task;

import java.util.List;

import api.Task;

public abstract class SuccessorTask<T> extends Task<T> {
	private static final long serialVersionUID = 7837344432049161528L;
	/**
	 * Target Successor Task's Argument Index.
	 */
	private int targetSuccessorTaskArgIndex;
	/**
	 * Number of missing arguments
	 */
	private int missingArgNum;

	/**
	 * Origin Task ID
	 */
	private String originID;

	/**
	 * Constructor of successor task. Call from inside.
	 * 
	 * @param arg
	 *            List of needed arguments. All null when initialized.
	 * @param argNum
	 *            Number of needed arguments.
	 * @param targetSuccessorTaskId
	 *            Target successor task Id.
	 * @param targetSuccessorTaskArgIndex
	 *            Target argument index of target successor task.
	 */
	public SuccessorTask(List<T> arg, int argNum, String targetSuccessorTaskId,
			int targetSuccessorTaskArgIndex) {
		super(arg);
		this.missingArgNum = argNum;
		this.originID = null;
		this.setTargetID(targetSuccessorTaskId);
		this.setTargetSuccessorTaskArgIndex(targetSuccessorTaskArgIndex);
	}

	/**
	 * Check if the task has all needed arguments and ready to run.
	 * 
	 * @return True if the task is ready to run. False otherwise.
	 */
	synchronized public boolean isRunnable() {
		if (missingArgNum == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Set the argument at the index position.
	 * 
	 * @param Index
	 *            Position of the argument to add.
	 * @param value
	 *            Argument value.
	 */
	synchronized public void setArgAt(int Index, T value) {
		if (arg.get(Index) == null) {
			((List<T>) arg).set(Index, value);
			missingArgNum--;
		}
	}

	/**
	 * Get the argument index of the target successor task.
	 * 
	 * @return the targetSuccessorTaskArgIndex
	 */
	public int getTargetSuccessorTaskArgIndex() {
		return targetSuccessorTaskArgIndex;
	}

	/**
	 * Set the argument index of the target successor task.
	 * 
	 * @param targetSuccessorTaskArgIndex
	 *            the targetSuccessorTaskArgIndex to set
	 */
	public void setTargetSuccessorTaskArgIndex(int targetSuccessorTaskArgIndex) {
		this.targetSuccessorTaskArgIndex = targetSuccessorTaskArgIndex;
	}

	/**
	 * Get Origin Task ID
	 * 
	 * @return Origin Task ID
	 */
	public String getOriginID() {
		return originID;
	}

	/**
	 * Set Origin Task ID
	 * 
	 * @param originID
	 *            Origin Task ID
	 */
	public void setOrginID(String originID) {
		this.originID = originID;
	}

}
