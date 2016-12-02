package cpsc441_assignment4;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import cpsc441.a4.shared.DvrPacket;

public class ReceiverThread extends Thread{
	private Router _Parent;
	private Socket _Socket;
	private boolean _Shutdown;
	
	public ReceiverThread(Router parent, Socket socket)
	{
		_Parent = parent;
		_Socket = socket;
		_Shutdown = false;
	}
	
	public void run()
	{
		DvrPacket pkt;
		int amountRead;
		byte[]serializedPacket = new byte[1000];
		DataInputStream dataInStream;
		
		try{
			dataInStream = new DataInputStream(_Socket.getInputStream());
			
			while(!this.isInterrupted() && !_Shutdown)
			{
				try{
					amountRead = dataInStream.read(serializedPacket);
					pkt = (DvrPacket)Utils.deserialize(serializedPacket, 0, amountRead);
					_Parent.processDvr(pkt);
				}catch(Exception ex)
				{
					System.out.println("Error in ReceiverThread: " + ex.getMessage());		
				}
			}
		}catch(IOException ex){
			System.out.println("IOException in ReceiverThread. Cannot read from socket." + ex.getMessage());
		}
		
		
	}
	
	public void shutdown()
	{
		_Shutdown = true;
	}
}
