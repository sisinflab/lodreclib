package it.poliba.sisinflab.LODRecommender.fileManager;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class TextFileManager {
	
	private String filename;
	private BufferedWriter bw;
	private boolean append;
	
	
	public TextFileManager(String filename){
		
		this.filename = filename;
		this.append = false;
		openFile();
		
	}
	
	public TextFileManager(String filename, boolean append){
		
		this(filename);
		this.append = append;
		
	}
	
	private void openFile(){
		
		try{
			
			bw = new BufferedWriter(new FileWriter(filename, append));
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public synchronized void write(String str){
		
		try{
			
			bw.append(str);
			bw.newLine();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	public void close(){
		
		try{
			bw.flush();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
