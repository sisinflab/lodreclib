package it.poliba.sisinflab.LODRec.recommender;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectFloatHashMap;
import it.poliba.sisinflab.LODRec.main.Main;
import it.poliba.sisinflab.LODRec.utils.BST;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.RankerFactory;
import ciir.umass.edu.learning.SparseDataPoint;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;

public class Recommender {
	private String workingDir;

	private String testFile;
	private String outFile;
	private boolean implicit;
	private Ranker rankLibModel;
	private Model libLinearModel;

	private int topN;
	private String model_file;
	private static Logger logger = LogManager.getLogger(Recommender.class
			.getName());

	private RankerFactory rFact;
	private boolean libLinear;
	private boolean rankLib;
	THashMap<Integer, TObjectFloatHashMap<String>> user_profiles;


	public Recommender(Model libLinearModel, int topN) {

		try {
			this.workingDir = Main.workingDir;
			this.testFile = workingDir + "test";
			this.libLinearModel = libLinearModel;
			this.topN = topN;

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Recommender(String workingDir, String reccOutputFile, int topN,
			 boolean implicit, String testRatingFile,
			boolean libLinear, boolean rankLib) {

		this.outFile = reccOutputFile;
		this.workingDir = workingDir;
		this.topN = topN;
		this.implicit = implicit;
		this.libLinear = libLinear;
		this.rankLib = rankLib;

		init();

	}

	private void init() {

		this.testFile = workingDir + "test";
		this.model_file = workingDir +"bestModel";
		loadModel();

	}

	private void loadModel() {

		logger.info("Loading prediction model");

		try {
			if (libLinear) {

				File modelFile = new File(model_file);
				libLinearModel = Model.load(modelFile);

			} else if (rankLib) {

				rFact = new RankerFactory();
				rankLibModel = rFact.loadRanker(model_file);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private double computePred(FeatureNode[] f) {
		double pred = 0;
		double[] prob_estimates = new double[2];

		if (libLinearModel.isProbabilityModel() & implicit) {

			prob_estimates = new double[2];

			Linear.predictProbability(libLinearModel, f, prob_estimates);
			pred = prob_estimates[0];

		} else
			pred += Linear.predict(libLinearModel, f);

		return pred;
	}

	private void computeReccFromTestFileWithRankLib() {
		logger.info("Start computing reccomendations");

		Map<Integer, BST> predictions = new HashMap();

		try {
			BufferedReader br = new BufferedReader(new FileReader(testFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));

			double pred = 0;

			String line = null;
			while ((line = br.readLine()) != null) {

				String[] vals = line.split(" ");

				int user_id = Integer.parseInt(vals[1].split(":")[1]);
				int item_id = Integer.parseInt(vals[2].split(":")[1]);

				DataPoint p = new SparseDataPoint(line);

				pred = rankLibModel.predictScore(p);

				if (!predictions.containsKey(user_id))
					predictions.put(user_id, new BST(topN));

				predictions.get(user_id).insert(pred, item_id);

			}

			List<Integer> users = new ArrayList();
			users.addAll(predictions.keySet());
			Collections.sort(users);

			for (int user_id : users) {
				bw.append(user_id + "\t");
				BST bst = predictions.get(user_id);
				bst.visit();
				List<Double> list_scores = bst.getSortedKeys();
				List<Integer> list_items = bst.getSortedValues();

				for (int i = 0; i < list_items.size(); i++) {
					bw.append(list_items.get(i)
							+ ":"
							+ String.format("%.3f", list_scores.get(i))
									.replace(",", ".") + " ");
				}
				bw.newLine();
			}

			br.close();
			bw.flush();
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void computeReccFromTestFileWithLibLinear() {

		logger.info("Start computing reccomendations");

		Map<Integer, BST> predictions = new HashMap();

		try {
			BufferedReader br = new BufferedReader(new FileReader(testFile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));

			double[] prob_estimates;

			String line = null;
			while ((line = br.readLine()) != null) {

				String[] vals = line.split(" ");
				FeatureNode[] f = new FeatureNode[vals.length - 3];
				for (int i = 3; i < vals.length; i++) {
					String[] ss = vals[i].split(":");
					int key = Integer.parseInt(ss[0]);
					double value = Double.parseDouble(ss[1]);
					f[i - 3] = new FeatureNode(key, value);
				}

				double pred = computePred(f);

				int user_id = Integer.parseInt(vals[1].split(":")[1]);
				int item_id = Integer.parseInt(vals[2].split(":")[1]);

				if (!predictions.containsKey(user_id))
					predictions.put(user_id, new BST(topN));

				predictions.get(user_id).insert(pred, item_id);

			}

			List<Integer> users = new ArrayList();
			users.addAll(predictions.keySet());
			Collections.sort(users);

			for (int user_id : users) {
				bw.append(user_id + "\t");
				BST bst = predictions.get(user_id);
				bst.visit();
				List<Double> list_scores = bst.getSortedKeys();
				List<Integer> list_items = bst.getSortedValues();

				for (int i = 0; i < list_items.size(); i++) {
					bw.append(list_items.get(i)
							+ ":"
							+ String.format("%.3f", list_scores.get(i))
									.replace(",", ".") + " ");
				}
				bw.newLine();
			}

			br.close();
			bw.flush();
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Map<Integer, List<Integer>> computeRecc(String file,
			Set<Integer> users, boolean implicit) {

		Map<Integer, List<Integer>> recc = new HashMap<Integer, List<Integer>>();
		Map<Integer, BST> predictions = new HashMap();

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));

			String line = null;

			while ((line = br.readLine()) != null) {

				String[] vals = line.split(" ");
				FeatureNode[] f = new FeatureNode[vals.length - 3];

				int user_id = Integer.parseInt(vals[1].split(":")[1]);

				if (users.contains(user_id)) {

					int item_id = Integer.parseInt(vals[2].split(":")[1]);

					for (int i = 3; i < vals.length; i++) {
						String[] ss = vals[i].split(":");
						int key = Integer.parseInt(ss[0]);
						double value = Double.parseDouble(ss[1]);
						f[i - 3] = new FeatureNode(key, value);
					}

					double pred = computePred(f);

					if (!predictions.containsKey(user_id))
						predictions.put(user_id, new BST(topN));

					predictions.get(user_id).insert(pred, item_id);

				}
			}

			for (int user_id : predictions.keySet()) {
				BST bst = predictions.get(user_id);
				bst.visit();
				List<Integer> list_items = bst.getSortedValues();
				recc.put(user_id, list_items);
			}

			br.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return recc;

	}

	public Map<Integer, List<Integer>> computeRecc(FeatureNode[][] X,
			int[] userIndex, int[] itemIndex, boolean implicit) {

		Map<Integer, List<Integer>> recc = new HashMap<Integer, List<Integer>>();
		// logger.info("Start computing reccomendations");
		// usare un Btree invece della mappa per tener traccia solo dei topN

		Map<Integer, BST> predictions = new HashMap();

		double[] prob_estimates;
		double pred;
		for (int i = 0; i < X.length; i++) {

			if (libLinearModel.isProbabilityModel() & implicit) {
				prob_estimates = new double[2];
				Linear.predictProbability(libLinearModel, X[i], prob_estimates);
				pred = prob_estimates[0];
			} else
				pred = Linear.predict(libLinearModel, X[i]);

			int user_id = userIndex[i];
			int item_id = itemIndex[i];

			if (!predictions.containsKey(user_id))
				predictions.put(user_id, new BST(topN));

			predictions.get(user_id).insert(pred, item_id);

		}

		for (int user_id : predictions.keySet()) {
			BST bst = predictions.get(user_id);
			bst.visit();
			List<Integer> list_items = bst.getSortedValues();
			recc.put(user_id, list_items);
		}
		return recc;

	}

	public void computeRecc() {
		if (libLinear)
			computeReccFromTestFileWithLibLinear();
		else if (rankLib)
			computeReccFromTestFileWithRankLib();
	}
}