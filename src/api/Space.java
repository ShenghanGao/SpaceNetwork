package api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * 
 * Space interface is exposed to both Computer and Client.
 *
 */
public interface Space extends Remote {
	/**
	 * The port used by the RMI registry.
	 */
	public static int PORT = 8002;

	/**
	 * The service name associated with this Remote interface.
	 */
	public static String SERVICE_NAME = "Space";

	/**
	 * Set an ID to the Space. Call from Universe.
	 * 
	 * @param spaceID
	 *            Space Id.
	 * @throws RemoteException
	 *             Failed to connect to Space.
	 */
	public void setID(final int spaceID) throws RemoteException;

	/**
	 * Add a Task to the Ready Task Queue. Call from Space Proxy in Universe.
	 * 
	 * @param task
	 *            The Ready/Successor Task to be submitted.
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	public void addTask(final Task<?> task) throws RemoteException;

	/**
	 * Get Result from Result Queue. Call from Space Proxy in Universe.
	 * 
	 * @return Result
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	public Result getResult() throws RemoteException;

	/**
	 * Register a computer in space. Call from Computer.
	 * 
	 * @param computer
	 *            Computer to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	public void register(final Computer computer) throws RemoteException;

	/**
	 * Restart Space. Call from Universe.
	 * 
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	public void restart() throws RemoteException;

	/**
	 * Check status of Space. Call from Universe.
	 * 
	 * @return Status of Space
	 * @throws RemoteException
	 *             Cannot connect with Space.
	 */
	public ArrayList<String> check() throws RemoteException;

}
