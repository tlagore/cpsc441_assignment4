package cpsc441_assignment4;

import java.io.ObjectInputStream;

import cpsc441.a4.shared.DvrPacket;

public class ReceiverThread extends Thread{
	private Router _Parent;
	private ObjectInputStream _InputStream;
	private boolean _Shutdown;
	
	public ReceiverThread(Router parent, ObjectInputStream ois)
	{
		_Parent = parent;
		_InputStream = ois;
		_Shutdown = false;
		
	}
	
	public void run()
	{
		int amountRead = 0;
		DvrPacket packet;

		System.out.println("ReceiverThread initialized, beginning read spin.");
		while(!this.isInterrupted() && !_Shutdown)
		{
			try{
				packet = (DvrPacket)_InputStream.readObject();
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
