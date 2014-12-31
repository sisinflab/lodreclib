package it.poliba.sisinflab.LODRecommender.userFeaturesExtractor;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TFloatHashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.utils.MemoryMonitor;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * This class is part of the LOD Recommender
 * 
 * This class extracts paths from user and item trees 
 * 
 * @author Vito Mastromarino
 */
public class UserFeaturesExtractor {
	
	private TIntObjectHashMap<TIntFloatHashMap> user_rating; // map user-[item-rating]
	private String inputRatingsFile; // input user ratings filename
	private String item_features_file;
	private String itemsFile;
	private TObjectIntHashMap<String> features_index;
	private TObjectIntHashMap<String> user_features_index;
	private THashMap<String, String> item_features;
	private SynchronizedCounter counter; // synchronized counter for path index
	private int item_features_in_memory;
	private int ratesThreshold;
	private TFloatHashSet labels;
	
	private static Logger logger = LogManager.getLogger(UserFeaturesExtractor.class.getName());
	
	/**
	 * Constuctor
	 */
	public UserFeaturesExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.inputRatingsFile = prop.get("inputRatingsFile");
			this.item_features_file = prop.get("outputFeaturesFile");
			this.item_features_in_memory = Integer.parseInt(prop.get("pathsInMemory"));
			this.itemsFile = prop.get("itemsFile");
			this.ratesThreshold = Integer.parseInt(prop.get("ratesThreshold"));
			
			loadItemFeatures();
			loadFeaturesIndex();
			loadInputRatingsFile();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	private void loadItemFeatures(){
		
		logger.info("Item features loading");
		
		StringFileManager pathReader = new StringFileManager(item_features_file, StringFileManager.READ);
		
		int num_item = (item_features_in_memory*pathReader.getKeysIndex().size())/100;
		item_features = pathReader.read(num_item);
		
		logger.info(num_item + " of " + pathReader.getKeysIndex().size() +
				" (" + item_features_in_memory + "%) item-features in memory");
		
		pathReader.close();
		
	}
	
	/**
	  * load features index
	  */
	private void loadFeaturesIndex(){
		features_index = new TObjectIntHashMap<String>();
		TextFileUtils.loadIndex("features_index", features_index);
		logger.debug("Features index loading");
	}
	
	
	/**
	 * load user ratings from input file
	 */
	public void loadInputRatingsFile(){
		
		user_rating = new TIntObjectHashMap<TIntFloatHashMap>();
		// load user-[item-rating] from input file
		TextFileUtils.loadInputUsersRatings(inputRatingsFile, user_rating, labels);
		
	}
	
	/**
	 * start path extraction
	 */
	public void start(){
		
		// get processors number for multi-threading
		int n_threads =  Runtime.getRuntime().availableProcessors();
		n_threads = 4;
		logger.debug("Threads number: " + n_threads);
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(n_threads);
		
		counter = new SynchronizedCounter(features_index.size());
		user_features_index = new TObjectIntHashMap<String>();
		
		try {
			
			BufferedWriter training_file = new BufferedWriter(new FileWriter("training_set"));
			BufferedWriter unknown_file = new BufferedWriter(new FileWriter("unknown_set"));
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_list = new ArrayList<String>(itemReader.getKeysIndex());
			itemReader.close();
			
			for(int user_id : user_rating.keys()){
				
				// user features extraction
				Runnable worker = new UserFeaturesExtractorWorker(counter, user_id, 
						user_rating.get(user_id), items_list, training_file, unknown_file, 
						item_features_file, features_index, user_features_index, item_features,
						ratesThreshold);
				// run the worker thread  
				executor.execute(worker);
				
			}
				
			// This will make the executor accept no new threads
			// and finish all existing threads in the queue
			executor.shutdown();
			// Wait until all threads are finish
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			training_file.flush();
		    training_file.close();
		    
		    unknown_file.flush();
		    unknown_file.close();
		    
		    // write top features index
		    TextFileUtils.writeData("user_features_index", features_index);
		 			
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		UserFeaturesExtractor pe = new UserFeaturesExtractor();
		
		long start = System.currentTimeMillis();
		
		pe.start();
		
		long stop = System.currentTimeMillis();
		System.out.println("User features extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		MemoryMonitor.stats();
		
	}
}
