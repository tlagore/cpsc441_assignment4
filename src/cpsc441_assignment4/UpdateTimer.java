package cpsc441_assignment4;

import java.util.TimerTask;

public class UpdateTimer extends TimerTask {
	private Router _Parent;
	
	public UpdateTimer(Router parent)
	{
		this._Parent = parent;
	}
	
	public void run()
	{
		_Parent.updateRtnTable();
	}
}
