package cpsc441_assignment4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import cpsc441.a4.shared.DvrPacket;

public class Utils {
	public Utils()
	{
		
	}
	
	public static byte[] serialize(Object obj)
	{
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutStream;
		try {
			objectOutStream = new ObjectOutputStream(byteOutStream);
			objectOutStream.writeObject(obj);
		}catch(IOException ex)
		{
			System.out.println("Erorr in serialize: " + ex.getMessage());
		}
		
		return byteOutStream.toByteArray();
	}
	
	public static Object deserialize(byte[] data, int offset, int length)
	{
		ByteArrayInputStream byteInStream;	
		ObjectInputStream objectInStream;
		Object toReturn = null;
		try{
			byteInStream = new ByteArrayInputStream(data, offset, length);
			objectInStream = new ObjectInputStream(byteInStream);
			
			toReturn = (Object)objectInStream.readObject();
		}catch(IOException ex)
		{
			ex.printStackTrace();
			System.out.println("Error in deserialize (IOException): " + ex.getMessage());
		}catch(ClassNotFoundException ex)
		{
			System.err.println("Error in deserialize (ClassNotFoundException): " + ex.getMessage());
		}
		
		return toReturn;
	}
}
