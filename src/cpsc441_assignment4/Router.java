package cpsc441_assignment4;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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
	private UpdateTimer _UpdateTmer;
	private Socket _RelayServerSocket;
	private RtnTable _RtnTable;
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
	}

	/**
	 * Called by the UpdateTimer thread to inform the router that the update interval has expired and needs to be 
	 * rerun.
	 */
	public void updateRtnTable()
	{

	}

	public void tcpHandshake()
	{
 		int amountRead;
		//byte[] serializedSendPacket;
		byte[] serializedReceivePacket = new byte[1000];
		DvrPacket hello = new DvrPacket(_ID, DvrPacket.SERVER, DvrPacket.HELLO);
		DvrPacket hi;
		DataInputStream dataInStream;
		DataOutputStream dataOutputStream;
		
		try{
			_RelayServerSocket = new Socket(_ServerName, _ServerPort);
			
			dataInStream = new DataInputStream(_RelayServerSocket.getInputStream());
			dataOutputStream = new DataOutputStream(_RelayServerSocket.getOutputStream());
			
			//serializedSendPacket = Utils.serialize(hello);
			
			dataOutputStream.write(Utils.serialize(hello));
			dataOutputStream.flush();
			
			while(dataInStream.available() == 0){}
			 	
			amountRead = dataInStream.read(serializedReceivePacket);
			System.out.println(amountRead);
			
			//amountRead = dataInStream.read(serializedReceivePacket);
			
			hi = (DvrPacket)Utils.deserialize(serializedReceivePacket, 0, amountRead);
			
			processDvr(hi);
			
		}catch(IOException ex)
		{
			System.out.println(ex.getMessage());
		}
	}	
	
	public void processDvr(DvrPacket pkt)
	{
		if(pkt.type == DvrPacket.HELLO)
		{
			if(pkt.sourceid == DvrPacket.SERVER)
				initTopology(pkt.getMinCost());
		}
	}
	
	private void initTopology(int[] minCost)
	{
		int numRouters = minCost.length;
		int[] nextHop = new int[numRouters];
		for(int i = 0; i < numRouters; i++){
			nextHop[i] = -1;				
		}
		nextHop[_ID] = _ID;
		
		_DistanceVector = new int[numRouters][numRouters];
		for(int j = 0; j < numRouters; j++)
			_DistanceVector[_ID][j] = minCost[j];
		
		_DistanceVector[_ID][_ID] = 0;
		
		//_RtnTable = new references.RtnTable(pkt.getMinCost(), )
	}

	/**
	 * starts the router 
	 * 
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {
		_RtnTable = new RtnTable();

		tcpHandshake();

		return _RtnTable;
	}
}
