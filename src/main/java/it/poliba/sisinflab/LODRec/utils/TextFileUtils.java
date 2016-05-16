package it.poliba.sisinflab.LODRec.utils;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

public class TextFileUtils {
	
	public static void writeTIntMapArrayString(String file, TIntObjectHashMap<ArrayList<String>> map){
		
		try{
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			TIntObjectIterator<ArrayList<String>> it = map.iterator();
			StringBuffer str;
			
			while(it.hasNext()){
				
				str = new StringBuffer();
				it.advance();
				str.append(it.key() + "\t");
				
				ArrayList<String> tmp = it.value();
				for(String s : tmp)
					str.append(s + ",");
				
				writer.append(str.substring(0, str.length()-1));
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void loadTIntMapArrayString(String file, TIntObjectHashMap<ArrayList<String>> map){
		
		try{
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line = null;
			while((line=br.readLine()) != null){
				
				String[] vals = line.split("\t");
				String[] vals1 = vals[1].split(",");
				
				ArrayList<String> tmp = new ArrayList<String>();
				for(String s : vals1)
					tmp.add(s);
				
				map.put(Integer.parseInt(vals[0]), tmp);
				
			}
			
			br.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void loadTIntMapTIntHashSet(String file, TIntObjectHashMap<TIntHashSet> map){
		
		try{
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			
			String line = null;
			while((line=br.readLine()) != null){
				
				String[] vals = line.split("\t");
				String[] vals1 = vals[1].split(",");
				
				TIntHashSet tmp = new TIntHashSet();
				for(String s : vals1)
					tmp.add(Integer.parseInt(s));
				
				map.put(Integer.parseInt(vals[0]), tmp);
				
			}
			
			br.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void writeTIntMapTIntHashSet(String file, TIntObjectHashMap<TIntHashSet> map){
		
		try{
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			TIntObjectIterator<TIntHashSet> it = map.iterator();
			StringBuffer str;
			
			while(it.hasNext()){
				
				str = new StringBuffer();
				it.advance();
				str.append(it.key() + "\t");
				TIntHashSet tmp = it.value();
				TIntIterator it1 = tmp.iterator();
				while(it1.hasNext())
					str.append(it1.next() + ",");
				
				writer.append(str.substring(0, str.length()-1));
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void writeData(String file, TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> path){
		
		BufferedWriter writer;
		try {
			
			writer = new BufferedWriter(new FileWriter(file));

			TIntObjectIterator<TIntObjectHashMap<TIntIntHashMap>> it = path.iterator();
			while(it.hasNext()){
				it.advance();
				int a = it.key();
				TIntObjectHashMap<TIntIntHashMap> tmp = it.value();
				
				if(tmp!=null){
					TIntObjectIterator<TIntIntHashMap> it1 = tmp.iterator();
					while(it1.hasNext()){
						it1.advance();
						int b = it1.key();
						writer.append(a + "-" + b + ": " + it1.value());
						writer.newLine();
					}
				}
			}

			writer.flush();
			writer.close();

		} 
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	public static void writeData(String file, TObjectIntHashMap<String> data){
		
		BufferedWriter writer;
		try {
			
			writer = new BufferedWriter(new FileWriter(file));

			for (String s : data.keySet()) {
				writer.append(data.get(s) + "\t" + s);
				writer.newLine();
			}

			writer.flush();
			writer.close();

		} 
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void loadInputURIs(String file, 
			TObjectIntHashMap<String> uri_id, boolean append, String outFile){
		
		try{
			
			int index = 1;
			if(append){
				if(new File(outFile).exists()){
				
					BufferedReader br = new BufferedReader(new FileReader(outFile));
					
					while(br.readLine() != null)
						index++;
					
					br.close();
				}
			}
			
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				if(vals.length==2){
					uri_id.put(vals[1], Integer.parseInt(vals[0]));
				}else if(vals.length>1){
					uri_id.put(vals[2], Integer.parseInt(vals[0]));
				}
				else{
					uri_id.put(vals[0], index);
					index++;
				}
			}
			
			br.close();
			
			write(uri_id, append, outFile);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void write(TObjectIntHashMap<String> uri_id, 
			boolean append, String outFile){
		
		BufferedWriter writer;
		try {
			
			writer = new BufferedWriter(new FileWriter(outFile, append));

			for (String s : uri_id.keySet()) {
				writer.append(uri_id.get(s) + "\t" + s);
				writer.newLine();
			}

			writer.flush();
			writer.close();

		} 
		catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static TIntObjectHashMap<TIntFloatHashMap> loadInputUsersRatings(String file) {
		TIntObjectHashMap<TIntFloatHashMap> user_rating =new TIntObjectHashMap<TIntFloatHashMap>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			float rate;
			int user_id, item_id;

			while ((line = br.readLine()) != null) {
				String[] vals = line.split("\t");
				
				if(vals.length==2)
					rate=1;
				else
					rate = Float.parseFloat(vals[2]);
				
				user_id = Integer.parseInt(vals[0]);
				item_id = Integer.parseInt(vals[1]);

				user_rating.putIfAbsent(user_id, new TIntFloatHashMap());
				user_rating.get(user_id).put(item_id, rate);

			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return user_rating;
	}
	
	public static void loadInputUsersRatings(String file, 
			TIntObjectHashMap<TIntFloatHashMap> user_rating, TFloatHashSet labels){
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			float rate;
			int user_id, item_id;
			
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				rate = Float.parseFloat(vals[2]);
				user_id = Integer.parseInt(vals[0]);
				item_id = Integer.parseInt(vals[1]);
				
				user_rating.putIfAbsent(user_id, new TIntFloatHashMap());
				user_rating.get(user_id).put(item_id, rate);
				labels.add(rate);
					
			}
			
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	
	public static void loadFileIndex(String file, THashMap<String, String> items_pair_value){
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				items_pair_value.put(vals[0], vals[1]+":"+vals[2]);
			}
			
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void loadFileIndex(String file, TIntObjectHashMap<String> id_value){
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				id_value.put(Integer.parseInt(vals[0]), vals[1]+":"+vals[2]);
			}
			
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void loadIndex(String file, TIntObjectHashMap<String> id_value){
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				id_value.put(Integer.parseInt(vals[0]), vals[1]);
			}
			
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void loadIndex(String file, TObjectIntHashMap<String> value_id){
		
		if(new File(file).exists()){
			try{
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = null;
				int index = 1;
				while((line=br.readLine()) != null){
					String[] vals = line.split("\t");
					if(vals.length>1)
						value_id.put(vals[1], Integer.parseInt(vals[0]));
					else
						value_id.put(vals[0], index++);
				}
				
				br.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	
	public static void computeIndex(String file, TObjectIntHashMap<String> value_id,
			HashSet<String> labels){
		
		
		if(new File(file).exists()){
			try{
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = null;
				int index = 1;
				while((line=br.readLine()) != null){
					String[] vals = line.split("\t");
					
					for(String s : labels){
						value_id.put(s + "-" + vals[0], index++);
						value_id.put(s + "-inv_" + vals[0], index++);
					}
				}
				
				br.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
		
	}
	
	
	public static void loadInputMetadataID(String metadata_file_index, String input_uri, 
			TIntIntHashMap input_metadata_id){
		
		TObjectIntHashMap<String> metadata_index = new TObjectIntHashMap<String>();
		loadIndex(metadata_file_index, metadata_index);
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(input_uri));
			String line = null;
			
			while((line=br.readLine()) != null){
				String[] vals = line.split("\t");
				if(metadata_index.containsKey(vals[1]));
					input_metadata_id.put(Integer.parseInt(vals[0]), metadata_index.get(vals[1]));
			}
			
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	public static void loadRelatedURIs(String file, TIntObjectHashMap<TIntArrayList> related_uri, 
			TObjectIntHashMap<String> uri_id){
		
		
		try{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = null;
			
			TIntArrayList tmp = null;
			while((line=br.readLine()) != null){
				tmp = new TIntArrayList();
				String[] vals = line.split(",");
				for(int i = 1; i < vals.length; i++){
					if(!vals[i].contentEquals("null") && uri_id.containsKey(vals[i]))
						tmp.add(uri_id.get(vals[i]));
				}
				if(uri_id.containsKey(vals[0]))
					related_uri.put(uri_id.get(vals[0]), tmp);
				
			}
			
			br.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}

}
