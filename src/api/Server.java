package api;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Server extends Remote {
	/**
	 * The port used by the RMI registry.
	 */
	public static int PORT = 8001;

	/**
	 * The service name associated with this Remote interface.
	 */
	public static String SERVICE_NAME = "Server";

	/**
	 * Set an ID to the Server. Call from Universe.
	 * 
	 * @param serverID
	 *            Server Id.
	 * @throws RemoteException
	 *             Failed to connect to Server.
	 */
	public void setID(final int serverID) throws RemoteException;

	/**
	 * Get a Task from Ready Task Queue. Call from Server Proxy in Universe.
	 * 
	 * @return Task
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	public Task<?> getTask() throws RemoteException;

	/**
	 * Dispatch the Result to corresponding Client Proxy. Call from Server Proxy
	 * in Universe. If the Client is down, discard the result.
	 * 
	 * @param result
	 *            Result to be dispatched.
	 * @throws RemoteExcemption
	 *             Cannot connect with Server.
	 */
	public void dispatchResult(final Result result) throws RemoteException;

	/**
	 * Register a Client in Server. Call from Client.
	 * 
	 * @param clientname
	 *            Client to be registered.
	 * @return Status of register.
	 * @throws RemoteException
	 *             Cannot connect with Server.
	 */
	public boolean register(final String clientname, final String duration)
			throws RemoteException;

	/**
	 * Unregister a Client in Server. Call from Client.
	 * 
	 * @param clientname
	 *            Client to be registered.
	 * @return Status of register.
	 * @throws RemoteException
	 *             Cannot connect with Server
	 */
	public boolean unregister(final String clientname) throws RemoteException;

	/**
	 * Add a Task to the Ready Task Queue. Call from Client
	 * 
	 * @param task
	 *            Task to be submitted.
	 * @return Task ID
	 * @throws RemoteException
	 *             Cannot connect with Server
	 */
	public String submit(final Task<?> task, final String clientname)
			throws RemoteException;

	/**
	 * Get the Result of a Task.
	 * 
	 * @param clientname
	 *            Client Name
	 * @return Result
	 * @throws RemoteException
	 *             Cannot connect with Server
	 */
	public Result getResult(final String clientname) throws RemoteException;

	public ArrayList<String> check() throws RemoteException;
}
