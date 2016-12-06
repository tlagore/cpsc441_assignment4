package cpsc441_assignment4;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import cpsc441.a4.shared.DvrPacket;

public class ReceiverThread extends Thread{
	private Router _Parent;
	private DataInputStream _DataInputStream;
	private boolean _Shutdown;
	
	public ReceiverThread(Router parent, DataInputStream dis)
	{
		_Parent = parent;
		_DataInputStream = dis;
		_Shutdown = false;
		
	}
	
	public void run()
	{
		int amountRead = 0;
		DataInputStream dataInStream;
		byte[] serializedReceivePacket = new byte[1000];
		DvrPacket packet;

		System.out.println("ReceiverThread initialized, beginning read spin.");
		while(!this.isInterrupted() && !_Shutdown)
		{
			try{
				amountRead = _DataInputStream.read(serializedReceivePacket);
				packet = (DvrPacket)Utils.deserialize(serializedReceivePacket, 0, amountRead);
				System.out.println(packet.toString());
				_Parent.processDvr(packet);
			}catch(Exception ex)
			{
				System.out.println("Failed to get response 'hello' packet: " + ex.getMessage());		
				if(amountRead == 0)
					_Shutdown = true;
				
				//tell parent we shutdown abnormally
				_Parent.abnormalShutdown();
			}
		}		
	}
	
	public void shutdown()
	{
		_Shutdown = true;
	}
}
