package cpsc441_assignment4;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
 * @author 	Majid Ghaderi
 * @version	2.1
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
	private int[] _MinCost;
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
		
		for(int i = 0; i < _MinCost.length; i++)
		{
			if(i != _ID)
			{
				packet = new DvrPacket(_ID, i, DvrPacket.ROUTE, _MinCost);
				try{
					_OutputStream.writeObject(packet);
					_OutputStream.flush();
				}catch(Exception ex)
				{
					System.out.println("Failed to send MinCost packet to router " + i + ". Exception message: " + ex.getMessage());
				}
			}
		}
		
		System.out.println("Broadcast!");
	}

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
	
	
	public synchronized void abnormalShutdown()
	{
		_Quit = true;
		_UpdateTimer.cancel();
		System.out.println("Abnormal shutdown initialized. Router shutting down.");
	}
	
	/**
	 * 
	 * @param pkt
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
				case DvrPacket.QUIT:
					if(_RcvrThread != null)
						_RcvrThread.shutdown();
					_Quit = true;
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
	 * 
	 * @param numRouters
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
	 * 
	 * @param minCost
	 */
	private void initTopology(int[] minCost)
	{
		int numRouters = minCost.length;
		_NextHop = new int[numRouters];
		_DistanceVector = new int[numRouters][numRouters];
		
		_MinCost = minCost;
		
		_NextHop[_ID] = _ID;		
		
		initDistanceVector(numRouters);
		_DistanceVector[_ID] = minCost;
		
		for(int i = 0; i < numRouters; i++){
			if(minCost[i] == 999)
				_NextHop[i] = -1;	
			else
				_NextHop[i] = i;
		}
	}
	
	private void computeLinkState()
	{
		int numRouters = _MinCost.length;
		int min = DvrPacket.INFINITY;
		
		for(int i = 0; i < numRouters; i++)
		{
			if(i != _ID)
			{
				for(int j = 0; j < numRouters; j++){
					if(j != _ID){
						//int value = _MinCost[j] + _DistanceVector[j][i];
						if (_MinCost[j] + _DistanceVector[j][i] < _MinCost[i])
							_NextHop[i] = _NextHop[j];
	
						min = Math.min(_MinCost[j] + _DistanceVector[j][i], min);
					}
				}
				if(min < _MinCost[i]){
					_MinCost[i] = min;
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
		
		while(!_Quit){}
		
		rtnTable = new RtnTable(_MinCost, _NextHop);
		return rtnTable;
	}
	
	
	/* **********************************************************************************
	 * 																					*
	 * 								Test Functions										*
	 * 																					*
	 * **********************************************************************************/
	private void displayRouterInfo()
	{
		int i, j;
		System.out.println("----------------------------");
		
		System.out.println("MinCost");
		for(i = 0; i < _MinCost.length; i++)
			System.out.print(_MinCost[i] + ", ");
		
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
