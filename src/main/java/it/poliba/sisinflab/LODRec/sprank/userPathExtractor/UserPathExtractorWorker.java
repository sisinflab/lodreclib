package it.poliba.sisinflab.LODRec.sprank.userPathExtractor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntFloatHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import it.poliba.sisinflab.LODRec.fileManager.StringFileManager;
import it.poliba.sisinflab.LODRec.utils.StringUtils;

/**
 * This class is part of the LOD Recommender
 * <p>
 * This class is used by UserPathExtractor for multi-threading paths extraction
 *
 * @author Vito Mastromarino
 */
public class UserPathExtractorWorker implements Runnable {

    private ArrayList<String> items_id; // items
    private TIntArrayList user_items;
    private int user_id;
    private TIntFloatHashMap trainRatings;
    private TIntFloatHashMap validationRatings;
    private BufferedWriter train_file;
    private BufferedWriter validation_file;
    private BufferedWriter test_file;
    private boolean normalize;
    private THashMap<String, String> items_path_index;
    private StringFileManager pathReader;
    private TObjectIntHashMap<String> path_index;
    private THashMap<String, String> paths;
    private String path_file;
    private int user_items_sampling;
    private float ratesThreshold;
    private TIntObjectHashMap<TIntHashSet> items_link;
    private TIntHashSet items_to_process;

    private static Logger logger = LogManager.getLogger(UserPathExtractorWorker.class.getName());

    /**
     * Constuctor
     */
    public UserPathExtractorWorker(int user_id, TIntFloatHashMap trainRatings,
                                   TIntFloatHashMap validationRatings, ArrayList<String> items_id,
                                   BufferedWriter train_file, BufferedWriter validation_file,
                                   BufferedWriter test_file, boolean normalize,
                                   THashMap<String, String> items_path_index, String path_file,
                                   TObjectIntHashMap<String> path_index, THashMap<String, String> paths,
                                   int user_items_sampling, float ratesThreshold,
                                   TIntObjectHashMap<TIntHashSet> items_link) {

        this.user_id = user_id;
        this.items_id = items_id;
        this.trainRatings = trainRatings;
        this.validationRatings = validationRatings;
        this.train_file = train_file;
        this.validation_file = validation_file;
        this.test_file = test_file;
        this.normalize = normalize;
        this.items_path_index = items_path_index;
        this.path_index = path_index;
        this.paths = paths;
        this.path_file = path_file;
        this.user_items_sampling = user_items_sampling;
        this.ratesThreshold = ratesThreshold;
        this.items_link = items_link;

    }

