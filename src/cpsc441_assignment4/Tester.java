package cpsc441_assignment4;

import cpsc441.a4.shared.RtnTable;

public class Tester {		
	    /**
	     * A simple test driver
	     * 
	     */
		public static void main(String[] args) {
			// default parameters
			int routerId = 0;
			String serverName = "localhost";
			int serverPort = 2227;
			int updateInterval = 1000; //milli-seconds
			
			// the router can be run with:
			// i. a single argument: router Id
			// ii. all required arquiments
			if (args.length == 1) {
				routerId = Integer.parseInt(args[0]);
			}
			else if (args.length == 4) {
				routerId = Integer.parseInt(args[0]);
				serverName = args[1];
				serverPort = Integer.parseInt(args[2]);
				updateInterval = Integer.parseInt(args[3]);
			}
			else {
				System.out.println("incorrect usage, try again.");
				System.exit(0);
			}
				
			// print the parameters
			System.out.printf("starting Router #%d with parameters:\n", routerId);
			System.out.printf("Relay server host name: %s\n", serverName);
			System.out.printf("Relay server port number: %d\n", serverPort);
			System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);
			
			// start the server
			// the start() method blocks until the router receives a QUIT message
			Router router = new Router(routerId, serverName, serverPort, updateInterval);
			RtnTable rtn = router.start();
			System.out.println("Router terminated normally");
			
			// print the computed routing table
			System.out.println();
			System.out.println("Routing Table at Router #" + routerId);
			System.out.print(rtn.toString());
		}
}

