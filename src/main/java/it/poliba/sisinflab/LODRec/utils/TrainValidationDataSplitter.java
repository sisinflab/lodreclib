package it.poliba.sisinflab.LODRec.utils;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TFloatHashSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ciir.umass.edu.utilities.MyThreadPool;

public class TrainValidationDataSplitter {
	private Set<Integer> items;

	private Map<Integer, Map<Integer, Float>> testRatings;
	private Map<Integer, Map<Integer, Float>> trainRatings;
	private Map<Integer, Map<Integer, Float>> validRatings;

	public void buildTrainValTestRatings(String originalFile, String trainFile,
			String validFile, String testFile, float valRatio, float testRatio,
			int minTrainRatings, int minValRatings, boolean implicit,
			int max_users) {

		TIntObjectHashMap<TIntFloatHashMap> original_train = TextFileUtils
				.loadInputUsersRatings(originalFile);

		TIntObjectHashMap<TIntFloatHashMap> train = new TIntObjectHashMap<TIntFloatHashMap>();
		int[] users = original_train.keys();
		for (int i = 0; i < max_users & i < original_train.keys().length; i++)
			train.put(users[i], original_train.get(users[i]));

		TIntObjectHashMap<TIntFloatHashMap> valid = new TIntObjectHashMap<TIntFloatHashMap>();
		TIntObjectHashMap<TIntFloatHashMap> test = new TIntObjectHashMap<TIntFloatHashMap>();

		// this.splitRatings(train, test, testRatio, minTrainRatings);
		this.splitRatings(train, valid, valRatio, minValRatings);

		try {
			BufferedWriter train_file = new BufferedWriter(new FileWriter(
					trainFile));
			BufferedWriter validation_file = new BufferedWriter(new FileWriter(
					validFile));
			BufferedWriter test_file = new BufferedWriter(new FileWriter(
					testFile));

			for (int u : train.keys()) {
				for (int i : train.get(u).keys()) {
					if (!implicit)
						train_file.append(u + "\t" + i + "\t"
								+ train.get(u).get(i) + "\n");

					else
						train_file.append(u + "\t" + i + "\t" + 1 + "\n");

				}
			}
			for (int u : valid.keys()) {
				for (int i : valid.get(u).keys()) {
					if (!implicit)
						validation_file.append(u + "\t" + i + "\t"
								+ valid.get(u).get(i) + "\n");
					else
						validation_file.append(u + "\t" + i + "\t" + 1 + "\n");

				}
			}

			for (int u : test.keys()) {

				for (int i : test.get(u).keys()) {
					if (!implicit)
						test_file.append(u + "\t" + i + "\t"
								+ test.get(u).get(i) + "\n");
					else
						test_file.append(u + "\t" + i + "\t" + 1 + "\n");

				}
			}

			train_file.flush();
			validation_file.flush();
			test_file.flush();
			train_file.close();
			validation_file.close();
			test_file.close();

			System.out.println("n.train users: " + train.keys().length
					+ "   n.validation users: " + valid.keys().length
					+ "   n.test users: " + test.keys().length);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	private void splitRatings(TIntObjectHashMap<TIntFloatHashMap> map1,
			TIntObjectHashMap<TIntFloatHashMap> map2, float ratio,
			int minRatings) {

		TIntSet users = map1.keySet();
		TIntIterator it = users.iterator();
		int u, i;
		TIntArrayList candidate;
		Set<Integer> usersToRemove = new HashSet();
		int splits = 0;
		while (it.hasNext()) {
			u = it.next();

			TIntSet tmp = map1.get(u).keySet();
			candidate = new TIntArrayList();
			candidate.addAll(tmp);

			int n = (int) (ratio * candidate.size());
			if (n > minRatings) {

				map2.put(u, new TIntFloatHashMap());

				splits++;
				TIntSet validItems = chooseRndItems(candidate, n);

				TIntIterator itt = validItems.iterator();

				// System.out.println(this.trainRatings.get(u).size() +" - "+
				// this.validationRatings.get(u).size());

				while (itt.hasNext()) {
					i = itt.next();
					map2.get(u).put(i, map1.get(u).get(i));
					map1.get(u).remove(i);
				}
			} else
				usersToRemove.add(u);

		}
		for (int us : usersToRemove)
			map1.remove(us);

		System.out.println("n.users map1 " + map1.keys().length
				+ " n.users map2 " + map2.keys().length + "  n.splits "
				+ splits);

	}

	private TIntSet chooseRndItems(TIntArrayList list, int N) {

		TIntSet keys = new TIntHashSet();
		TIntSet ret = new TIntHashSet();
		Random r = new Random();
		int cont = 0;

		if (list.size() < N)
			return ret;

		while (cont < N) {
			int rr = r.nextInt(list.size());
			while (keys.contains(rr))
				rr = r.nextInt(list.size());
			keys.add(rr);
			cont++;
		}

		TIntIterator it = keys.iterator();
		while (it.hasNext())
			ret.add(list.get(it.next()));

		return ret;
	}

	public void analyze(String train, String valid, String test, String metadata) {

		loadItemIDsFromItemTextualFile(metadata);
		this.readTrainData(train);
		this.readValidData(valid);
		this.readTestData(test);
		Set<Integer> users = trainRatings.keySet();
		int min = 1000;
		int max = 0;

		int count = 0;
		float avg=0;
		for (int u : users) {
			count = 0;
			count += trainRatings.get(u).size();
			if (testRatings.containsKey(u)) {
				count += testRatings.get(u).size();
			}
			if (validRatings.containsKey(u)) {
				count += validRatings.get(u).size();
			}

			
			if (count < min)
				min = count;
			if (count > max)
				max = count;
			avg+=count;
		}
		avg/=(float)users.size();
		System.out.println("n. train users " + users.size()  + " min: " + min
				+ " max: " + max+" avg: "+avg);

	}

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
		System.out.println("size "+ trainRatings.size());
	}

	public void readValidData(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			String line = reader.readLine();
			validRatings = new HashMap<Integer, Map<Integer, Float>>();
			while (line != null) {

				String[] str = line.split("\t");

				int u = Integer.parseInt(str[0].trim());
				int i = Integer.parseInt(str[1].trim());
				if (items.contains(i)) {

					float rel = Float.parseFloat(str[2].trim());
					if (!validRatings.containsKey(u))
						validRatings.put(u, new HashMap());
					validRatings.get(u).put(i, rel);
				}
				line = reader.readLine();
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
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
		System.out.println("Loaded "+items.size());

	}

	public static void main(String[] args) {

		TrainValidationDataSplitter splitter = new TrainValidationDataSplitter();
		

		
		System.out.println("Movielens");
		splitter.analyze("ML/ratings/TrainSetML_65_15_20_reduced_min50", "ML/ratings/ValidSetML_65_15_reduced_min50", "ML/ratings/TestSetML_80_20_reduced_min50", "ML/itemMetadata");
		
		System.out.println("Library");
		splitter.analyze("Lib/rating/TrainSetLibrary_65_15_20_reduced", "Lib/rating/ValidSetLibrary_15_20_reduced", "Lib/rating/TestSetLibrary_80_20_reduced", "Lib/itemMetadata");
		
		System.out.println("LastFM");
		splitter.analyze("LF/feedback/TrainSetLF_65_15_20_percentile_rank_norm_reduced_v2_noNegExam", "LF/feedback/ValidSetLF_15_20_percentile_rank_norm_reduced_v2_noNegExam", "LF/feedback/TestSetLF_80_20_percentile_rank_norm_reduced_v2", "LF/itemMetadata");
		
		// boolean implicit=false;
		// int n_users=1000000;
		// splitter.buildTrainValTestRatings("TrainSetLF_80_20_percentile_rank_norm_reduced_v2_noNegExam",
		// "TrainSetLF_65_15_20_percentile_rank_norm_reduced_v2_noNegExam",
		// "ValidSetLF_15_20_percentile_rank_norm_reduced_v2_noNegExam",
		// "XXX", 0.2f, 0f, 0, 3 ,implicit, n_users);

	}

}