    /**
     * run path extraction
     */
    public void run() {

        try {
            start();
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * start path extraction considering all the pairs main_item-items
     *
     * @throws IOException
     */
    public void start() throws NumberFormatException, ClassNotFoundException, IOException {

        long start = System.currentTimeMillis();

        user_items = new TIntArrayList();
        items_to_process = new TIntHashSet();

        for (int item_rated : trainRatings.keys()) {
            /* controllo se l'item
			 * 1) è presente nel file metadata, 
			 * 2) è collegato ad altri item
			 */
            if (items_id.contains(Integer.toString(item_rated))
                    && items_link.contains(item_rated)) {
                user_items.add(item_rated);
            }
        }

        int real_num_items = user_items.size();
        int num_user_items = (user_items_sampling * user_items.size()) / 100;
        user_items = (TIntArrayList) user_items.subList(0, num_user_items);

        if (user_items.size() > 0) {

            TIntIterator it = user_items.iterator();
            while (it.hasNext())
                items_to_process.addAll(items_link.get(it.next()));

            //logger.info("user " + user_id + " start paths extraction");

            pathReader = new StringFileManager(path_file, items_path_index);

            TIntIterator it1 = items_to_process.iterator();
            while (it1.hasNext()) {
                int item_id = it1.next();
                buildFeatureVector(item_id, computePaths(item_id));
            }

            pathReader.close();

            synchronized (train_file) {
                train_file.flush();
            }
            synchronized (validation_file) {
                validation_file.flush();
            }
            synchronized (test_file) {
                test_file.flush();
            }

        }

        long stop = System.currentTimeMillis();
        logger.info("user " + user_id + "(" + user_items.size() + "/"
                + real_num_items + " items rated): paths extraction terminated in [sec] "
                + ((stop - start) / 1000));
    }


    /**
     * Extract paths from a user tree and an item tree
     *
     * @return paths map (path index:freq)
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NumberFormatException
     */
    public TIntIntHashMap computePaths(int item_id) {

        TIntIntHashMap res = new TIntIntHashMap();

        String item_pair_paths;
        boolean reverse;

        TIntIterator it = user_items.iterator();
        while (it.hasNext()) {

            reverse = false;
            int user_item_id = it.next();

            if (items_link.get(user_item_id).contains(item_id)) {

                if (user_item_id != item_id) {

                    String user_item_rate = StringUtils.extractRate(
                            trainRatings.get(user_item_id), ratesThreshold);

                    String key = user_item_id + "-" + item_id;

                    if (!pathReader.containsKey(key)) {
                        reverse = true;
                        key = item_id + "-" + user_item_id;
                    }

                    item_pair_paths = loadPathsFromMap(key);

                    String[] pair_vals = item_pair_paths.split(",");

                    if (pair_vals.length > 0) {

                        for (String s : pair_vals) {

                            String[] path_freq = s.split("=");
                            int key1 = 0;

                            if (reverse)
                                key1 = extractKey(user_item_rate + "-inv_"
                                        + path_freq[0]);
                            else
                                key1 = extractKey(user_item_rate + "-"
                                        + path_freq[0]);

                            res.adjustOrPutValue(key1,
                                    Integer.parseInt(path_freq[1]),
                                    Integer.parseInt(path_freq[1]));

                        }
                    }
                }
            }
        }

        return res;

    }

    private String loadPathsFromFile(String key) {

        return pathReader.read(key);

    }

    private String loadPathsFromMap(String key) {

        if (paths.containsKey(key))
            return paths.get(key);
        else
            return loadPathsFromFile(key);

    }

    /**
     * Extract key from path index
     *
     * @param s string to index
     * @return index of s
     */
    private int extractKey(String s) {

        return path_index.get(s);

    }

    private void buildFeatureVector(int item_id, TIntIntHashMap paths) {

        try {
            double rate = 0;
            double n = 1;
            boolean training = false, validation = false;
            DecimalFormat form = new DecimalFormat("#.####");
            form.setRoundingMode(RoundingMode.CEILING);
            StringBuffer str = new StringBuffer();

            if (trainRatings.containsKey(item_id)) {
                training = true;
                rate = trainRatings.get(item_id);
            } else if (validationRatings.containsKey(item_id)) {
                validation = true;
                rate = validationRatings.get(item_id);
            }

            if (normalize)
                n = norm(paths);

            str.append(rate + " qid:" + user_id + " 1:" + item_id + " ");

            for (int i = 1; i <= path_index.size(); i++) {

                int count = 0;

                if (paths.size() == 1)
                    n = norm(paths);

                if (paths.containsKey(i)) {

                    count = paths.get(i);

                    if (normalize)
                        str.append(i + 1 + ":"
                                + form.format(count / n).replace(",", ".")
                                + " ");
                    else
                        str.append(i + 1 + ":" + count + " ");

                }
            }

            if (training) {
                synchronized (train_file) {
                    train_file.append(str);
                    train_file.newLine();
                }
            } else if (validation) {
                synchronized (validation_file) {
                    validation_file.append(str);
                    validation_file.newLine();
                }
            } else {
                synchronized (test_file) {
                    test_file.append(str);
                    test_file.newLine();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double norm(TIntIntHashMap map) {

        int sum = 0;
        for (int i : map.keys()) {
            sum += (Math.pow(map.get(i), 2));
        }

        return Math.sqrt(sum);
    }
}
