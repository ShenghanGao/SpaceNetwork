package client;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import api.Client;
import api.Server;
import api.Task;
import result.ValueResult;
import tsp_tasks.TspData;
import tsp_tasks.TspReadyTask;

/**
 * TSP Client.
 *
 */
public class ClientTsp extends Client<List<Integer>, double[][]> {
	private static final long serialVersionUID = 4192126821917742620L;
	private static final int NUM_PIXALS = 600;

	public ClientTsp(String clientName) {
		super("TSP", clientName);
	}

	/**
	 * Prepare a TSP Ready Task
	 * 
	 * @param cities
	 *            Cities
	 * @return TSP Ready Task
	 */
	@Override
	public Task<TspData> makeTask(double[][] cities) {
		double[][] distance = calDistance(cities);
		final int numOfCities = cities.length;
		List<Integer> ordered = new ArrayList<Integer>();
		ordered.add(0);
		List<Integer> unordered = new ArrayList<Integer>();
		for (int i = 1; i < numOfCities; ++i)
			unordered.add(i);
		TspData data = new TspData(-8, ordered, unordered);
		List<TspData> args = new ArrayList<TspData>();
		args.add(data);
		return new TspReadyTask(args, numOfCities, distance);
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		System.setSecurityManager(new SecurityManager());
		String serverDomainName = args.length == 0 ? "localhost" : args[0];
		String clientName = args.length == 2 ? args[1] : "TSP";
		ClientTsp client = new ClientTsp(clientName);
		client.begin();

		// double[][] CITIES = {
		// { 6, 3 },
		// { 2, 2 },
		// { 5, 8 },
		// { 1, 5 },
		// { 1, 6 },
		// { 2, 7 },
		// { 2, 8 },
		// { 6, 5 },
		// { 1, 3 },
		// { 6, 6 }
		// };

		double[][] CITIES = {
				{ 1, 1 },
				// { 8, 1 },
				// { 8, 8 },
				// { 1, 8 },
				{ 2, 2 }, { 7, 2 }, { 7, 7 }, { 2, 7 }, { 3, 3 }, { 6, 3 },
				{ 6, 6 }, { 3, 6 }, { 4, 4 }, { 5, 4 }, { 5, 5 }, { 4, 5 } };

		Task<TspData> tspTask = client.makeTask(CITIES);
		try {
			Server server = client.findServer(serverDomainName);
			if (!server.register(client.getName(), null)) {
				System.out.println("Failed to register in Server");
			} else {
				String taskID = server.submit(tspTask, client.getName());
				Logger.getLogger(Client.class.getCanonicalName()).log(
						Level.INFO, "Task: TSP({0}) is submitted. ID is {1}",
						new Object[] { CITIES.length, taskID });
				ValueResult<TspData> result = (ValueResult<TspData>) server
						.getResult(client.getName());
				TspData tspData = result.getResultValue();
				List<Integer> minTour;
				minTour = tspData.getOrderedCities();
				minTour.add(0);
				client.add(client.getLabel(minTour.toArray(new Integer[0]),
						CITIES));
			}
			server.unregister(client.getName());
		} catch (MalformedURLException | NotBoundException e) {
			System.out.println("Bad Server domain name!");
		} catch (RemoteException e) {
			System.out.println("Cannot regiseter to the Server!");
		}
		client.end();
	}

	private String tourToString(Integer[] cities) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Tour: ");
		for (Integer city : cities) {
			stringBuilder.append(city).append(' ');
		}
		return stringBuilder.toString();
	}

	private static double[][] calDistance(double[][] CITIES) {
		int numOfCities = CITIES.length;
		double[][] distance = new double[numOfCities][numOfCities];

		for (int i = 0; i < numOfCities; ++i)
			for (int j = 0; j < numOfCities; ++j)
				distance[i][j] = Math.sqrt(Math.pow(
						CITIES[i][0] - CITIES[j][0], 2)
						+ Math.pow(CITIES[i][1] - CITIES[j][1], 2));
		return distance;
	}

	public JLabel getLabel(final Integer[] tour, final double[][] cities) {
		Logger.getLogger(ClientTsp.class.getCanonicalName()).log(Level.INFO,
				tourToString(tour));
		// display the graph graphically, as it were
		// get minX, maxX, minY, maxY, assuming they 0.0 <= mins
		double minX = cities[0][0], maxX = cities[0][0];
		double minY = cities[0][1], maxY = cities[0][1];
		for (double[] city : cities) {
			if (city[0] < minX)
				minX = city[0];
			if (city[0] > maxX)
				maxX = city[0];
			if (city[1] < minY)
				minY = city[1];
			if (city[1] > maxY)
				maxY = city[1];
		}

		// scale points to fit in unit square
		final double side = Math.max(maxX - minX, maxY - minY);
		double[][] scaledCities = new double[cities.length][2];
		for (int i = 0; i < cities.length; i++) {
			scaledCities[i][0] = (cities[i][0] - minX) / side;
			scaledCities[i][1] = (cities[i][1] - minY) / side;
		}

		final Image image = new BufferedImage(NUM_PIXALS, NUM_PIXALS,
				BufferedImage.TYPE_INT_ARGB);
		final Graphics graphics = image.getGraphics();

		final int margin = 10;
		final int field = NUM_PIXALS - 2 * margin;
		// draw edges
		graphics.setColor(Color.BLUE);
		int x1, y1, x2, y2;
		int city1 = tour[0], city2;
		x1 = margin + (int) (scaledCities[city1][0] * field);
		y1 = margin + (int) (scaledCities[city1][1] * field);
		for (int i = 1; i < cities.length; i++) {
			city2 = tour[i];
			x2 = margin + (int) (scaledCities[city2][0] * field);
			y2 = margin + (int) (scaledCities[city2][1] * field);
			graphics.drawLine(x1, y1, x2, y2);
			x1 = x2;
			y1 = y2;
		}
		city2 = tour[0];
		x2 = margin + (int) (scaledCities[city2][0] * field);
		y2 = margin + (int) (scaledCities[city2][1] * field);
		graphics.drawLine(x1, y1, x2, y2);

		// draw vertices
		final int VERTEX_DIAMETER = 6;
		graphics.setColor(Color.RED);
		for (int i = 0; i < cities.length; i++) {
			int x = margin + (int) (scaledCities[i][0] * field);
			int y = margin + (int) (scaledCities[i][1] * field);
			graphics.fillOval(x - VERTEX_DIAMETER / 2, y - VERTEX_DIAMETER / 2,
					VERTEX_DIAMETER, VERTEX_DIAMETER);
		}
		final ImageIcon imageIcon = new ImageIcon(image);
		return new JLabel(imageIcon);
	}

}
