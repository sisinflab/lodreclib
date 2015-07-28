package it.poliba.sisinflab.LODRec.sprank.userPathExtractor;

import gnu.trove.iterator.TFloatIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.fileManager.ItemFileManager;
import it.poliba.sisinflab.LODRec.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRec.main.Main;
import it.poliba.sisinflab.LODRec.utils.PropertyFileReader;
import it.poliba.sisinflab.LODRec.utils.StringUtils;
import it.poliba.sisinflab.LODRec.utils.TextFileUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

	private TObjectIntHashMap<String> path_index; // path index
	private TIntObjectHashMap<TIntFloatHashMap> trainRatings; // map
																// user-[item-rating]
	private TIntObjectHashMap<TIntFloatHashMap> validationRatings; // map
	// user-[item-rating]
	private TIntObjectHashMap<TIntFloatHashMap> testRatings; // map
	// user-[item-rating]

	private String trainRatingFile; // input user ratings filename
	private String validationRatingFile; // input user ratings filename
	private String testRatingFile; // input user ratings filename

	private boolean normalize;
	private String path_file;
	private String pathIndexFile;
	private String itemsFile;
	private THashMap<String, String> paths;
	private int paths_in_memory;
	private THashMap<String, String> items_path_index;
	private int user_items_sampling;
	private float ratingThreshold;
	private TIntObjectHashMap<TIntHashSet> items_link;
	private TFloatHashSet labels;
	private String itemLinkFile;

	private String trainFile;
	private String validationFile;
	private String testFile;
	private String userpathIndexFile;

	private int nThreads;

	private static Logger logger = LogManager.getLogger(UserPathExtractor.class
			.getName());

	/**
	 * Constuctor
	 */

	public UserPathExtractor(String workingDir, String trainRatingFile,
			String validationRatingFile, String testRatingFile,
			boolean normalize, String pathFile, String itemMetadataFile,
			int paths_in_memory, int user_items_sampling,
			float ratingThreshold, int nThreads) {

		this.workingDir = workingDir;
		this.trainRatingFile = trainRatingFile;
		this.validationRatingFile = validationRatingFile;
		this.testRatingFile = testRatingFile;
		this.normalize = normalize;
		this.path_file = pathFile;
		this.itemsFile = itemMetadataFile;
		this.paths_in_memory = paths_in_memory;
		this.user_items_sampling = user_items_sampling;
		this.ratingThreshold = ratingThreshold;
		this.nThreads = nThreads;

		init();

	}

	private void init() {

		this.pathIndexFile = this.workingDir + "path_index";
		this.itemLinkFile = workingDir + "items_link";

		this.trainFile = workingDir + "train";
		this.validationFile = workingDir + "validation";
		this.testFile = workingDir + "test";

		this.userpathIndexFile = workingDir + "user_path_index";

		this.trainRatings = loadInputRatingsFile(this.trainRatingFile);

		this.validationRatings = loadInputRatingsFile(this.validationRatingFile);

		this.testRatings = loadInputRatingsFile(this.testRatingFile);

		loadItemPaths();
		loadPathIndex();
		loadItemsLink();
	}

	private void loadItemsLink() {

		logger.info("Items link loading");
		items_link = new TIntObjectHashMap<TIntHashSet>();
		TextFileUtils.loadTIntMapTIntHashSet(this.itemLinkFile, items_link);

	}

	private void loadItemPaths() {

		// se non carico tutti i paths in memoria, calolo quali items tra quelli
		// votati dall'utente
		// sono più ricorrenti, in modo da privilegiare il caricamento in
		// memoria dei path più interessanti
		HashMap<Integer, Integer> items_count = new HashMap<Integer, Integer>();
		TIntObjectIterator<TIntFloatHashMap> it = trainRatings.iterator();
		while (it.hasNext()) {
			it.advance();
			for (int i : it.value().keys()) {
				if (!items_count.containsKey(i))
					items_count.put(i, 1);
				else {
					int old_count = items_count.get(i);
					items_count.put(i, old_count + 1);
				}
			}
		}

		logger.info("Paths file index loading");

		StringFileManager pathReader = new StringFileManager(path_file,
				items_count.keySet());

		items_path_index = new THashMap<String, String>(
				pathReader.getFileIndex());

		long num_item_pair = (paths_in_memory * items_path_index.size()) / 100;

		logger.info("Loading " + num_item_pair + " of "
				+ items_path_index.size() + " (" + paths_in_memory
				+ "%) item pair paths in memory");

		paths = pathReader.read(num_item_pair);

		logger.info("Paths loading completed");

		pathReader.close();

	}

	/**
	 * load user ratings from input file
	 * 
	 * @return
	 */
	private TIntObjectHashMap<TIntFloatHashMap> loadInputRatingsFile(String file) {

		TIntObjectHashMap<TIntFloatHashMap> m = new TIntObjectHashMap<TIntFloatHashMap>();
		labels = new TFloatHashSet();
		// load user-[item-rating] from input file
		TextFileUtils.loadInputUsersRatings(file, m, labels);

		logger.info("User ratings loaded (" + labels.size()
				+ " different labels)");

		return m;
	}

	private void loadPathIndex() {

		path_index = new TObjectIntHashMap<String>();

		// costruisce l'index di tutti i possibili path dell'utente
		HashSet<String> string_labels = new HashSet<String>();
		TFloatIterator it = labels.iterator();
		while (it.hasNext())
			string_labels.add(StringUtils.extractRate(it.next(),
					ratingThreshold));

		TextFileUtils.computeIndex(pathIndexFile, path_index, string_labels);
		logger.info("New Path index built: " + path_index.size()
				+ " paths loaded");

	}

	/**
	 * start path extraction
	 */
	public void start() {

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

			ItemFileManager itemReader = new ItemFileManager(itemsFile,
					ItemFileManager.READ);
			ArrayList<String> items_list = new ArrayList<String>(
					itemReader.getKeysIndex());
			itemReader.close();

			logger.info("Users: " + trainRatings.size());
			logger.info("Items: " + items_list.size());

			int[] users = trainRatings.keys();
			Arrays.sort(users);

			TIntSet testUsers = new TIntHashSet();
			//
			// users.addAll(trainRatings.keySet());
			// users.addAll(validationRatings.keySet());
			testUsers.addAll(testRatings.keySet());
			// int[] sorted_users = users.toArray();
			// Arrays.sort(sorted_users);
			// int limit =-1;

			// TIntSet items = new TIntHashSet();
			// for (String s : items_list)
			// items.add(Integer.parseInt(s));
			// if (!onlyTestSetEval) {
			// buildArtificialTestSet(items);
			// }
			// int count = 0;
			for (int user_id : users) {

				// if (count >= limit & limit!=-1)
				// break;
				// count++;
				// System.out.println("user " + user_id+" train: "
				// +trainRatings.get(user_id).size()+" valid: "
				// +validationRatings.get(user_id).size()+" test: "
				// +testRatings.get(user_id).size()+". tot items:"+items.size());
				// path extraction user-items
				Runnable worker = new UserPathExtractorWorker(user_id,
						trainRatings.get(user_id),
						validationRatings.get(user_id), items_list, train_file,
						validation_file, test_file, normalize,
						items_path_index, path_file, path_index, paths,
						user_items_sampling, ratingThreshold, items_link,
						testUsers);

				// run the worker thread
				executor.execute(worker);

			}

			// This will make the executor accept no new threads
			// and finish all existing threads in the queue
			executor.shutdown();
			// Wait until all threads are finish
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

			train_file.flush();
			train_file.close();

			test_file.flush();
			test_file.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		// write path index
		TextFileUtils.writeData(userpathIndexFile, path_index);

	}

	private void buildArtificialTestSet(TIntSet items) {

		int item_id;
		TIntSet tmp = null;
		TIntSet trainItems = null;
		for (int user_id : testRatings.keys()) {
			// int [] testItems=testRatings.get(user_id).keys();
			TIntSet testItems = testRatings.get(user_id).keySet();
			tmp = new TIntHashSet();
			tmp.addAll(items);
			if (trainRatings.contains(user_id)) {
				trainItems = trainRatings.get(user_id).keySet();
				if (trainItems != null)
					tmp.removeAll(trainItems);
			}
			tmp.removeAll(testItems);
			TIntIterator it = tmp.iterator();

			while (it.hasNext()) {
				item_id = it.next();
				testRatings.get(user_id).put(item_id, 0);
			}
			// da valutare se aggiungere anche validation items a quelli da
			// rimuovere

		}

	}

//	/**
//	 * @param args
//	 * @throws IOException
//	 */
//	public static void main(String[] args) throws IOException {
//		// TODO Auto-generated method stub
//
//		long start = System.currentTimeMillis();
//
//		UserPathExtractor upe = new UserPathExtractor();
//		upe.start();
//
//		long stop = System.currentTimeMillis();
//		logger.info("User path extraction terminated in [sec]: "
//				+ ((stop - start) / 1000));
//
//		// MemoryMonitor.stats();
//
//	}
}
