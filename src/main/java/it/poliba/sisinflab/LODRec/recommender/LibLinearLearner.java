package it.poliba.sisinflab.LODRec.recommender;

import it.poliba.sisinflab.LODRec.evaluation.Evaluator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;

public class LibLinearLearner {

	// IMPORTANTE -> RICORDARSI CHE LA PRIMA FEATURE Ãˆ L'ITEM ID QUINDI NN DEVE
	// ESSER CONSIDERATA

	private String workingDir;

	private String user_path_index_file;
	private int num_features;

	private FeatureNode[][] XTrain;
	private int[] trainUserIndex; // user_id corresponding to row_i in
									// the dataset
	private int[] trainItemIndex; // item_id corresponding to row_i in
									// the dataset
	private int topN = 100;
	private double[] yTrain;
	private Problem problem;
	private List<Double> listC = new ArrayList<Double>();
	private List<Double> listEps = new ArrayList<Double>();
	private List<Double> listP = new ArrayList<Double>();
	private List<Integer> listSolverType = new ArrayList<Integer>();
	private String modelFileName;
	private static Logger logger = LogManager.getLogger(LibLinearLearner.class
			.getName());
	private String trainingFile;
	private String validationFile;
	Map<Integer, Map<Integer, Float>> trainRatings;
	Map<Integer, Map<Integer, Float>> validationRatings;
	float thresh, relUnknownItems;
	private String validRatingFile;
	private String trainRatingFile;
	private Set<Integer> validationUsers;
	private boolean silent = true;
	private boolean implicit; // if implicit change the trainset loading
	private String listStrSolverType;
	private String listStrC;
	private String listStrEps;
	private String listStrP;

	// private int n_splits;

	public LibLinearLearner(String workingDir, String trainRatingFile,
			String validationRatingFile, float evalRatingThresh,
			float relUnknownItems, boolean implicit, boolean silentLearning,
			String listStrSolverType, String listStrC, String listStrEps,
			String listStrP) {

		this.workingDir = workingDir;
		this.trainRatingFile = trainRatingFile;
		this.validRatingFile = validationRatingFile;
		this.thresh = evalRatingThresh;
		this.relUnknownItems = relUnknownItems;
		this.implicit = implicit;
		this.silent = silentLearning;
		this.listStrSolverType = listStrSolverType;
		this.listStrC = listStrC;
		this.listStrEps = listStrEps;
		this.listStrP = listStrP;
		init();
	}

	private void init() {

		trainRatings = this.loadRatingData(trainRatingFile);

		this.validationRatings = this.loadRatingData(validRatingFile);
		this.validationUsers = new HashSet();
		this.validationUsers.addAll(validationRatings.keySet());

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
		parts = listStrP.split(",");
		for (int i = 0; i < parts.length; i++) {
			double val = Double.parseDouble(parts[i]);
			listP.add(val);
		}
		parts = listStrSolverType.split(",");
		for (int i = 0; i < parts.length; i++) {
			int val = Integer.parseInt(parts[i]);
			listSolverType.add(val);
		}

		this.user_path_index_file = workingDir + "user_path_index";
		countNumFeatures();

		this.trainingFile = workingDir + "train";
		this.validationFile = workingDir + "validation";

	}

	private void countNumFeatures() {

		try {

			int count = 0;

			BufferedReader br = new BufferedReader(new FileReader(
					user_path_index_file));
			while (br.readLine() != null)
				count++;

			num_features = count;
			br.close();

		} catch (Exception e) {
			e.printStackTrace();
			num_features = 0;
		}

	}

