package api;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * Task is a data structure containing a task to be executed by Computer.
 *
 */
public abstract class Task<T> implements Serializable {
	private static final long serialVersionUID = -1946800150903851225L;
	
	/**
	 * Task Id, same as its assocaited resutl Id.
	 */
	private String taskID;

	/**
	 * Target Task Id. The successor task Id of this task.
	 */
	private String targetTaskID;

	/**
	 * Layer
	 */
	private int layer;

	/**
	 * Argument list.
	 */
	protected List<T> arg;

	/**
	 * Flag of whether the task can be run in Space or not. False by Default.
	 */
	private boolean isSpaceRunnable;

	/**
	 * Constructor of Task
	 * 
	 */
	public Task(List<T> arg) {
		this.arg = arg;
		this.layer = 0;
		this.taskID = null;
		this.targetTaskID = null;
		this.isSpaceRunnable = false;
	}

	/**
	 * Get the argument list.
	 * 
	 * @return the arg
	 */
	public List<T> getArg() {
		return arg;
	}

	/**
	 * Set the argument list.
	 * 
	 * @return the arg
	 */
	public void setArg(List<T> arg) {
		this.arg = arg;
	}

	/**
	 * Get layer.
	 * 
	 * @return layer
	 */
	public int getLayer() {
		return layer;
	}

	/**
	 * Set layer
	 * 
	 * @param layer
	 *            layer to be set to.
	 */
	public void setLayer(int layer) {
		this.layer = layer;
	}

	/**
	 * Get Task ID.
	 * 
	 * @return Task ID
	 */
	public String getID() {
		return taskID;
	}

	/**
	 * Set Task ID
	 * 
	 * @param taskId
	 *            Task ID to be set to.
	 */
	public void setID(String taskId) {
		this.taskID = taskId;
	}

	/**
	 * Get the target Task ID.
	 * 
	 * @return the targetTaskId
	 */
	public String getTargetID() {
		return targetTaskID;
	}

	/**
	 * Set the target task Id.
	 * 
	 * @param targetTaskId
	 *            the targetTaskId to set
	 */
	public void setTargetID(String targetTaskId) {
		this.targetTaskID = targetTaskId;
	}

	/**
	 * Check if the Task is Space Runnable.
	 * 
	 * @return True if it can be executed directly within the Space. False
	 *         otherwise.
	 */
	public boolean isSpaceRunnable() {
		return this.isSpaceRunnable;
	}

	/**
	 * Set the Space Runnable field.
	 * 
	 * @param isSpaceRunnable
	 *            Set whether the task can (True) run in Space or not(False).
	 */
	public void setSpaceRunnable(boolean isSpaceRunnable) {
		this.isSpaceRunnable = isSpaceRunnable;
	}

	/**
	 * Check if the Task is coarse or not.
	 * 
	 * @return True if the Task is coarse. False otherwise.
	 */
	public abstract boolean isCoarse();

	/**
	 * Execute the Task.
	 * 
	 * @return Result of the Task execution.
	 */
	public abstract Result execute();
}
