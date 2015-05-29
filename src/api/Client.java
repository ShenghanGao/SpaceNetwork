package api;

import java.awt.BorderLayout;
import java.awt.Container;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

public abstract class Client<T,A> extends JFrame {
	private static final long serialVersionUID = -4472984886617837870L;
	protected T taskReturnValue;
	private long clientStartTime;
	private String name;

	/**
	 * Constructor of Client.
	 * 
	 * @param title
	 *            Frame title.
	 * @param clientName
	 *            Client Name
	 */
	public Client(final String title, final String clientName) {
		setTitle(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.name = clientName;
	}

	/**
	 * Get Client Name.
	 * 
	 * @return Client Name.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Prepare a Client Task.
	 * @param <A>
	 * 
	 * @return Task
	 */
	public abstract Task<?> makeTask(A arg);

	/**
	 * Find a Server.
	 * 
	 * @param serverDomainName
	 *            Server Domain Name.
	 * @return
	 * @throws MalformedURLException
	 *             Bad Server Domain Name.
	 * @throws RemoteException
	 *             Cannnot connect to Server.
	 * @throws NotBoundException
	 *             Bad Server Domain Name.
	 */
	public Server findServer(String serverDomainName)
			throws MalformedURLException, RemoteException, NotBoundException {
		String url = "rmi://" + serverDomainName + ":" + Server.PORT + "/"
				+ Server.SERVICE_NAME;
		return (Server) Naming.lookup(url);
	}

	/**
	 * Start counting time.
	 */
	public void begin() {
		clientStartTime = System.nanoTime();
	}

	/**
	 * Stop counting time
	 */
	public void end() {
		Logger.getLogger(this.getClass().getCanonicalName()).log(Level.INFO,
				"Client time: {0} ms.",
				(System.nanoTime() - clientStartTime) / 1000000);
	}

	/**
	 * Add Graph
	 * 
	 * @param jLabel
	 *            label
	 */
	public void add(final JLabel jLabel) {
		final Container container = getContentPane();
		container.setLayout(new BorderLayout());
		container.add(new JScrollPane(jLabel), BorderLayout.CENTER);
		pack();
		setVisible(true);
	}

}
