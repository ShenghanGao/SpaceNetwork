package api;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Universe extends Remote {
	/**
	 * The port used by the RMI registry.
	 */
	public static int PORT = 8000;

	/**
	 * The service name associated with this Remote interface.
	 */
	public static String SERVICE_NAME = "Universe";

	/**
	 * Register a Server in Universe. Call from Server.
	 * 
	 * @param server
	 *            Server to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Universe.
	 */
	void register(final Server server) throws RemoteException;

	/**
	 * Register a Space in Universe. Call from Space.
	 * 
	 * @param Space
	 *            Space to be registered.
	 * @throws RemoteException
	 *             Cannot connect with Universe.
	 */
	void register(final Space space) throws RemoteException;
	void printSuccessors() throws RemoteException;
}
