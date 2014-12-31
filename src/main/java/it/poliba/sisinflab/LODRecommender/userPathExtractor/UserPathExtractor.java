package it.poliba.sisinflab.LODRecommender.userPathExtractor;

import gnu.trove.iterator.TFloatIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.StringUtils;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
public class UserPathExtractor {
	
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<TIntFloatHashMap> user_rating; // map user-[item-rating]
	private String inputRatingsFile; // input user ratings filename
	private boolean normalize;
	private String path_file;
	private String itemsFile;
	private THashMap<String, String> paths;
	private int paths_in_memory;
	private THashMap<String, String> items_path_index;
	private int user_items_sampling;
	private int ratesThreshold;
	private TIntObjectHashMap<TIntHashSet> items_link;
	private TFloatHashSet labels;
	
	private static Logger logger = LogManager.getLogger(UserPathExtractor.class.getName());
	
	/**
	 * Constuctor
	 */
	public UserPathExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.inputRatingsFile = prop.get("inputRatingsFile");
			this.normalize =  Boolean.valueOf(prop.get("normalize"));
			this.path_file = prop.get("outputPathFile");
			this.itemsFile = prop.get("itemsFile");
			this.paths_in_memory = Integer.parseInt(prop.get("pathsInMemory"));
			this.user_items_sampling = Integer.parseInt(prop.get("userItemsSampling"));
			this.ratesThreshold = Integer.parseInt(prop.get("ratesThreshold"));
			
			loadInputRatingsFile();
			loadItemPaths();
			loadPathIndex();
			loadItemsLink();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	
	private void loadItemsLink(){
		
		logger.info("Items link loading");
		items_link = new TIntObjectHashMap<TIntHashSet>();
		TextFileUtils.loadTIntMapTIntHashSet("items_link", items_link);
		
	}
	
	
	private void loadItemPaths(){
		
		// se non carico tutti i paths in memoria, calolo quali items tra quelli votati dall'utente
		// sono più ricorrenti, in modo da privilegiare il caricamento in memoria dei path più interessanti
		HashMap<Integer, Integer> items_count = new HashMap<Integer, Integer>();
		TIntObjectIterator<TIntFloatHashMap> it = user_rating.iterator();
		while(it.hasNext()){
			it.advance();
			for(int i : it.value().keys()){
				if(!items_count.containsKey(i))
					items_count.put(i, 1);
				else{
					int old_count = items_count.get(i);
					items_count.put(i, old_count + 1);
				}
			}
		}
		
		
		logger.info("Paths file index loading");
		
		StringFileManager pathReader = new StringFileManager(path_file, items_count.keySet());
		
		items_path_index = new THashMap<String, String>(pathReader.getFileIndex());
		
		long num_item_pair = (paths_in_memory * items_path_index.size()) / 100;
		
		logger.info("Loading " + num_item_pair + " of " + items_path_index.size() +
				" (" + paths_in_memory + "%) item pair paths in memory");
		
		paths = pathReader.read(num_item_pair);
		
		logger.info("Paths loading completed");
		
		pathReader.close();
		
	}
	
		
	/**
	 * load user ratings from input file
	 */
	private void loadInputRatingsFile(){
		
		user_rating = new TIntObjectHashMap<TIntFloatHashMap>();
		labels = new TFloatHashSet();
		// load user-[item-rating] from input file
		TextFileUtils.loadInputUsersRatings(inputRatingsFile, user_rating, labels);
		
		logger.info("User ratings loaded (" + labels.size() + " different labels)");
		
	}
	
	private void loadPathIndex(){
		
		path_index = new TObjectIntHashMap<String>();
		
		// costruisce l'index di tutti i possibili path dell'utente
		HashSet<String> string_labels = new HashSet<String>();
		TFloatIterator it = labels.iterator();
		while(it.hasNext())
			string_labels.add(StringUtils.extractRate(it.next(), ratesThreshold));
		
		TextFileUtils.computeIndex("path_index", path_index, string_labels);
		logger.info("Path index loading: " + path_index.size() + " paths loaded");
		
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
		
		try {
			
			BufferedWriter training_file = new BufferedWriter(new FileWriter("training_set"));
			BufferedWriter unknown_file = new BufferedWriter(new FileWriter("unknown_set"));
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_list = new ArrayList<String>(itemReader.getKeysIndex());
			itemReader.close();
			
			logger.info("Users: " + user_rating.size());
			logger.info("Items: " + items_list.size());
			
			int[] users = user_rating.keys();
			Arrays.sort(users);
			
			for(int user_id : users){
				
				// path extraction user-items
//				Runnable worker = new UserPathExtractorWorkerOptimized(user_id, 
//						user_rating.get(user_id), items_list, training_file, unknown_file, normalize,
//						items_path_index, path_file, path_index, paths, user_items_sampling,
//						ratesThreshold, items_link);
				Runnable worker = new UserPathExtractorWorker(user_id, 
						user_rating.get(user_id), items_list, training_file, unknown_file, normalize,
						items_path_index, path_file, path_index, paths, user_items_sampling,
						ratesThreshold, items_link);
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
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		// write path index
		TextFileUtils.writeData("user_path_index", path_index);
		
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		long start = System.currentTimeMillis();
		
		UserPathExtractor upe = new UserPathExtractor();
		upe.start();
		
		long stop = System.currentTimeMillis();
		logger.info("User path extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		//MemoryMonitor.stats();
		
	}
}
