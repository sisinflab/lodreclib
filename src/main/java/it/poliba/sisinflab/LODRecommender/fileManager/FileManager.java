package it.poliba.sisinflab.LODRecommender.fileManager;

import gnu.trove.map.hash.THashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;

public class FileManager {
	
	public static int WRITE = 1;
	public static int READ = 2;
	public static int APPEND = 3;
	
	
	protected long position = 0;
	protected RandomAccessFile file;
	protected BufferedWriter file_index_writer;
	protected BufferedReader file_index_reader;
	protected THashMap<String, String> file_index;
	protected long index_size = 0;
	
	public FileManager(String filename, int mode){
		
		switch(mode){
		
			case 1:{
				
				try{
					File f = new File(filename + ".dat");
					f.delete();
					file = new RandomAccessFile(filename + ".dat", "rw");
					file_index_writer = new BufferedWriter(new FileWriter(filename + "_file_index"));
				}
				catch(Exception e){
					e.printStackTrace();
				}
				
				break;
			}
			case 2:{
				
				try{
					file = new RandomAccessFile(filename + ".dat", "r");
					file_index_reader = new BufferedReader(new FileReader(filename + "_file_index"));
					file_index = readFileIndex();
				}
				catch(Exception e){
					e.printStackTrace();
				}
				
				break;
			}
			case 3:{
				
				try{
					file = new RandomAccessFile(filename + ".dat", "rw");
					file_index_writer = new BufferedWriter(new FileWriter(filename + "_file_index", true));
					setPosition();
				}
				catch(Exception e){
					e.printStackTrace();
				}
				
			}
		
		}
		
	}
	
	public FileManager(String filename, Set<Integer> entries_to_consider){
		
		try{
			file = new RandomAccessFile(filename + ".dat", "r");
			file_index_reader = new BufferedReader(new FileReader(filename + "_file_index"));
			file_index = readFileIndex(entries_to_consider);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public FileManager(String filename, THashMap<String, String> file_index){
	
		try{
			file = new RandomAccessFile(filename + ".dat", "r");
			file_index_reader = new BufferedReader(new FileReader(filename + "_file_index"));
			this.file_index = file_index;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	private void setPosition(){
		try {
			position = file.length();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public THashMap<String, String> getFileIndex(){
		return file_index;
	}

    private THashMap<String, String> readFileIndex(){
    	
    	THashMap<String,String> file_index = new THashMap<String,String>();
		
		try{
			String line = null;
			
			while((line=file_index_reader.readLine()) != null){
				String[] vals = line.split("\t");
				file_index.put(vals[0], vals[1]+":"+vals[2]);
				index_size++;
			}
			
			return file_index;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
		
	}
    
    private THashMap<String, String> readFileIndex(Set<Integer> entries_to_consider){
    	
    	THashMap<String,String> file_index = new THashMap<String,String>();
		
		try{
			String line = null;
			
			while((line=file_index_reader.readLine()) != null){
				String[] vals = line.split("\t");
				String[] items = vals[0].split("-");
				int items1 = Integer.parseInt(items[0]);
				int items2 = Integer.parseInt(items[1]);
				
				if(entries_to_consider.contains(items1) || entries_to_consider.contains(items2)){
				
					file_index.put(vals[0], vals[1]+":"+vals[2]);
					index_size++;
				}
			}
			
			return file_index;
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return null;
		
	}
    
    public long getIndexSize(){
    	
    	return index_size;
    	
    }
    
    public Set<String> getKeysIndex(){
    	
    	return file_index.keySet();
    	
    }
    
    public boolean containsKey(String key){
    	
    	if(file_index.containsKey(key))
    		return true;
    	else
    		return false;
    	
    }
    
    
    public void close(){
    	
    	try{
	    	file.close();
	    	if(file_index_writer!=null){
	    		file_index_writer.flush();
		    	file_index_writer.close();
	    	}
	    	if(file_index_reader!=null)
		    	file_index_reader.close();
	    	
    	}
    	catch(Exception e){
    		e.printStackTrace();
    	}
    	
    }
}