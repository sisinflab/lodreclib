package it.poliba.sisinflab.LODRec.fileManager;

import gnu.trove.map.hash.THashMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Set;

public class StringFileManager extends FileManager {
	
	
	public StringFileManager(String filename, Set<Integer> entries_to_consider) {
		super(filename, entries_to_consider);
		// TODO Auto-generated constructor stub
	}
	
	public StringFileManager(String filename, int mode) {
		super(filename, mode);
		// TODO Auto-generated constructor stub
	}
	
	public StringFileManager(String filename, THashMap<String, String> items_path_index) {
		super(filename, items_path_index);
		// TODO Auto-generated constructor stub
	}

	public synchronized void write(String str){
    	
    	try{
    		
    		String[] vals = str.split("\t");
    		
	    	// Serialize to a byte array
	        ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
	        ObjectOutput out = new ObjectOutputStream(bos);
	        out.writeObject(vals[1]);
	        out.close();
	        
	        // Get the bytes of the serialized object
	        byte[] buf = bos.toByteArray();
	        
	        // save the position and length of the serialized object
	        file_index_writer.append(vals[0] + "\t" + position + "\t" + buf.length);
	        file_index_writer.newLine();
	        
	        // write to the file
	        file.seek(position);
	        file.write(buf);
	        position += buf.length;
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	
    }
    
    public String read(String key){
    	
    	try{
				
			String[] vals = file_index.get(key).split(":");
				
			byte[] buf = new byte[Integer.parseInt(vals[1])];
			file.seek(Long.parseLong(vals[0])); // seek to the objects data
			file.readFully(buf); // read the data 
		    ByteArrayInputStream bis = new ByteArrayInputStream(buf);
		    ObjectInputStream ois = new ObjectInputStream(bis);
		    String res = (String) ois.readObject();
		    return res;
    	}
    	catch(Exception e){
    		return null;
    	}
    	
    }
    
    public THashMap<String, String> read(long n){
    	
    	THashMap<String,String> res = new THashMap<String,String>();
    	
    	long count = 0;
    	for(String key : file_index.keySet()){
			
			if(count < n){
				String tmp = read(key);
				res.put(key, tmp);
				count++;
			}
			else
				break;
			
		}
    	
    	return res;
    	
    }
    
}