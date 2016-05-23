package it.poliba.sisinflab.LODRec.sprank.userPathExtractor;

import gnu.trove.iterator.TFloatIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.fileManager.FileManager;
import it.poliba.sisinflab.LODRec.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRec.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRec.utils.StringUtils;
import it.poliba.sisinflab.LODRec.utils.TextFileUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
	
	private String workingDir;
	private int nThreads;
	
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<TIntFloatHashMap> trainRatings; 
	// map user-[item-rating]
	private TIntObjectHashMap<TIntFloatHashMap> validationRatings; 
	// map user-[item-rating]
	private String trainRatingFile; // input user train ratings filename
	private String validationRatingFile; // input user validation ratings filename
	private boolean normalize;
	private String path_file;
	private String pathIndexFile;
	private String itemsFile;
	private THashMap<String, String> paths;
	private int paths_in_memory;
	private THashMap<String, String> items_path_index;
	private int user_items_sampling;
	private float ratesThreshold;
	private TIntObjectHashMap<TIntHashSet> items_link;
	private TFloatHashSet labels;
	private String itemLinkFile;
	private String trainFile;
	private String validationFile;
	private String testFile;
	private String userPathIndexFile;
	private boolean splitValidationSet;
	private int numUsersValidationSet;
	
	private static Logger logger = LogManager.getLogger(UserPathExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */

	public UserPathExtractor(String workingDir, String trainRatingFile,
			String validationRatingFile, boolean normalize, String pathFile, 
			String itemMetadataFile, int paths_in_memory, int user_items_sampling,
			float ratingThreshold, int nThreads, boolean splitValidationSet) {

		this.workingDir = workingDir;
		this.trainRatingFile = trainRatingFile;
		this.validationRatingFile = validationRatingFile;
		this.normalize = normalize;
		this.path_file = pathFile;
		this.itemsFile = itemMetadataFile;
		this.paths_in_memory = paths_in_memory;
		this.user_items_sampling = user_items_sampling;
		this.ratesThreshold = ratingThreshold;
		this.nThreads = nThreads;
		this.splitValidationSet = splitValidationSet;

		init();

	}

	private void init() {

		this.pathIndexFile = this.workingDir + "path_index";
		this.itemLinkFile = workingDir + "items_link";
		
		this.trainFile = workingDir + "train";
		this.validationFile = workingDir + "validation";
		this.testFile = workingDir + "test";
		
		this.userPathIndexFile = workingDir + "user_path_index";

		loadRatings();
		loadItemPaths();
		buildPathIndex();
		loadItemsLink();		
		
	}
	
	private void loadRatings() {
		
		labels = new TFloatHashSet();
		
		trainRatings = new TIntObjectHashMap<TIntFloatHashMap>();
		TextFileUtils.loadInputUsersRatings(trainRatingFile, trainRatings, labels);
		
		if(splitValidationSet)
			numUsersValidationSet = trainRatings.size()/nThreads + 1;
		else
			numUsersValidationSet = trainRatings.size();
		
		logger.info("Users per test set: " + numUsersValidationSet + "("
				+ trainRatings.size() + ")");
		
		validationRatings = new TIntObjectHashMap<TIntFloatHashMap>();
		TextFileUtils.loadInputUsersRatings(validationRatingFile, validationRatings, labels);
		
	}
	
	
	private void loadItemsLink(){
		
		logger.info("Items link loading");
		items_link = new TIntObjectHashMap<TIntHashSet>();
		TextFileUtils.loadTIntMapTIntHashSet(itemLinkFile, items_link);
		
	}
	
	
	private void loadItemPaths(){
		
		StringFileManager pathReader;
		
		/* se non carico tutti i paths in memoria, 
		 * calcolo quali items, tra quelli votati dall'utente, 
		 * sono più ricorrenti, in modo da privilegiare il caricamento
		 * in memoria dei path più interessanti.
		 */
		if(paths_in_memory < 100) {
			HashMap<Integer, Integer> items_count = new HashMap<Integer, Integer>();
			TIntObjectIterator<TIntFloatHashMap> it = trainRatings.iterator();
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
			
			pathReader = new StringFileManager(path_file, items_count.keySet());
		}
		else
			pathReader = new StringFileManager(path_file, FileManager.READ);
		
		
		logger.info("Paths file index loading");
		
		items_path_index = new THashMap<String, String>(pathReader.getFileIndex());
		
		long num_item_pair = (paths_in_memory * items_path_index.size()) / 100;
		
		logger.info("Loading " + num_item_pair + " of " + items_path_index.size() +
				" (" + paths_in_memory + "%) item pair paths in memory");
		
		paths = pathReader.read(num_item_pair);
		
		logger.info("Paths loading completed");
		
		pathReader.close();
		
	}
	
	private void buildPathIndex(){
		
		path_index = new TObjectIntHashMap<String>();
		
		// costruisce l'index di tutti i possibili path dell'utente
		HashSet<String> string_labels = new HashSet<String>();
		TFloatIterator it = labels.iterator();
		while(it.hasNext())
			string_labels.add(StringUtils.extractRate(it.next(), ratesThreshold));
		
		TextFileUtils.computeIndex(pathIndexFile, path_index, string_labels);
		logger.info("New path index built: " + path_index.size() + " paths loaded");
		
	}
	
	
	/**
	 * start path extraction
	 */
	public void start(){
		
		//nThreads = 4;
		logger.debug("Threads number: " + nThreads);
		
		ExecutorService executor;
		executor = Executors.newFixedThreadPool(nThreads);
		
		try {
			
			BufferedWriter train_file = new BufferedWriter(new FileWriter(
					trainFile));
			
			BufferedWriter validation_file = new BufferedWriter(new FileWriter(
					validationFile));
			
			BufferedWriter test_file = new BufferedWriter(new FileWriter(
					testFile));
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			ArrayList<String> items_list = new ArrayList<String>(itemReader.getKeysIndex());
			itemReader.close();
			
			logger.info("Users: " + trainRatings.size());
			logger.info("Items: " + items_list.size());
			
			int[] users = trainRatings.keys();
			Arrays.sort(users);
			
			int count = 0;
			for(int user_id : users){
				
				if(executor.isTerminated()) {
		    		executor = Executors.newFixedThreadPool(nThreads);
		    		test_file = new BufferedWriter(new FileWriter(
							testFile + "_" + count));		    		
				}
				
				// path extraction worker user-items
				Runnable worker = new UserPathExtractorWorker(user_id, 
						trainRatings.get(user_id), validationRatings.get(user_id),
						items_list, train_file, validation_file, test_file, 
						normalize, items_path_index, path_file, path_index, paths, 
						user_items_sampling, ratesThreshold, items_link);
				// run the worker thread  
				executor.execute(worker);
				
				count++;
				
				if(count % numUsersValidationSet == 0) {
					
					executor.shutdown();
					executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					test_file.flush();
				    test_file.close();
				    
				}
				
			}
			
			if(!executor.isTerminated()) {
			
				// This will make the executor accept no new threads
			    // and finish all existing threads in the queue
			    executor.shutdown();
			    // Wait until all threads are finish
			    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			    
			    test_file.flush();
			    test_file.close();
			    
			}
		    
		    train_file.flush();
		    train_file.close();
		    
		    validation_file.flush();
		    validation_file.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		// write path index
		TextFileUtils.writeData(userPathIndexFile, path_index);
		
	}
}