	private void loadTrainDataset(String trainFile) {

		logger.info("Loading training set - " + trainFile);

		int nRows = computeTrainRows(trainFile);

		try {
			System.out.println("n.rows to read: " + nRows);
			BufferedReader br = new BufferedReader(new FileReader(trainFile));

			XTrain = new FeatureNode[nRows][];
			yTrain = new double[nRows];
			trainUserIndex = new int[nRows];
			trainItemIndex = new int[nRows];
			String line = null;
			int j = 0;
			while ((line = br.readLine()) != null) {
				if (j % 100000 == 0)
					System.out.println(j + " lines read");
				String[] vals = line.split(" ");
				FeatureNode[] f = new FeatureNode[vals.length - 3];

				for (int i = 3; i < vals.length; i++) {
					String[] ss = vals[i].split(":");
					int key = Integer.parseInt(ss[0]);
					float value = Float.parseFloat(ss[1]);
					key = key - 1; // from [2...] to [1...]
					f[i - 3] = new FeatureNode(key, value);
				}

				int user_id = Integer.parseInt(vals[1].split(":")[1]);
				int item_id = Integer.parseInt(vals[2].split(":")[1]);

				trainUserIndex[j] = user_id;
				trainItemIndex[j] = item_id;

				XTrain[j] = f;
				yTrain[j] = Double.parseDouble(vals[0]);
				j++;

			}

			br.close();

			logger.info("training set loading terminated. n.rows: "
					+ XTrain.length + " n.cols: " + this.num_features);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private int computeTrainRows(String trainFile) {
		boolean fastComp = false;
		int nRows = 0;
		if (fastComp) {
			for (int u : this.trainRatings.keySet())
				for (int i : this.trainRatings.get(u).keySet()) {
					if (!implicit) {
						nRows++;
					} else {
						nRows += nRows; // for each positive ex there is a
										// negative one also or more. take this
										// into account

					}
				}

		} else {
			BufferedReader br;
			try {
				br = new BufferedReader(new FileReader(trainFile));
				ArrayList<FeatureNode[]> ff = new ArrayList<FeatureNode[]>();

				String line = null;
				while ((line = br.readLine()) != null)
					nRows++;

				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return nRows;
	}

	private Problem createProblem() {

		logger.info("Creating problem");

		Problem problem = new Problem();
		problem.l = XTrain.length;// - 1; // number of training examples
		problem.n = num_features + 1; // number of features
		problem.x = XTrain; // feature nodes
		problem.y = yTrain; // target values

		logger.info("Number of training examples: " + problem.l);
		logger.info("Number of features: " + problem.n);

		return problem;
	}

	public void train() {

		try {

			int k_modeSel = 5;
			Recommender pred;
			List<Integer> topK = Arrays.asList(1, 3, 5, 10, 15, 20);

			Evaluator validEval = new Evaluator(validRatingFile, topK, thresh,
					relUnknownItems);

			double bestPerf = 0, bestC = 0, bestEps = 0;
			Model bestModel = null;
			int bestModelType = 0;

			Map<Integer, Float> valMapPrec = new HashMap<Integer, Float>();
			Map<Integer, Float> valMapRec = new HashMap<Integer, Float>();
			Map<Integer, Float> valMapNDCG = new HashMap<Integer, Float>();

			if (silent)
				Linear.setDebugOutput(null);
			Model model;
			loadTrainDataset(this.trainingFile);
			Problem problem = createProblem();

			for (int solverType : listSolverType) {
				if (solverType != 11 & solverType != 12 & solverType != 13) {
					listP = new ArrayList<Double>();
					listP.add(0.1);
				}

				for (double c : listC) {
					for (double eps : listEps) {
						for (double p : listP) {
							SolverType solver = SolverType.getById(solverType);
							Parameter parameter = new Parameter(solver, c, eps,
									p);

							model = Linear.train(problem, parameter);

							pred = new Recommender(model, topN);
							Map<Integer, List<Integer>> validRecc = pred
									.computeRecc(this.validationFile,
											validationUsers, implicit);

							validEval.eval(validRecc, valMapNDCG, valMapPrec,
									valMapRec);

							float perf = valMapPrec.get(k_modeSel);
							// float perf = valMapRec.get(k_modeSel);
							// float perf = valMapNDCG.get(k_modeSel);
							if (perf >= bestPerf) {

								bestPerf = perf;
								bestModel = model;

								bestModelType = parameter.getSolverType()
										.getId();
								bestC = parameter.getC();
								bestEps = parameter.getEps();
							}

						}
					}
				}
			}

			logger.info("-----------------------------------------------------------------");
			logger.info("---------BEST MODEL " + bestModelType + ". C: "
					+ bestC + ". Eps: " + bestEps + " . bestPerf_" + k_modeSel
					+ ": " + bestPerf);
			modelFileName = workingDir + "bestModel";
			File modelFile = new File(modelFileName);
			bestModel.save(modelFile);

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
				if (!ratings.containsKey(u))
					ratings.put(u, new HashMap());
				ratings.get(u).put(i, rel);

				line = reader.readLine();
			}

		} catch (IOException e) {
		}
		return ratings;
	}

}