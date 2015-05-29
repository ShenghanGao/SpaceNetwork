package tsp_tasks;

import java.io.Serializable;
import java.util.*;

/**
 * <b>TspData</b> is a class designed to store the specific information
 * in a {@link TspReadyTask} object. The way to deal with a <b>TaskTsp</b> object
 * is decided by the <i>args</i> in it, which is a list of <b>TspData</b> objects.
 */
public class TspData implements Serializable {
	
	private static final long serialVersionUID = -1662829519219063057L;
	
	/**
	 * The distance of the tour, related to the order of the cities
	 */
	private final double distance;
	
	/**
	 * The order of the cities whose sequence is already decided
	 */
	private final List<Integer> orderedCities;
	
	/**
	 * The cities whose order has not been decided yet
	 */
	private final List<Integer> unorderedCities;
	
	/**
	 * The class constructor initializing the fields.
	 * 
	 * @param distance
	 * The distance of the tour, when the object is created, it
	 * can be set to a fixed value, say, -1 or -8. The field will
	 * be modified when the distance calculation is done.
	 * @param ORDERED
	 * The order of the cities whose sequence is already decided.
	 * The first city in the list is where the tour starts.
	 * @param UNORDERED
	 * The cities whose order has not been decided yet. The order of
	 * the elements in this list does not matter.
	 */
	public TspData(double distance, List<Integer> ORDERED, List<Integer> UNORDERED) {
		orderedCities = new ArrayList<Integer> ();
		unorderedCities = new ArrayList<Integer> ();
		
		this.distance = distance;
		orderedCities.addAll(ORDERED);
		unorderedCities.addAll(UNORDERED);
	}
	
	/**
	 * Gets the distance of the tour.
	 * 
	 * @return
	 * the distance of the tour
	 */
	public double getDistance() {
		return distance;
	}
	
	/**
	 * Gets the ordered cities list.
	 * 
	 * @return
	 * a list of the ordered cities
	 */
	public List<Integer> getOrderedCities() {
		return orderedCities;
	}

	/**
	 * Gets the unordered cities list.
	 * 
	 * @return
	 * a list of the unordered cities
	 */
	public List<Integer> getUnorderedCities() {
		return unorderedCities;
	}
}
