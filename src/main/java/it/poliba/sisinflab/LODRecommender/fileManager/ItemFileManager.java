package it.poliba.sisinflab.LODRecommender.fileManager;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class ItemFileManager extends FileManager{
	
	
	public ItemFileManager(String filename, int mode) {
		super(filename, mode);
		// TODO Auto-generated constructor stub
	}
	
	public ItemFileManager(String filename, THashMap<String, String> items_index) {
		super(filename, items_index);
		// TODO Auto-generated constructor stub
	}


	public synchronized void write(ItemTree i){
    	
    	try{
	    	// Serialize to a byte array
	        ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
	        ObjectOutput out = new ObjectOutputStream(bos);
	        out.writeObject(i);
	        out.close();
	        
	        // Get the bytes of the serialized object
	        byte[] buf = bos.toByteArray();
	        // save the position and length of the serialized object
	        file_index_writer.append(i.getItemId() + "\t" + position + "\t" + buf.length);
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
	
	public ItemTree read(String key){
		
		String[] vals = file_index.get(key).split(":");
		
		try{
			
			ItemTree res = null;
			
			byte[] buf = new byte[Integer.parseInt(vals[1])];
			file.seek(Long.parseLong(vals[0])); // seek to the objects data
			file.readFully(buf); // read the data 
	        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
	        ObjectInputStream ois = new ObjectInputStream(bis);
	        res = (ItemTree) ois.readObject(); // deserialize data
        
	        return res;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	public TIntObjectHashMap<ItemTree> read(long n){
    	
		TIntObjectHashMap<ItemTree> res = new TIntObjectHashMap<ItemTree>();
    	
    	long count = 0;
    	for(String key : file_index.keySet()){
			
			if(count < n){
				
				ItemTree tmp = read(key);
				res.put(tmp.getItemId(), tmp);
				count++;
			}
			else
				break;
			
		}
    	
    	return res;
    	
    }
}