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
	
	public static byte[] serialize(Object obj) throws IOException
	{
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutStream;

		objectOutStream = new ObjectOutputStream(byteOutStream);
		objectOutStream.writeObject(obj);

		return byteOutStream.toByteArray();
	}
	
	public static Object deserialize(byte[] data, int offset, int length) throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream byteInStream;	
		ObjectInputStream objectInStream;
		Object toReturn = null;
		
		byteInStream = new ByteArrayInputStream(data, offset, length);
		objectInStream = new ObjectInputStream(byteInStream);
		
		toReturn = (Object)objectInStream.readObject();
		
		return toReturn;
	}
}
