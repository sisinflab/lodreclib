package it.poliba.sisinflab.LODRec.graphkernel.model;

import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import it.poliba.sisinflab.LODRec.evaluation.Evaluator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class UserModelRecommender {
	// private static final Float NEG_FB_VALUE = 0f;
	private static final Float POS_FB_VALUE = 1f;

	// private String workingDir;
	private TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures;

	private String outFile;
	private int nThreads;
	private boolean implicit;
	private int num_features;

	private List<Double> listC = new ArrayList<Double>();
	private List<Double> listEps = new ArrayList<Double>();
	private List<Integer> listSolverType = new ArrayList<Integer>();

	private BufferedWriter bw;
	Map<Integer, Map<Integer, Float>> trainRatings;
	Map<Integer, Map<Integer, Float>> validationRatings;

	float evalRatingThresh, negRatingThresh, relUnknownItems;

	private String validRatingFile;
	private String trainRatingFile;

	private boolean silent = true;

	private int topN;
	private HashSet<Integer> items;
	private boolean addNegValidationEx;

	private int timesRealFb;
	private int nValidNegEx;
	private int minTrainEx;
	private int embeddingOption;
	private String entityMapFile;
	private String branchMapFile;
	private String listStrSolverType;
	private String listStrC;
	private String listStrEps;
	private String itemMetadataFile;
	
	private static Logger logger = LogManager
			.getLogger(UserModelRecommender.class.getName());

	private void init() {

		if (embeddingOption == 1)
			itemMetadataFile = itemMetadataFile + entityMapFile;
		else if (embeddingOption == 2)
			itemMetadataFile = itemMetadataFile + branchMapFile;

		String[] parts = listStrC.split(",");
		for (int i = 0; i < parts.length; i++) {
			double val = Double.parseDouble(parts[i]);
			listC.add(val);
		}
		parts = listStrEps.split(",");
		for (int i = 0; i < parts.length; i++) {
			double val = Double.parseDouble(parts[i]);
			listEps.add(val);
		}
		parts = listStrSolverType.split(",");
		for (int i = 0; i < parts.length; i++) {
			int val = Integer.parseInt(parts[i]);
			listSolverType.add(val);
		}

		this.loadItemFeatureData(itemMetadataFile);

		trainRatings = this.loadRatingData(trainRatingFile);

		this.validationRatings = this.loadRatingData(validRatingFile);

	}

	public UserModelRecommender(int topN, int nThreads, String reccOutputFile,
			String itemMetadataFile, int embeddingOption, String entityMapFile,
			String branchMapFile, String trainRatingFile,
			String validationRatingFile, boolean implicit,
			String listStrSolverType, String listStrC, String listStrEps,
			float evalRatingThresh, float relUnknownItems, float negRatingThresh,
			int timesRealFb, int nValidNegEx, int minTrainEx, boolean addNegValidationEx) {

		this.topN = topN;
		this.nThreads = nThreads;
		this.outFile = reccOutputFile;
		this.itemMetadataFile = itemMetadataFile;
		this.embeddingOption = embeddingOption;
		this.entityMapFile = entityMapFile;
		this.branchMapFile = branchMapFile;
		this.trainRatingFile = trainRatingFile;
		this.validRatingFile = validationRatingFile;
		this.implicit = implicit;
		this.listStrSolverType = listStrSolverType;
		this.listStrC = listStrC;
		this.listStrEps = listStrEps;
		this.evalRatingThresh = evalRatingThresh;
		this.relUnknownItems = relUnknownItems;
		this.negRatingThresh = negRatingThresh;
		this.timesRealFb = timesRealFb;
		this.nValidNegEx = nValidNegEx;
		this.minTrainEx = minTrainEx;
		this.addNegValidationEx = addNegValidationEx;

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

		num_features = maxfID;

		avg = avg / (float) map_item_intFeatures.keySet().size();
		System.out
				.println("item data loading terminated. avg features (considering also collaborative features) per item: "
						+ avg + ". n. features in the index: " + num_features);

	}

	public void exec() {

		try {
			bw = new BufferedWriter(new FileWriter(outFile));

			List<Integer> topK = Arrays.asList(10, 25, 50);

			Evaluator trainEval = new Evaluator(trainRatingFile, topK,
					evalRatingThresh, relUnknownItems);

			Evaluator validEval = new Evaluator(validRatingFile, topK,
					evalRatingThresh, relUnknownItems);

			ExecutorService executor;
			executor = Executors.newFixedThreadPool(nThreads);

			int limit = -1;
			int count = 0;
			for (int u : trainRatings.keySet()) {

				count++;
				if (count == limit)
					break;
				// train ------------------

				Map<Integer, Float> userTrainRatings = trainRatings.get(u);
				Map<Integer, Float> userValRatings = validationRatings.get(u);

				Runnable worker = new UserModelRecommenderWorker(u, bw,
						map_item_intFeatures, trainEval, validEval, silent,
						topN, num_features, listC, listEps, listSolverType,
						userTrainRatings, userValRatings, implicit,
						nValidNegEx, addNegValidationEx, timesRealFb, minTrainEx, items,
						relUnknownItems);
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
