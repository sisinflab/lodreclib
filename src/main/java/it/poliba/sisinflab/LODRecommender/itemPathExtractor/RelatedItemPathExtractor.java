package it.poliba.sisinflab.LODRecommender.itemPathExtractor;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRecommender.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRecommender.fileManager.TextFileManager;
import it.poliba.sisinflab.LODRecommender.itemManager.ItemTree;
import it.poliba.sisinflab.LODRecommender.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRecommender.utils.SynchronizedCounter;
import it.poliba.sisinflab.LODRecommender.utils.TextFileUtils;

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
 * This class extracts paths from item trees 
 * 
 * @author Vito Mastromarino
 */
public class RelatedItemPathExtractor {
	
	private String itemsFile; // metadata file name
	private boolean inverseProps; // directed property
	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<String> inverse_path_index; // key-value path index
	private TIntObjectHashMap<String> props_index; // property index
	private SynchronizedCounter counter; // synchronized counter for path index
	private boolean outputPathTextFormat;
	private boolean outputPathBinaryFormat;
	private boolean computeInversePaths;
	private String path_file;
	private TIntObjectHashMap<TIntArrayList> related_uri;
	private TObjectIntHashMap<String> uri_id;
	private TIntIntHashMap input_metadata_id;
	private boolean selectTopPaths;
	private TIntObjectHashMap<TIntHashSet> items_link;
	
	private static Logger logger = LogManager.getLogger(RelatedItemPathExtractor.class.getName());
	
	
	/**
	 * Constuctor
	 */
	public RelatedItemPathExtractor(){
		
		// load config file
		Map<String, String> prop = null;
		
		try {
			prop = PropertyFileReader.loadProperties("config.properties");
			this.inverseProps =  Boolean.valueOf(prop.get("directed"));
			this.itemsFile = prop.get("itemsFile");
			this.path_file = prop.get("outputPathFile");
			this.selectTopPaths =  Boolean.valueOf(prop.get("computeTopPaths"));
			this.outputPathTextFormat =  Boolean.valueOf(prop.get("outputExtractionTextFormat"));
			this.outputPathBinaryFormat =  Boolean.valueOf(prop.get("outputExtractionBinaryFormat"));
			this.computeInversePaths =  Boolean.valueOf(prop.get("computeInversePaths"));
			
			loadPropsIndex();
			loadResourceID();
			loadRelatedURIs();
			loadInputMetadataID();
			
			if(selectTopPaths){
				logger.debug("Selection of top paths abilited");
				computeTopPaths();
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prop.clear();
	}
	
	private void computeTopPaths(){
		
		TopItemPathExtractor top = new TopItemPathExtractor();
		top.start();
		
		loadPathIndex();
		
	}
	
	/**
	  * load path index
	  */
	private void loadPathIndex(){
		path_index = new TObjectIntHashMap<String>();
		inverse_path_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("path_index", path_index);
		
		if(computeInversePaths)
			TextFileUtils.loadIndex("path_index", inverse_path_index);
		
		logger.debug("Path index loading");
	}
	
	
	
	/**
	  * load property index
	  */
	private void loadPropsIndex(){
		props_index = new TIntObjectHashMap<String>();
		TextFileUtils.loadIndex("props_index", props_index);
		logger.debug("Properties index loading");
	}
	
	
	/**
	  * load resource index
	  */
	private void loadResourceID(){
		uri_id = new TObjectIntHashMap<String>();
		TextFileUtils.loadIndex("input_uri_id", uri_id);
		logger.debug("Input uri's IDs loading");
	}
	
	/**
	  * load related uri
	  */
	private void loadRelatedURIs(){
		related_uri = new TIntObjectHashMap<TIntArrayList>();
		TextFileUtils.loadRelatedURIs("related_uri.txt", related_uri, uri_id);
		logger.debug("Related items loading");
	}
	
	/**
	  * load metadata input id
	  */
	private void loadInputMetadataID(){
		input_metadata_id = new TIntIntHashMap();
		TextFileUtils.loadInputMetadataID("metadata_index", "input_uri_id", input_metadata_id);
		System.out.println(input_metadata_id.size());
		logger.debug("Input metadata index loading");
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
		counter = new SynchronizedCounter();
		items_link = new TIntObjectHashMap<TIntHashSet>();
		
		if(!selectTopPaths){
			path_index = new TObjectIntHashMap<String>();
			if(computeInversePaths)
				inverse_path_index = new TIntObjectHashMap<String>();
		}
		
		try {
			
			TextFileManager textWriter = null;
			if(outputPathTextFormat)
				textWriter = new TextFileManager(path_file + ".txt");
			
			StringFileManager pathWriter = null;
			if(outputPathBinaryFormat)
				pathWriter = new StringFileManager(path_file, StringFileManager.WRITE);
			
			ItemFileManager itemReader = new ItemFileManager(itemsFile, ItemFileManager.READ);
			
			ItemTree tmp = null;
			for(int main_item_id : related_uri.keys()){
				
				tmp = itemReader.read(Integer.toString(main_item_id));
				
				if(tmp!=null){
					
					TIntArrayList items_id_list = related_uri.get(main_item_id);
					ArrayList<ItemTree> items_o = new ArrayList<ItemTree>(items_id_list.size());
					
					TIntIterator it = items_id_list.iterator();
					ItemTree tmp1 = null;
					while(it.hasNext()){
						
						tmp = itemReader.read(Integer.toString(it.next()));
						if(tmp1!=null)
							items_o.add(tmp1);
					}
					
					Runnable worker = new ItemPathExtractorWorker(counter, path_index, inverse_path_index,
							tmp, items_o, props_index, inverseProps, textWriter, pathWriter, selectTopPaths, 
							input_metadata_id, computeInversePaths, items_link);
					// run the worker thread  
					executor.execute(worker);
					
				}
				
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			
			itemReader.close();
			
			if(textWriter!=null)
				textWriter.close();
		    
		    if(pathWriter!=null)
		    	pathWriter.close();
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		// write path index
		TextFileUtils.writeData(path_file + "_index", path_index);
		TextFileUtils.writeTIntMapTIntHashSet("items_link", items_link);
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		RelatedItemPathExtractor pe = new RelatedItemPathExtractor();
		
		long start = System.currentTimeMillis();
		
		pe.start();
		
		long stop = System.currentTimeMillis();
		logger.info("Related item paths extraction terminated in [sec]: " 
				+ ((stop - start) / 1000));
		
		//MemoryMonitor.stats();
		
	}
}
