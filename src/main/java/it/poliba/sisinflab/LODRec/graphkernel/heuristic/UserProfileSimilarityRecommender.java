package it.poliba.sisinflab.LODRec.graphkernel.heuristic;

import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class UserProfileSimilarityRecommender {

	private static final float POS_FB_VALUE = 1;

	private TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures;

	private String outFile;
	private boolean implicit;
	private BufferedWriter bw;
	Map<Integer, Map<Integer, Float>> trainRatings;

	private String trainRatingFile;
	private TIntObjectHashMap<TIntFloatHashMap> itemSim;
	private int topN;
	private HashSet<Integer> items;
	private int embeddingOption;
	private String entityMapFile;
	private String branchMapFile;
	private static final float MIN_SIM = 0f;
	private String itemMetadataFile;

	private Float evalRatingThresh;

	private int nThreads;

	private static Logger logger = LogManager
			.getLogger(UserProfileSimilarityRecommender.class.getName());

	private void init() {

		if (embeddingOption == 1)
			itemMetadataFile = itemMetadataFile + entityMapFile;
		else if (embeddingOption == 2)
			itemMetadataFile = itemMetadataFile + branchMapFile;

		this.loadItemFeatureData(itemMetadataFile);

		trainRatings = this.loadRatingData(trainRatingFile);
	}

	public UserProfileSimilarityRecommender(int topN, String reccOutputFile,
			String itemMetadataFile, int embeddingOption, String entityMapFile,
			String branchMapFile, String trainRatingFile, boolean implicit,
			float evalRatingThresh,int nThreads) {

		this.topN = topN;
		this.outFile = reccOutputFile;
		this.itemMetadataFile = itemMetadataFile;
		this.embeddingOption = embeddingOption;
		this.entityMapFile = entityMapFile;
		this.branchMapFile = branchMapFile;
		this.trainRatingFile = trainRatingFile;
		this.implicit = implicit;
		this.evalRatingThresh = evalRatingThresh;
		this.nThreads=nThreads;
		init();
	}

	private void loadItemFeatureData(String file_name) {

		int maxfID = 0;
		this.items = new HashSet<Integer>();
		map_item_intFeatures = new TIntObjectHashMap<TIntFloatHashMap>();
		BufferedReader br;
		float avg = 0;
		try {
			br = new BufferedReader(new FileReader(file_name));
			String line = null;
			while ((line = br.readLine()) != null) {
				try {
					String[] vals = line.split("\t");
					int id = Integer.parseInt(vals[0]);

					if (!items.contains(id))
						items.add(id);

					map_item_intFeatures.put(id, new TIntFloatHashMap());

					String[] values = vals[1].trim().split(" ");
					for (int i = 0; i < values.length; i++) {
						String[] pair = values[i].split(":");
						int fId = Integer.parseInt(pair[0]);
						float fVal = Float.parseFloat(pair[1]);
						map_item_intFeatures.get(id).put(fId, fVal);

						if (fId > maxfID)
							maxfID = fId;
					}

					avg += map_item_intFeatures.get(id).size();

				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					System.out.println(line);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		avg = avg / (float) map_item_intFeatures.keySet().size();
		System.out
				.println("item data loading terminated. avg features (considering also collaborative features) per item: "
						+ avg);

	}

	public void exec() {

		try {
			bw = new BufferedWriter(new FileWriter(outFile));
			ExecutorService executor;
			executor = Executors.newFixedThreadPool(nThreads);

			for (int u : trainRatings.keySet()) {

				Map<Integer, Float> userTrainRatings = trainRatings.get(u);

				Runnable worker = new UserProfileSimilarityRecommenderWorker(u,items,
						bw, map_item_intFeatures, topN, userTrainRatings,
						implicit,evalRatingThresh);
				// run the worker thread
				executor.execute(worker);
			}
			
			executor.shutdown();
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);


			bw.flush();
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	

	public Map<Integer, Map<Integer, Float>> loadRatingData(String filename) {
		Map<Integer, Map<Integer, Float>> ratings = new HashMap<Integer, Map<Integer, Float>>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line = reader.readLine();

			while (line != null) {

				String[] str = line.split("\t");

				int u = Integer.parseInt(str[0].trim());
				int i = Integer.parseInt(str[1].trim());
				float rel = Float.parseFloat(str[2].trim());

				if (items.contains(i)) {
					if (implicit)
						rel = POS_FB_VALUE;
					if (!ratings.containsKey(u))
						ratings.put(u, new HashMap());
					ratings.get(u).put(i, rel);
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
		}
		System.out.println(filename + " . loaded " + ratings.size() + " users");
		return ratings;
	}

}
