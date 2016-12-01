package cpsc441_assignment4;

import java.net.Socket;

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
		while(!this.isInterrupted() && !_Shutdown)
		{
			
		}
	}
}
