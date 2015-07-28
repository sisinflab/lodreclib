package it.poliba.sisinflab.LODRec.evaluation;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Evaluator {

	private List<Integer> topK;
	private Map<Integer, Map<Integer, Float>> testRatings;
	private Map<Integer, Map<Integer, Float>> trainRatings;
	private Map<Integer, Float> mapItemPopularity;
	private TIntObjectHashMap<TIntFloatHashMap> itemSim;
	private Map<Integer, List<Integer>> recommendations;
	private String reccDir;
	private Set<Integer> items;
	private BufferedWriter out;
	private String evalFile;
	private float thresh, BADThresh, relUnknownItems;
	private String workingDir;
	private int topN;
	private String itemMetadataFile;
	private String itemMetadataEvalFile;

	private String trainRatingFile;
	private String testRatingFile;
	// private String reccFile;
	// private boolean cmpBinsBasedEvaluation = false;
	// private List<Integer> bins = Arrays.asList(0, 15, 30, 45);
	private DecimalFormat df = new DecimalFormat();
	private boolean process_triple_list = false;
	// private static final String PROP = "#Psubject";
	// private boolean computeUserNov = false;
	// private boolean computeILD = true;
	private static final float MIN_SIM = 0.0001f;
	private Set<Integer> users;
	// private TIntObjectHashMap<THashSet<String>> itemCatMap;
	private TIntObjectHashMap<TIntFloatHashMap> map_item_intFeatures;
	private boolean WRITE_HEADER = false;

	public Evaluator(String testRatingFile, List<Integer> topK, float thresh,
			float relUnknownItems) {
		this.thresh = thresh;
		this.relUnknownItems = relUnknownItems;
		this.topK = topK;
		this.readTestData(testRatingFile);

	}

	public Evaluator(String workingDir, String itemMetadataFile,
			String itemMetadataEvalFile, String trainRatingFile,
			String testRatingFile, float ratingThreshold, float BADThresh,
			float relUnknownItems, int topN) {

		System.out.println("starting evaluation ");
		this.workingDir = workingDir;
		this.itemMetadataFile = itemMetadataFile;
		this.itemMetadataEvalFile = itemMetadataEvalFile;
		this.trainRatingFile = trainRatingFile;
		this.testRatingFile = testRatingFile;
		this.thresh = ratingThreshold;
		this.BADThresh = BADThresh;
		this.relUnknownItems = relUnknownItems;
		this.topN = topN;
		init();

	}

	private void init() {

		this.topK = Arrays.asList(1, 5, 10, 25, 50, 100, 250);

		this.evalFile = this.workingDir + "evaluation.txt";

		this.reccDir = this.workingDir + "out/";

		this.itemSim = new TIntObjectHashMap<TIntFloatHashMap>();

		map_item_intFeatures = new TIntObjectHashMap<TIntFloatHashMap>();

		loadItemIDsFromItemTextualFile(itemMetadataFile);

		if (itemMetadataEvalFile == null)
			itemMetadataEvalFile = itemMetadataFile;

		loadItemFeatureData(itemMetadataEvalFile);
		computeItemSim();

		System.out.println(items.size() + " items loaded");
		readTrainData(trainRatingFile);

		readTestData(testRatingFile);

		cmpItemPopularity();
		System.out.println(testRatingFile + " - " + testRatings.size());

		df.setMaximumFractionDigits(5);

	}

	private void computeItemSim() {

		List<Integer> sortedItems = new ArrayList<Integer>();
		sortedItems.addAll(items);
		Collections.sort(sortedItems);
		int id1, id2;
		for (int i = 0; i < sortedItems.size() - 1; i++) {
			id1 = sortedItems.get(i);
			this.itemSim.put(id1, new TIntFloatHashMap());
			for (int j = i + 1; j < sortedItems.size(); j++) {
				id2 = sortedItems.get(j);
				float val = 0;
				if (map_item_intFeatures.containsKey(id1)
						& map_item_intFeatures.containsKey(id2))
					val = cmpJaccardSim(this.map_item_intFeatures.get(id1)
							.keySet(), this.map_item_intFeatures.get(id2)
							.keySet());

				if (val > MIN_SIM) {
					itemSim.get(id1).put(id2, val);
				}
			}

		}
	}

	private float cmpJaccardSim(TIntSet tIntSet, TIntSet tIntSet2) {
		TIntSet inters = new TIntHashSet();
		inters.addAll(tIntSet);
		inters.retainAll(tIntSet2);

		if (inters.size() == 0)
			return 0;
		else
			return (inters.size() / (float) (tIntSet.size() + tIntSet2.size() - inters
					.size()));

	}

	private float cmpCosineSim(TIntFloatHashMap v1, TIntFloatHashMap v2) {

		TIntHashSet inters = new TIntHashSet();
		inters.addAll(v1.keySet());
		inters.retainAll(v2.keySet());

		if (inters.size() == 0)
			return 0;
		else {
			int i = 0;
			TIntIterator it = inters.iterator();
			float num = 0;
			float norm_v1 = 0;
			float norm_v2 = 0;
			while (it.hasNext()) {
				i = it.next();
				num += v1.get(i) * v2.get(i);
			}
			for (int k1 : v1.keys())
				norm_v1 += (v1.get(k1) * v1.get(k1));
			for (int k2 : v2.keys())
				norm_v2 += (v2.get(k2) * v2.get(k2));
			return num / (float) (Math.sqrt(norm_v1) * Math.sqrt(norm_v2));

		}
		// return (inters.size() / (float) (set1.size() + set2.size() - inters
		// .size()));

	}

	private void loadItemFeatureData(String file_name) {

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file_name));
			String line = null;
			int count = 0;
			while ((line = br.readLine()) != null) {
				try {
					String[] vals = line.split("\t");
					int id = Integer.parseInt(vals[0]);

					if (items.contains(id)) {

						map_item_intFeatures.put(id, new TIntFloatHashMap());

						String[] values = vals[1].trim().split(" ");
						for (int i = 0; i < values.length; i++) {
							String[] pair = values[i].split(":");
							int fId = Integer.parseInt(pair[0]);
							float fVal = Float.parseFloat(pair[1]);
							map_item_intFeatures.get(id).put(fId, fVal);
						}
						count++;
					}
				} catch (Exception ex) {
					// System.out.println(ex.getMessage());
					// System.out.println(line);
				}
			}
			System.out.println("item metadata loaded for evaluation - " + count
					+ " items");
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void loadItemIDsFromItemTextualFile(String itemsFile) {

		BufferedReader reader;
		try {
			items = new HashSet<Integer>();
			reader = new BufferedReader(new FileReader(itemsFile));
			String line = "";
			int item_id;
			while ((line = reader.readLine()) != null) {

				String[] parts = line.split("\t");
				item_id = Integer.parseInt(parts[0]);
				this.items.add(item_id);

			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void eval(Map<Integer, List<Integer>> recommendations,
			Map<Integer, Float> mapNDCG, Map<Integer, Float> mapPrecision,
			Map<Integer, Float> mapRecall) {

		this.recommendations = recommendations;
		float mrr = 0;
		int count = 0;

		for (int k : topK) {
			mapPrecision.put(k, 0f);
			mapRecall.put(k, 0f);
			mapNDCG.put(k, 0f);
		}

		for (int uid : recommendations.keySet()) {

			Set<Integer> relevItems = new HashSet<Integer>();
			List<Integer> sortedRecc = null;

			if (testRatings.containsKey(uid)) {
				for (int i : testRatings.get(uid).keySet()) {
					if (testRatings.get(uid).get(i) >= thresh)
						relevItems.add(i);
				}

				if (relevItems.size() > 0) {

					sortedRecc = recommendations.get(uid);

					boolean found = false;
					float rr = 0;
					for (int i = 0; i < sortedRecc.size() & !found; i++) {
						if (relevItems.contains(sortedRecc.get(i))) {
							rr = 1 / (float) (i + 1);
							found = true;
						}
					}
					mrr += rr;

					List<Float> idealRanking = new ArrayList<Float>();

					for (int i : items) {

						if (testRatings.get(uid).containsKey(i))

							idealRanking.add(testRatings.get(uid).get(i));
						else
							// we do not know the real relevance for this
							// item and assign it default value
							idealRanking.add(relUnknownItems);

					}
					Collections.sort(idealRanking, Collections.reverseOrder());

					for (int k : topK) {

						// ----computation of precision, recall and nov

						int hits = 0;
						float nov = 0, pop = 0;
						for (int i = 0; i < k && i < sortedRecc.size(); i++) {
							if (relevItems.contains(sortedRecc.get(i))) {
								hits++;
							}
						}
						float prec = hits / (float) k;
						float rec = hits / (float) relevItems.size();
						float rel = 0, dcg = 0, idcg = 0;

						int p = 0;

						for (int j = 0; j < k && j < idealRanking.size(); j++) {
							p = j + 1;
							idcg += ((Math.pow(2, idealRanking.get(j)) - 1) / (Math
									.log(p + 1) / Math.log(2)));
						}

						p = 0;
						int item_id;
						for (int i = 0; i < k && i < sortedRecc.size(); i++) {
							rel = 0;
							item_id = sortedRecc.get(i);
							p = (i + 1);
							if (testRatings.get(uid).containsKey(item_id))
								rel = testRatings.get(uid).get(item_id);
							else
								rel = relUnknownItems;

							dcg += ((Math.pow(2, rel) - 1) / (Math.log(p + 1) / Math
									.log(2)));

						}

						float ndcg = dcg / idcg;

						mapPrecision.put(k, mapPrecision.get(k) + prec);
						mapRecall.put(k, mapRecall.get(k) + rec);
						mapNDCG.put(k, mapNDCG.get(k) + ndcg);

					}

					count++;
				}

			}
		}

		mrr = mrr / (float) count;

		for (int k : topK) {
			float prec = mapPrecision.get(k) / count;
			float rec = mapRecall.get(k) / count;
			float ndcg = mapNDCG.get(k) / count;
			mapPrecision.put(k, prec);
			mapRecall.put(k, rec);
			mapNDCG.put(k, ndcg);
		}

	}

	private TIntFloatHashMap build_user_content_profile(int uid) {
		TIntFloatHashMap user_prof = new TIntFloatHashMap();
		int c = 0;
		for (int i : trainRatings.get(uid).keySet()) {
			if (trainRatings.get(uid).get(i) >= thresh) {
				c++;
				if (map_item_intFeatures.containsKey(i)) {
					for (int j : this.map_item_intFeatures.get(i).keys()) {
						float val = this.map_item_intFeatures.get(i).get(j);
						user_prof.adjustOrPutValue(j, val, val);
					}
				}
			}
		}
		for (int i : user_prof.keys())
			user_prof.adjustValue(i, 1 / (float) (c));

		return user_prof;
	}

	public void eval(String fileReccData, int min, int max) {
		try {
			users = new HashSet<Integer>();
			out = new BufferedWriter(new FileWriter(evalFile, true));
			BufferedWriter user_scores = new BufferedWriter(new FileWriter(
					fileReccData + "_user_scores", false));
			float mrr = 0;

			String header = "";
			Map<Integer, Set<Integer>> mapRecommendedItemsInCatalog = new HashMap();
			Map<Integer, Float> mapPrecision = new HashMap<Integer, Float>();
			Map<Integer, Float> mapRecall = new HashMap<Integer, Float>();
			Map<Integer, Float> mapBADPrecision = new HashMap<Integer, Float>();
			Map<Integer, Float> mapBADRecall = new HashMap<Integer, Float>();
			Map<Integer, Float> mapNDCG = new HashMap<Integer, Float>();
			Map<Integer, Float> mapEBN = new HashMap<Integer, Float>();
			Map<Integer, Float> mapILD = new HashMap<Integer, Float>();
			int count = 0;

			Set<Integer> usersWithBADItemsInTestSet = new HashSet();

			for (int k : topK) {
				mapPrecision.put(k, 0f);
				mapRecall.put(k, 0f);
				mapBADPrecision.put(k, 0f);
				mapBADRecall.put(k, 0f);
				mapNDCG.put(k, 0f);
				mapEBN.put(k, 0f);
				mapILD.put(k, 0f);
				mapRecommendedItemsInCatalog.put(k, new HashSet());

			}
			for (int uid : recommendations.keySet()) {

				int n_train_ratings = 0;
				if (trainRatings.containsKey(uid))
					n_train_ratings = trainRatings.get(uid).size();

				if ((min == -1 & max == -1)
						|| (n_train_ratings >= min & n_train_ratings < max)) {

					Set<Integer> relevItems = new HashSet<Integer>();
					List<Integer> sortedRecc = null;
					Set<Integer> BADItems = new HashSet<Integer>();

					// & trainRatings.containsKey(uid) --> can be removed
					// actually
					if (testRatings.containsKey(uid)
							& trainRatings.containsKey(uid)) {
						for (int i : testRatings.get(uid).keySet()) {
							if (testRatings.get(uid).get(i) >= thresh)
								relevItems.add(i);
							if (testRatings.get(uid).get(i) <= BADThresh)
								BADItems.add(i);
						}

						if (relevItems.size() > 0) {

							users.add(uid);
							TIntFloatHashMap user_prof = build_user_content_profile(uid);

							sortedRecc = recommendations.get(uid);
							boolean found = false;
							float rr = 0;
							for (int i = 0; i < sortedRecc.size() & !found; i++) {
								if (relevItems.contains(sortedRecc.get(i))) {
									rr = 1 / (float) (i + 1);
									found = true;
								}
							}
							mrr += rr;
							user_scores.append("" + uid);
							List<Float> idealRanking = new ArrayList<Float>();

							for (int i : items) {
								if (testRatings.get(uid).containsKey(i))
									idealRanking.add(testRatings.get(uid)
											.get(i));
								else
									// we do not know the real relevance for
									// this
									// item and assign it default value
									idealRanking.add(relUnknownItems);
							}
							Collections.sort(idealRanking,
									Collections.reverseOrder());

							for (int k : topK) {

								// ----computation of precision, recall
								int hits = 0;
								float nov = 0, pop = 0, ild = 0;
								Set<Integer> topNitems = new HashSet();
								int id1, id2, id, idd;
								float sim = 0;
								int ild_count = 0;

								for (int i = 0; i < k && i < sortedRecc.size(); i++) {

									if (relevItems.contains(sortedRecc.get(i))) {
										hits++;
									}

									if (mapItemPopularity
											.containsKey(sortedRecc.get(i))) {
										pop = mapItemPopularity.get(sortedRecc
												.get(i));
										nov += (-1) * pop
												* (Math.log(pop) / Math.log(2));
									}

									id = sortedRecc.get(i);

									for (int j = i + 1; j < k
											&& j < sortedRecc.size(); j++) {
										idd = sortedRecc.get(j);

										if (idd > id) {
											id1 = id;
											id2 = idd;
										} else {
											id2 = id;
											id1 = idd;
										}
										sim = 0;
										if (itemSim.contains(id1)) {
											if (itemSim.get(id1).contains(id2))
												sim = itemSim.get(id1).get(id2);
										}

										ild += (1 - sim);
										ild_count++;
									}

									topNitems.add(sortedRecc.get(i));
								}

								mapRecommendedItemsInCatalog.get(k).addAll(
										topNitems);

								ild /= (float) ild_count;
								float prec = hits / (float) k;
								float rec = hits / (float) relevItems.size();

								float BADPrec = 0;
								float BADRec = 0;
								if (BADItems.size() > 0) {

									if (!usersWithBADItemsInTestSet
											.contains(uid))
										usersWithBADItemsInTestSet.add(uid);

									hits = 0;
									for (int i = 0; i < k
											&& i < sortedRecc.size(); i++) {
										if (BADItems
												.contains(sortedRecc.get(i))) {
											hits++;
										}
									}
									BADPrec = hits / (float) k;
									BADRec = hits / (float) BADItems.size();
								}

								float rel = 0, dcg = 0, idcg = 0;

								int p = 0;

								for (int j = 0; j < k
										&& j < idealRanking.size(); j++) {
									p = j + 1;
									idcg += ((Math.pow(2, idealRanking.get(j)) - 1) / (Math
											.log(p + 1) / Math.log(2)));
								}

								p = 0;
								int item_id;
								for (int i = 0; i < k && i < sortedRecc.size(); i++) {
									rel = 0;
									item_id = sortedRecc.get(i);
									p = (i + 1);
									if (testRatings.get(uid).containsKey(
											item_id))
										rel = testRatings.get(uid).get(item_id);
									else
										rel = relUnknownItems;

									dcg += ((Math.pow(2, rel) - 1) / (Math
											.log(p + 1) / Math.log(2)));

								}

								float ndcg = dcg / idcg;

								mapPrecision.put(k, mapPrecision.get(k) + prec);
								mapRecall.put(k, mapRecall.get(k) + rec);
								mapBADPrecision.put(k, mapBADPrecision.get(k)
										+ BADPrec);
								mapBADRecall.put(k, mapBADRecall.get(k)
										+ BADRec);
								mapNDCG.put(k, mapNDCG.get(k) + ndcg);
								mapEBN.put(k, mapEBN.get(k) + nov);
								mapILD.put(k, mapILD.get(k) + ild);
								user_scores.append(" " + k + " " + prec + " "
										+ rec + " " + ndcg + " " + nov + " "
										+ ild + " " + BADPrec + " " + BADRec);

							}
							user_scores.append("\n");
							// -----------------------------------------

							count++;

						}

					}
				}
			}

			Date d = new Date();
			mrr = mrr / (float) count;
			// String strBin = "_allUsers";
			// // if (max > count)
			// // max = count;
			//
			// if (min != -1 || max != -1)
			// strBin = "users_Min" + min + "_Max" + max;

			String sep = "\t";
			header = "date" + sep + "name" + sep + "thresh" + sep + "badThresh"
					+ sep + "relevUnknownValues" + sep + "n.users" + sep
					+ "n.items" + sep + "MRR" + sep;

			String outline = d.toGMTString() + sep + fileReccData + sep
					+ this.thresh + sep + this.BADThresh + sep
					+ this.relUnknownItems + sep + count + sep + items.size()
					+ sep + formatVal(mrr) + sep;

			for (int k : topK) {
				float prec = mapPrecision.get(k) / count;
				float rec = mapRecall.get(k) / count;
				float ndcg = mapNDCG.get(k) / count;
				float nov = mapEBN.get(k) / count;
				float uNov = mapILD.get(k) / count;

				float BADP = mapBADPrecision.get(k)
						/ usersWithBADItemsInTestSet.size();
				float BADR = mapBADRecall.get(k)
						/ usersWithBADItemsInTestSet.size();

				float item_cat_cov = mapRecommendedItemsInCatalog.get(k).size()
						/ (float) items.size();

				header += "P@" + k + sep + "R@" + k + sep + "nDCG@" + k + sep
						+ "EBN@" + k + sep + "ILD@" + k + sep + "ItemCov@" + k
						+ sep + "BAD_P@" + k + sep + "BAD_R@" + k + sep;

				outline += formatVal(prec) + sep + formatVal(rec) + sep
						+ formatVal(ndcg) + sep + formatVal(nov) + sep
						+ formatVal(uNov) + sep + formatVal(item_cat_cov) + sep
						+ formatVal(BADP) + sep + formatVal(BADR) + sep;

			}

			if (WRITE_HEADER)
				out.append(header + "\n");
			out.append(outline + "\n");

			System.out.println(header + "\n" + outline);
			// System.out.println("num users:" + count + ". num items:"
			// + items.size());
			out.flush();
			out.close();

			user_scores.flush();
			user_scores.close();

			cmpDatasetStats();

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	private void cmpDatasetStats() {
		int c = 0;
		for (int u : trainRatings.keySet()) {
			if (users.contains(u))
				c += trainRatings.get(u).size();
		}
		System.out.println("train set - ratings:" + c);

		c = 0;

		for (int u : testRatings.keySet()) {
			if (users.contains(u))
				c += testRatings.get(u).size();
		}
		System.out
				.println("test set - users:" + users.size() + " ratings:" + c);

	}

	private String formatVal(float val) {

		if (Float.isNaN(val))
			val = 0;
		return df.format(val).replace(".", ",");
	}

	public void loadRecommendations(String fileReccData) {
		try {
			int k = topK.get(topK.size() - 1);
			try {
				recommendations = new HashMap<Integer, List<Integer>>();
				BufferedReader reader = new BufferedReader(new FileReader(
						fileReccData));

				String line = reader.readLine();
				process_triple_list = false;
				if (line != null) {
					if (line.split("\t").length == 3)
						process_triple_list = true;
				}

				reader = new BufferedReader(new FileReader(fileReccData));

				Map<Integer, Map<Float, Set<Integer>>> tmp = new HashMap();
				while (line != null) {

					if (!process_triple_list)
						processeReccList(line);
					else
						processeTripleList(line, tmp);
					line = reader.readLine();
				}

				if (tmp.size() > 0) {
					for (int u : tmp.keySet()) {
						List<Float> scores = new ArrayList<Float>();
						scores.addAll(tmp.get(u).keySet());
						Collections.sort(scores, Collections.reverseOrder());
						List<Integer> sortedRecc = new ArrayList();
						int c = 0;
						float score;
						for (int j = 0; j < scores.size() & c < k; j++) {
							score = scores.get(j);
							Set<Integer> items = tmp.get(u).get(score);
							for (int i : items) {
								sortedRecc.add(i);
								c++;
							}
						}
						recommendations.put(u, sortedRecc);
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			recommendations = new HashMap<Integer, List<Integer>>();

		}
	}

	private void processeTripleList(String line,
			Map<Integer, Map<Float, Set<Integer>>> tmp) {

		// user item score triples
		String[] str;
		if (line.contains(" "))
			str = line.split(" ");
		else
			str = line.split("\t");
		int u = Integer.parseInt(str[0].trim());
		int i = Integer.parseInt(str[1].trim());
		float score = Float.parseFloat(str[2].trim());

		if (!tmp.containsKey(u))
			tmp.put(u, new HashMap());
		if (!tmp.get(u).containsKey(score))
			tmp.get(u).put(score, new HashSet());
		tmp.get(u).get(score).add(i);

	}

	private void processeReccList(String line) {
		line = line.replace("[", "").replace("]", "");

		String[] str = line.split("\t");
		int uid = Integer.parseInt(str[0].trim());

		if (str.length > 1) {

			List<Integer> sortedRecc = new ArrayList();

			Set<Integer> trainItems = new HashSet();
			if (trainRatings.containsKey(uid))
				trainItems = this.trainRatings.get(uid).keySet();

			String pairsSep = " ";
			if (!str[1].contains(pairsSep))
				pairsSep = ",";

			String[] pairs = str[1].split(pairsSep);
			int c = 0;
			for (String pair : pairs) {
				c++;
				if (c > topN)
					break;

				String id = pair.substring(0, pair.indexOf(":"));
				int iditem = Integer.parseInt(id.trim());

				if (!trainItems.contains(iditem))
					sortedRecc.add(iditem);

			}
			recommendations.put(uid, sortedRecc);

		}
	}

	// public void loadItemIndex(String filename) {
	// try {
	// BufferedReader reader = new BufferedReader(new FileReader(filename));
	//
	// String line = reader.readLine();
	// items = new HashSet<Integer>();
	//
	// while (line != null) {
	// String[] vals = line.split("\t");
	// if (vals.length > 1) {
	// int id = Integer.parseInt(vals[0]);
	// if (!items.contains(id))
	// items.add(id);
	// }
	// line = reader.readLine();
	// }
	//
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }

	// public Map<Integer, Set<String>> loadItemMetadata(String filename) {
	// Map<Integer, Set<String>> map = new HashMap();
	// try {
	//
	// BufferedReader reader = new BufferedReader(new FileReader(filename));
	//
	// String line = reader.readLine();
	// items = new HashSet<Integer>();
	// // id\tval1 val2 valk
	// while (line != null) {
	// String[] str = line.split("\t");
	// int id = Integer.parseInt(str[0].trim());
	// map.put(id, new HashSet());
	// String[] vals = str[1].trim().split(" ");
	// for (int i = 0; i < vals.length; i++)
	// map.get(id).add(vals[i]);
	//
	// line = reader.readLine();
	// }
	//
	// } catch (IOException e) {
	// }
	// return map;
	// }

	public void readTestData(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line = reader.readLine();
			testRatings = new HashMap<Integer, Map<Integer, Float>>();
			boolean add = false;
			Set<Integer> tmp = new HashSet();
			while (line != null) {

				try {
					String[] str = line.split("\t");

					int u = Integer.parseInt(str[0].trim());
					int i = Integer.parseInt(str[1].trim());
					if (items != null) {
						if (items.contains(i)) {
							add = true;
						} else
							add = false;
					} else {
						add = true;
						tmp.add(i);
					}
					if (add) {
						float rel = Float.parseFloat(str[2].trim());
						if (!testRatings.containsKey(u))
							testRatings.put(u, new HashMap());
						testRatings.get(u).put(i, rel);
					}

				} catch (Exception ex) {
					System.out.println(ex.getMessage());

				}

				line = reader.readLine();
			}
			if (items == null) {
				items = new HashSet<Integer>();
				items.addAll(tmp);
			}

		} catch (IOException e) {
		}
	}

	public void readTrainData(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line = reader.readLine();
			trainRatings = new HashMap<Integer, Map<Integer, Float>>();
			while (line != null) {

				String[] str = line.split("\t");

				int u = Integer.parseInt(str[0].trim());
				int i = Integer.parseInt(str[1].trim());
				if (items.contains(i)) {

					float rel = Float.parseFloat(str[2].trim());
					if (!trainRatings.containsKey(u))
						trainRatings.put(u, new HashMap());
					trainRatings.get(u).put(i, rel);
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public void cmpItemPopularity() {

		float rel;
		mapItemPopularity = new HashMap<Integer, Float>();
		for (int u : trainRatings.keySet()) {
			for (int i : trainRatings.get(u).keySet()) {
				rel = trainRatings.get(u).get(i);
				if (rel >= this.thresh) {
					if (!mapItemPopularity.containsKey(i))
						mapItemPopularity.put(i, 1f);
					else
						mapItemPopularity.put(i, mapItemPopularity.get(i) + 1);
				}
			}
		}

		// Map<Integer, Set<Integer>> tmp = new HashMap<Integer,
		// Set<Integer>>();

		for (int i : mapItemPopularity.keySet()) {

			// System.out.println("item " + i + ". n.ratings "
			// + mapItemPopularity.get(i) + ". norm pop "
			// + mapItemPopularity.get(i)
			// / (float) (trainRatings.keySet().size()));

			// int n = (mapItemPopularity.get(i)).intValue();
			//
			// if (!tmp.containsKey(n))
			// tmp.put(n, new HashSet());
			// tmp.get(n).add(i);
			float f = mapItemPopularity.get(i)
					/ (float) (trainRatings.keySet().size());
			mapItemPopularity.put(i, mapItemPopularity.get(i)
					/ (float) (trainRatings.keySet().size()));

		}

		// List<Integer> vals=new ArrayList<Integer>();
		// vals.addAll(tmp.keySet());
		// Collections.sort(vals,Collections.reverseOrder());
		// for(int v:vals){
		// System.out.print(v+": ");
		// for(int ii:tmp.get(v))
		// System.out.print(ii+", ");
		//
		// System.out.println();
		// }

	}

	public void evalDir(String dir) {

		this.reccDir = this.workingDir + dir;
		System.out.println("starting evaluation for " + this.reccDir);
		File directory = new File(this.reccDir);
		int inf = -1, sup = -1;
		File[] files = directory.listFiles();
		for (int i = 0; i < files.length; i++) {

			String filename;
			filename = files[i].getName();

			if (!filename.contains("user_scores")) {

				this.loadRecommendations(this.reccDir + filename);
				eval(this.reccDir + filename, -1, -1);

				// eval(this.reccDir + filename, -1,25 );
				// eval(this.reccDir + filename, -1, 50);
				// eval(this.reccDir + filename, -1, 100);
				//
				// if (cmpBinsBasedEvaluation) {
				// for (int j = 0; j < bins.size() - 1; j++) {
				// inf = bins.get(j);
				// sup = bins.get(j + 1);
				// eval(this.reccDir + filename, inf, sup);
				// }
				// inf = bins.get(bins.size() - 1);
				// sup = Integer.MAX_VALUE;
				// eval(this.reccDir + filename, inf, sup);
				//
				// }
			}

		}
	}

	public void eval(String reccFile, String evalFile) {

		this.evalFile = "./" + evalFile;
		this.reccDir = this.workingDir;
		this.loadRecommendations(reccFile);
		System.out.println("loaded recc from " + reccFile);
		eval(reccFile, -1, -1);

	}

}