package it.poliba.sisinflab.LODRecommender.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class FileInputConversion {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try{
			
			BufferedReader br = new BufferedReader(new FileReader("C://Users/Vito/Desktop/item_index.tsv"));
			BufferedWriter bw = new BufferedWriter(new FileWriter("C://Users/Vito/Desktop/lastfm-big"));
			String line = null;
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				bw.append(vals[2] + "\t" + vals[0] + "\t" + vals[1]);
				bw.newLine();
				
			}
			
			br.close();
			bw.flush();
			bw.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}

}
