package config;

/**
 * 
 * Configuration of Space and Computer.
 *
 */
public abstract class Config {
	/**
	 * Flag of Amelioration of Communication.
	 */
	public final static boolean AmeliorationFlag = true;

	/**
	 * Flag of Space Direct Execution.
	 */
	public final static boolean SpaceExecutTaskFlag = true;

	/**
	 * Flag of Computer using mulitithread
	 */
	public final static boolean ComputerMultithreadFlag = true;

	/**
	 * Flag of Status Output
	 */
	public final static boolean STATUSOUTPUT = false;

	/**
	 * Flag of debug Output
	 */
	public static final boolean DEBUG = true;

	/**
	 * Max Time of a Client
	 */
	public static final int ClientTimeLimit = 3600;

	/**
	 * Default Time of a Client
	 */
	public static final int ClientTimeDefault = 65;

	/**
	 * Number of Takes to be cached. Start from 0.
	 */
	public static final int CacheTaskNum = 1;

	/**
	 * Computer Workload. Threashold of Ready Task Queue size.
	 */
	public static final int ComputerWorkload = 4;

}
