package task;

import java.util.List;

import api.Task;

/**
 * 
 * A Ready Task is a task that is ready to run. In Divide and Conquer context,
 * it is a Decompose task, which, after execution, either generates a Task
 * Result (further Decompose tasks and Compose tasks) or generates a Value
 * Result (Send argument to a Compose task).
 *
 * @param <T>
 *            Argument type of the task.
 */
public abstract class ReadyTask<T> extends Task<T> {
	private static final long serialVersionUID = -998071752240333400L;
	
	/**
	 * Target Successor Task's Argument Index.
	 */
	protected int targetSuccessorTaskArgIndex;
	
	/**
	 * Constructor of Ready Task.
	 * 
	 * @param arg
	 *            List of arguments.
	 * @param targetSuccessorTaskArgIndex
	 *            Target Successoor Task Argument Index.
	 */
	public ReadyTask(List<T> arg, int targetSuccessorTaskArgIndex) {
		super(arg);
		this.targetSuccessorTaskArgIndex = targetSuccessorTaskArgIndex;
	}

	/**
	 * Constructor of Ready Task. Call from Client.
	 * 
	 * @param arg
	 *            Argument List.
	 */
	public ReadyTask(List<T> arg) {
		super(arg);
		this.targetSuccessorTaskArgIndex = -1;
	}

	/**
	 * Check if the task cannot be further decomposed or not.
	 * 
	 * @return True is the task is atomic and cannot be further decomposed.
	 *         False otherwise.
	 */
	public abstract boolean isAtomic();

	/**
	 * 
	 * Get the target successor task's argument index.
	 * 
	 * @return the targetSuccessorTaskArgIndex
	 */
	public int getTargetSuccessorTaskArgIndex() {
		return targetSuccessorTaskArgIndex;
	}

	/**
	 * Set the target successor task argument's index.
	 * 
	 * @param targetSuccessorTaskArgIndex
	 *            the targetSuccessorTaskArgIndex to set
	 */
	public void setTargetSuccessorTaskArgIndex(int targetSuccessorTaskArgIndex) {
		this.targetSuccessorTaskArgIndex = targetSuccessorTaskArgIndex;
	}

}
