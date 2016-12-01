package cpsc441_assignment4;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
	private RtnTable _RtnTable;
	private ReceiverThread _RcvrThread;
	
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
	public void broadcastCost()
	{
		//System.out.println("----------------");
		System.out.println(_MinCost.length);
		
		for(int i = 0; i < _MinCost.length; i++)
		{
			for(int j = 0; j < _MinCost.length; j++)
				System.out.print(_DistanceVector[i][j] + " ");
			
			System.out.println();
		}
		
		System.out.println("----------------");
			
		System.out.println("Broadcast!\n");
	}

	public void tcpHandshake()
	{
 		int amountRead;
		//byte[] serializedSendPacket;
		byte[] serializedReceivePacket = new byte[1000];
		DvrPacket hello = new DvrPacket(_ID, DvrPacket.SERVER, DvrPacket.HELLO);
		DvrPacket hi = null;
		DataInputStream dataInStream;
		DataOutputStream dataOutputStream;
		
		try{
			_RelayServerSocket = new Socket(_ServerName, _ServerPort);
			
			dataInStream = new DataInputStream(_RelayServerSocket.getInputStream());
			dataOutputStream = new DataOutputStream(_RelayServerSocket.getOutputStream());
			
			//serializedSendPacket = Utils.serialize(hello);
		
			while(hi == null)
			{
				dataOutputStream.write(Utils.serialize(hello));
				dataOutputStream.flush();
				
				try{
					Thread.sleep(500);
					
					amountRead = dataInStream.read(serializedReceivePacket);
					
					hi = (DvrPacket)Utils.deserialize(serializedReceivePacket, 0, amountRead);
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
			}
		}
	}
	
	private void initTopology(int[] minCost)
	{
		int numRouters = minCost.length;
		_NextHop = new int[numRouters];
		_DistanceVector = new int[numRouters][numRouters];
		
		_MinCost = minCost;
		
		_NextHop[_ID] = _ID;		
		_DistanceVector[_ID]= minCost;
		_DistanceVector[_ID][_ID] = 0;
		 
		for(int i = 0; i < numRouters; i++){
			if(minCost[i] == 999)
				_NextHop[i] = -1;	
			else
				_NextHop[i] = i;
		}
		
	}

	/**
	 * starts the router 
	 * 
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {
		_RtnTable = new RtnTable();
		tcpHandshake();
		
		_RcvrThread = new ReceiverThread(this, _RelayServerSocket);
		_UpdateTimer.scheduleAtFixedRate(new UpdateTimer(this), 1000, _UpdateInterval);
		
		while(!_Quit){}
		
		return _RtnTable;
	}
}
