package it.poliba.sisinflab.LODRecommender.userPathExtractor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.utils.StringUtils;

/**
 * This class is part of the LOD Recommender
 * 
 * This class is used by UserPathExtractor for multi-threading paths extraction 
 * 
 * @author Vito Mastromarino
 */
public class UserPathExtractorWorkerOptimized implements Runnable {
	
	private ArrayList<String> items_id; // items 
	private TIntArrayList user_items;
	private int user_id;
	private TIntFloatHashMap user_rating;
	private BufferedWriter training_file;
	private BufferedWriter unknown_file;
	private boolean normalize;
	private THashMap<String, String> items_path_index;
	private StringFileManager pathReader;
	private TObjectIntHashMap<String> path_index;
	private THashMap<String, String> paths;
	private String path_file;
	private int user_items_sampling;
	private int ratesThreshold;
	private TIntObjectHashMap<TIntHashSet> items_link;
	private TIntObjectHashMap<TIntIntHashMap> user_paths;
	
	private static Logger logger = LogManager.getLogger(UserPathExtractorWorkerOptimized.class.getName());
	
	/**
	 * Constuctor
	 */
	public UserPathExtractorWorkerOptimized(int user_id, TIntFloatHashMap user_rating,
			ArrayList<String> items_id, BufferedWriter training_file, BufferedWriter unknown_file, 
			boolean normalize, THashMap<String, String> items_path_index, String path_file, 
			TObjectIntHashMap<String> path_index, THashMap<String, String> paths,
			int user_items_sampling, int ratesThreshold,
			TIntObjectHashMap<TIntHashSet> items_link){
		
		this.user_id = user_id;
		this.items_id = items_id;
		this.user_rating = user_rating;
		this.training_file = training_file;
		this.unknown_file = unknown_file;
		this.normalize = normalize;
		this.items_path_index = items_path_index;
		this.path_index = path_index;
		this.paths = paths;
		this.path_file = path_file;
		this.user_items_sampling = user_items_sampling;
		this.ratesThreshold = ratesThreshold;
		this.items_link = items_link;
		
	}
	
	/**
	 * run path extraction
	 */
	public void run(){
		
		try {
			start();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * start path extraction considering all the pairs main_item-items
	 * @throws IOException 
	 */
	public void start() throws NumberFormatException, ClassNotFoundException, IOException{
		
		long start = System.currentTimeMillis();
		
		user_items = new TIntArrayList();
		user_paths = new TIntObjectHashMap<TIntIntHashMap>();
		
		for(int item_rated : user_rating.keys()){
			if(items_id.contains(Integer.toString(item_rated))){
				user_items.add(item_rated);
			}
		}
		
		int real_num_items = user_items.size();
		int num_user_items = (user_items_sampling * user_items.size()) / 100;
		user_items = (TIntArrayList) user_items.subList(0, num_user_items);
		
		if(user_items.size() > 0){
			
			logger.info("user " + user_id + " start paths extraction");
			
			pathReader = new StringFileManager(path_file, items_path_index);
			
			TIntIterator it = user_items.iterator();
			while(it.hasNext())
				computePaths(it.next());
							
			pathReader.close();
			
			if(user_paths.size() > 0)
				kernelize();
		}
		
		long stop = System.currentTimeMillis();
		
		logger.info("user " + user_id + " (" + user_items.size() + "/" 
				+ real_num_items + " items rated): paths extraction terminated in sec " 
				+ ((stop - start) / 1000));
	
	}
	
	
	/**
	 * Extract paths from a user tree and an item tree
	 * @param     user user tree
	 * @param     item item tree
	 * @return    paths map (path index:freq)
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws NumberFormatException 
	 */
	public void computePaths(int user_item_id){
		
		String item_pair_paths = null;
		String user_item_rate = StringUtils.extractRate(user_rating.get(user_item_id), 
				ratesThreshold);
		
		if(items_link.containsKey(user_item_id)){
			
			// itero solo sugli items collegati all'item votato dall'utente
			TIntIterator it = items_link.get(user_item_id).iterator();
			while(it.hasNext()){
				
				boolean reverse = false;
				
				int item_id = it.next();
				user_paths.putIfAbsent(item_id, new TIntIntHashMap());
				
				String key = user_item_id + "-" + item_id;
				
				if(!pathReader.containsKey(key)){
					reverse = true;
					key = item_id + "-" + user_item_id;
				}
				
				item_pair_paths = loadPathsFromMap(key);				
				
				String[] pair_vals = item_pair_paths.split(",");
						
				if(pair_vals.length > 0){
							
					for(String s : pair_vals){
									
						String[] path_freq = s.split("=");
						int key1 = 0;
									
						if(reverse)
							key1 = extractKey(user_item_rate + "-inv_" + path_freq[0]);
						else
							key1 = extractKey(user_item_rate + "-" + path_freq[0]);
								
						user_paths.get(item_id).adjustOrPutValue
							(key1, Integer.parseInt(path_freq[1]), Integer.parseInt(path_freq[1]));
									
					}
				}
			}
		}
	}
	
	private String loadPathsFromFile(String key){
		
		return pathReader.read(key);
		
	}
	
	private String loadPathsFromMap(String key){
	
		if(paths.containsKey(key))
			return paths.get(key);
		else
			return loadPathsFromFile(key);
		
	}
	
	/**
	 * Extract key from path index
	 * @param     s  string to index
	 * @return    index of s
	 */
	private int extractKey(String s) {
		
		return path_index.get(s);

	}
	
	private void kernelize(){
		
		//logger.info("kernelize user " + user_id + " (" + user_paths.size() + " items)");
		
		try{
			
			DecimalFormat form = new DecimalFormat("0.000000"); 
			
			
			TIntObjectIterator<TIntIntHashMap> it = user_paths.iterator();
			
			while(it.hasNext()){
				
				it.advance();
				
				int item_id = it.key();
				double rate = 0;
				double n = 1;
				boolean training = false;
				
				TIntIntHashMap paths = it.value();
				
				if(user_rating.containsKey(item_id)){
					training=true;
					rate = user_rating.get(item_id);
				}
				
				if(normalize)
					n = norm(paths);
				
				StringBuffer str = new StringBuffer();
				str.append(rate + " qid:" + user_id + " 1:" + item_id + " ");
				
				for(int i=1; i <= path_index.size(); i++){
						
					int count = 0;
						
					if(paths.containsKey(i)){
						
						count = paths.get(i);
							
						if(normalize)
							str.append(i+1 + ":" + form.format(count/n).replace(",", ".") + " ");
						else
							str.append(i+1 + ":" + count + " ");
							
					}
				}
				
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
	
	private double norm(TIntIntHashMap map){
		
		int sum = 0;
		for(int i : map.keys()){
			sum += (map.get(i)^2);
		}
		
		return Math.sqrt(sum);
		
	}

}
