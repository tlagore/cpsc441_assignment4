package cpsc441_assignment4;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Timer;

import cpsc441.a4.shared.DvrPacket;
import cpsc441.a4.shared.RtnTable;

/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 * A separate process broadcasts routing update messages
 * to directly connected neighbors at regular intervals.
 * 
 *      
 * @author Tyrone Lagore
 * @version	2.2
 *
 */
public class Router {
	private int _ID;
	private int _UpdateInterval;
	private int _ServerPort;
	private String _ServerName;
	private Timer _UpdateTimer;
	private Socket _RelayServerSocket;
	private ReceiverThread _RcvrThread;
	private ObjectOutputStream _OutputStream;
	private ObjectInputStream _InputStream;
	
	private boolean _Quit;
	private int[] _LinkCost;
	private int[] _NextHop; 
	private int[][] _DistanceVector;

	/**
	 * Constructor to initialize the router instance 
	 * 
	 * @param routerId			Unique ID of the router starting at 0
	 * @param serverName		Name of the host running the network server
	 * @param serverPort		TCP port number of the network server
	 * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
	 */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		_ID = routerId;
		_UpdateInterval = updateInterval;
		_ServerName = serverName;
		_ServerPort = serverPort;
		_UpdateTimer = new Timer();
		_Quit = false;
	}

	/**
	 * Called by the UpdateTimer thread to inform the router that the update interval has expired and needs to be 
	 * rerun.
	 */
	public synchronized void broadcastCost()
	{
		DvrPacket packet;
		
		for(int i = 0; i < _NextHop.length; i++)
		{
			if(i != _ID && _LinkCost[i] != 999)
			{
				packet = new DvrPacket(_ID, i, DvrPacket.ROUTE, _LinkCost);
				try{
					_OutputStream.writeObject(packet);
					_OutputStream.flush();
				}catch(Exception ex)
				{
					System.out.println("Failed to send MinCost packet to router " + i + ". Exception message: " + ex.getMessage());
				}
			}
		}
	}

	/**
	 * 
	 */
	public void tcpHandshake()
	{
		DvrPacket hello = new DvrPacket(_ID, DvrPacket.SERVER, DvrPacket.HELLO);
		DvrPacket hi = null;
		
		try{
			_RelayServerSocket = new Socket(_ServerName, _ServerPort);
			_OutputStream = new ObjectOutputStream(_RelayServerSocket.getOutputStream());
			_InputStream = new ObjectInputStream(_RelayServerSocket.getInputStream());
		
			while(hi == null)
			{
				_OutputStream.writeObject(hello);
				_OutputStream.flush();
				
				try{					
					hi = (DvrPacket)_InputStream.readObject();
				}catch(Exception ex)
				{
					System.out.println("Failed to get response 'hello' packet: " + ex.getMessage());		
				}
			}

			processDvr(hi);
			
		}catch(IOException ex)
		{
			System.out.println(ex.getMessage());
		}
	}	
	
	/**
	 * abnormalShutdown allows the router to know if a thread has malfunctioned. In the case of this router
	 * implementation, the router will simply shutdown. If the router was to be of a more sophisticated design,
	 * the function might take in some parameters to find out where the error occurred and attempt to remedy
	 * the issue.
	 */
	public synchronized void abnormalShutdown()
	{
		_Quit = true;
		_UpdateTimer.cancel();
		System.out.println("Abnormal shutdown initialized. Router shutting down.");
	}
	
	
	/**
	 * updateTopology cancels the current broadcast timer, updates the topology using initTopology, then 
	 * begins the broadcast timer again.
	 * @param min
	 */
	private void updateTopology(int [] min)
	{
		_UpdateTimer.cancel();
		_UpdateTimer = new Timer();
		
		initTopology(min);
		_UpdateTimer.scheduleAtFixedRate(new UpdateTimer(this), 1000, _UpdateInterval);
	}
	
	/**
	 * processDvr handles all received Dvrs. Based on the packet received, the router will either initialize it's
	 * topology, update its topology, calculate new minimum distances, or quit (which handles cleanup).
	 * 
	 * @param pkt the packet to be processed
	 */
	public synchronized void processDvr(DvrPacket pkt)
	{	
		if(pkt.sourceid == DvrPacket.SERVER)
		{
			switch(pkt.type)
			{
				case DvrPacket.HELLO:
					initTopology(pkt.getMinCost());
					break;
				case DvrPacket.ROUTE:
					updateTopology(pkt.getMinCost());
					break;
				case DvrPacket.QUIT:
					if(_RcvrThread != null)
						_RcvrThread.shutdown();
					
					_UpdateTimer.cancel();
					_Quit = true;
					System.out.println("Quit!");
					break;
			}
		}
		else
		{
			if(pkt.type == DvrPacket.ROUTE)
			{
				_DistanceVector[pkt.sourceid] = pkt.getMinCost();
				computeLinkState();
			}
		}
	}
	
	/**
	 * initDistanceVecotr is an internal initialization method that initializes a default Distance Vector with all
	 * values set to infinity, and a value of 0 from the router to itself. Minimum distances should be set after this call.
	 * 
	 * @param numRouters The number of routers in the network topology
	 */
	private void initDistanceVector(int numRouters)
	{
		_DistanceVector = new int[numRouters][numRouters];
		for(int i = 0; i < numRouters; i++)
		{
			for(int j = 0; j < numRouters; j++)
				_DistanceVector[i][j] = DvrPacket.INFINITY;
		}
		
		_DistanceVector[_ID][_ID] = 0;
	}
	
	/**
	 * initTopology initializes a new network topology based on the routers minimum cost to all routers on the network
	 * (infinity if not directly linked).
	 * 
	 * @param minCost The minimum cost of this router to all of the routers in the network (infinity if not a neighbour)
	 */
	private synchronized void initTopology(int[] minCost)
	{
		int numRouters = minCost.length;
		_NextHop = new int[numRouters];
		_DistanceVector = new int[numRouters][numRouters];
		
		_LinkCost = Arrays.copyOf(minCost, minCost.length);
		
		_NextHop[_ID] = _ID;		
		
		initDistanceVector(numRouters);
		_DistanceVector[_ID] = Arrays.copyOf(minCost, minCost.length);
		
		for(int i = 0; i < numRouters; i++){
			if(minCost[i] == 999)
				_NextHop[i] = -1;	
			else
				_NextHop[i] = i;
		}
	}
	
	/**
	 * computeLinkState implements the Bellman Ford link state algorithm to create a distance vector that 
	 * is the minimum cost for each router to each router within the network based on the cost of it's 
	 * neighboring routers to other routers within the network.
	 * 
	 */
	private void computeLinkState()
	{
		int numRouters = _LinkCost.length;
		int min = DvrPacket.INFINITY;
		
		for(int i = 0; i < numRouters; i++)
		{
			if(i != _ID)
			{
				for(int j = 0; j < numRouters; j++){
					if(j != _ID){
						if (_DistanceVector[_ID][j] + _DistanceVector[j][i] < _DistanceVector[_ID][i])
							_NextHop[i] = _NextHop[j];
						min = Math.min(_DistanceVector[_ID][j] + _DistanceVector[j][i], min);
					}
				}
				if(min < _DistanceVector[_ID][i]){
					_DistanceVector[_ID][i] = min;
				}

				min = DvrPacket.INFINITY;
			}
		}
	}
	
	/**
	 * starts the router 
	 * 
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {
		RtnTable rtnTable;
		
		tcpHandshake();
		
		_RcvrThread = new ReceiverThread(this, _InputStream);
		_UpdateTimer.scheduleAtFixedRate(new UpdateTimer(this), 1000, _UpdateInterval);
		
		_RcvrThread.start();
		
		while(!_Quit){
			Thread.yield();
		}
		
		rtnTable = new RtnTable(_DistanceVector[_ID], _NextHop);
		return rtnTable;
	}
	
	
	/* *********************************************************************************
	 * 														Test Functions														*
	 * *********************************************************************************/
	private void displayRouterInfo()
	{
		int i, j;
		
		System.out.println("Router with ID " + _ID);
		System.out.println("----------------------------");
		
		System.out.println("LinkCost");
		for(i = 0; i < _LinkCost.length; i++)
			System.out.print(_LinkCost[i] + ", ");
		
		System.out.println();
		System.out.println("NextHop");
		for(i = 0; i < _NextHop.length; i++)
			System.out.print(_NextHop[i] + ", ");
		
		System.out.println();
		System.out.println("DistanceVector");
		for(i = 0; i < _DistanceVector[0].length; i++)
		{
			for(j = 0; j < _DistanceVector[0].length; j++)
				System.out.print(_DistanceVector[i][j] + "\t");
			
			System.out.println();
		}
		
		System.out.println("----------------------------");
	}

	public void testRouter(DvrPacket[] pckts)
	{
		for (int i = 0; i < pckts.length; i++)
		{
			processDvr(pckts[i]);
			displayRouterInfo();
			try{
				
				Thread.sleep(5000);
			}catch(Exception ex)
			{
				System.out.println("Error in testRouter");
			}
		}
	}

	
	@SuppressWarnings("unused")
	private void test1()
	{
		int[] R0 = new int[]{0, 1, 7, 999};
		int[] R1 = new int[]{1, 0, 1, 999};
		int[] R2 = new int[]{7, 1, 0, 1 };
		int[] R3 = new int[]{999, 999, 1, 0};
		
		
		DvrPacket pckt0 = new DvrPacket(DvrPacket.SERVER, 0, DvrPacket.HELLO, R0);
		DvrPacket pckt1 = new DvrPacket(1, 0, DvrPacket.ROUTE, R1);
		DvrPacket pckt2 = new DvrPacket(2, 0, DvrPacket.ROUTE, R2);
		DvrPacket pckt3 = new DvrPacket(3, 0, DvrPacket.ROUTE, R3);
		
		DvrPacket[] pckts = new DvrPacket[]{pckt0, pckt1, pckt2, pckt3};

		testRouter(pckts);
	}
}
