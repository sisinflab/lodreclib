package it.poliba.sisinflab.LODRecommender.userFeaturesExtractor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.utils.StringUtils;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;

/**
 * This class is part of the LOD Recommender
 * 
 * This class is used by UserPathExtractor for multi-threading paths extraction 
 * 
 * @author Vito Mastromarino
 */
public class UserFeaturesExtractorWorker implements Runnable {
	
	private SynchronizedCounter counter; // synchronized counter for path index
	private TIntFloatHashMap user_rating;
	private THashMap<String, String> item_features;
	private BufferedWriter training_file;
	private BufferedWriter unknown_file;
	private int user_id;
	private String user_features_string;
	private ArrayList<String> items_id;
	private String item_features_file;
	private TObjectIntHashMap<String> features_index;
	private TObjectIntHashMap<String> user_features_index;
	private TIntArrayList user_items;
	private StringFileManager itemFeaturesReader;
	private int ratesThreshold;
	
	private static Logger logger = LogManager.getLogger(UserFeaturesExtractorWorker.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public UserFeaturesExtractorWorker(SynchronizedCounter counter, 
			int user_id, TIntFloatHashMap user_rating, ArrayList<String> items_id, 
			BufferedWriter training_file, BufferedWriter unknown_file,
			String item_features_file, TObjectIntHashMap<String> features_index, TObjectIntHashMap<String> user_features_index, 
			THashMap<String, String> item_features, int ratesThreshold){
		
		this.counter = counter;
		this.user_id = user_id;
		this.user_rating = user_rating;
		this.item_features = item_features;
		this.features_index = features_index;
		this.user_features_index = user_features_index;
		this.training_file = training_file;
		this.unknown_file = unknown_file;
		this.items_id = items_id;
		this.item_features_file = item_features_file;
		this.ratesThreshold = ratesThreshold;
	}
	
	/**
	 * run path extraction
	 */
	public void run(){
		
		long start = System.currentTimeMillis();
		
		start();
		
		long stop = System.currentTimeMillis();
		logger.info("user " + user_id + ": features extraction terminated in [sec] " 
				+ ((stop - start) / 1000));
		
	}
	
	/**
	 * start path extraction considering all the pairs main_item-items
	 * @throws IOException 
	 */
	public void start(){
		
		user_items = new TIntArrayList();
		
		for(int j : user_rating.keys()){
			if(items_id.contains(Integer.toString(j))){
				user_items.add(j);
			}
		}
		
		if(user_items.size() > 0){
			
			try{
			
				logger.info("user " + user_id + " (" + user_items.size() + " items rated): start extraction");
				
				itemFeaturesReader = new StringFileManager(item_features_file, StringFileManager.READ);
				TIntIntHashMap user_features = new TIntIntHashMap();
				
				TIntIterator it = user_items.iterator();
				while(it.hasNext()){
					
					int item_id = it.next();
					String features_value;
					
					if(item_features.containsKey(Integer.toString(item_id)))
						features_value = item_features.get(Integer.toString(item_id));
					else
						features_value = itemFeaturesReader.read(Integer.toString(item_id));
					
					String rate = StringUtils.extractRate(user_rating.get(item_id), 
							ratesThreshold);
					
					String[] vals = features_value.split(",");
					for(String s : vals){
						
						String[] features_freq = s.split("=");
						int key = extractKey(rate + "-" + features_freq[0]);
						user_features.adjustOrPutValue(key, Integer.parseInt(features_freq[0]), 
								Integer.parseInt(features_freq[0]));
					}
					
				}
				
				if(user_features.size()>0){
					
					StringBuffer str = new StringBuffer();
					
					for(int i = features_index.size(); 
							i <= user_features_index.size() + features_index.size(); i++){
						
						if(user_features.containsKey(i))
							str.append(i+1 + ":" + user_features.get(i) + " ");
						
					}
					
					user_features_string = new String(str);
					
					kernelize();
				}
				
				itemFeaturesReader.close();
			
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
		}
		
	}
	
	private void kernelize(){
		
		try{
		
			for(String s : items_id){
				
				double rate = 0;
				boolean training = false;
				int item_id = Integer.parseInt(s);
				StringBuffer str = new StringBuffer();
				
				if(user_rating.containsKey(item_id)){
					training=true;
					rate = user_rating.get(item_id);
				}
				
				String features_value = "";
				
				if(item_features.containsKey(item_id))
					features_value = item_features.get(item_id);
				else{
					features_value = itemFeaturesReader.read(Integer.toString(item_id));
				}
				
				TIntIntHashMap tmp = new TIntIntHashMap();
				String[] vals = features_value.split(",");
				for(String ss : vals){
					
					String[] features_freq = ss.split("=");
					tmp.put(Integer.parseInt(features_freq[1]), Integer.parseInt(features_freq[1]));
				}
				
				
				str.append(rate + " qid:" + user_id + " 1:" + item_id + " ");
				
				for(int i = 0; i <= features_index.size(); i++){
					
					if(tmp.containsKey(i))
						str.append(i+1 + ":" + tmp.get(i) + " ");
					
				}
				
				str.append(user_features_string);
				
				if(training){
					synchronized(training_file){
						training_file.append(str);
						training_file.newLine();
						training_file.flush();
					}
				}
				else{
					synchronized(unknown_file){
						unknown_file.append(str);
						unknown_file.newLine();
						unknown_file.flush();
					}
				}
			}
		
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * Extract key from path index
	 * @param     s  string to index
	 * @return    index of s
	 */
	private int extractKey(String s) {
		
		synchronized(user_features_index){
			if(user_features_index.contains(s)){
				return user_features_index.get(s);
			}
			else{
				int id = counter.value();
				user_features_index.put(s, id);
				return id;
			}
		}

	}

}
