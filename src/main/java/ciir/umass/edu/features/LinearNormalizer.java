/*===============================================================================
 * Copyright (c) 2010-2012 University of Massachusetts.  All Rights Reserved.
 *
 * Use of the RankLib package is subject to the terms of the software license set 
 * forth in the LICENSE file included with this software, and also available at
 * http://people.cs.umass.edu/~vdang/ranklib_license.html
 *===============================================================================
 */

package ciir.umass.edu.features;

import java.util.Arrays;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.RankList;

/**
 * @author Laura Dietz, vdang
 */
public class LinearNormalizer extends Normalizer {
	@Override
	public void normalize(RankList rl) {
		if (rl.size() == 0) {
			System.out
					.println("Error in LinearNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}
		int nFeature = DataPoint.getFeatureCount();
		int[] fids = new int[nFeature];
		for (int i = 1; i <= nFeature; i++)
			fids[i - 1] = i;
		normalize(rl, fids);
	}

	@Override
	public void normalize(RankList rl, int[] fids) {
		if (rl.size() == 0) {
			System.out
					.println("Error in LinearNormalizor::normalize(): The input ranked list is empty");
			System.exit(1);
		}

		// remove duplicate features from the input @fids ==> avoid normalizing
		// the same features multiple times
		fids = removeDuplicateFeatures(fids);

		float[] min = new float[fids.length];
		float[] max = new float[fids.length];
		Arrays.fill(min, 0);
		Arrays.fill(max, 0);
		for (int i = 0; i < rl.size(); i++) {
			DataPoint dp = rl.get(i);
			for (int j = 0; j < fids.length; j++) {
				min[j] = Math.min(min[j], dp.getFeatureValue(fids[j]));
				max[j] = Math.max(max[j], dp.getFeatureValue(fids[j]));
			}
		}
		for (int i = 0; i < rl.size(); i++) {
			DataPoint dp = rl.get(i);
			for (int j = 0; j < fids.length; j++) {
				if (dp.getFeatureValue(fids[j]) > 0) {
					if (max[j] > min[j]) {
						float value = (dp.getFeatureValue(fids[j]) - min[j])
								/ (max[j] - min[j]);
						dp.setFeatureValue(fids[j], value);
					} else
						dp.setFeatureValue(fids[j], 0);
				}
			}
		}
	}

	public String name() {
		return "linear";
	}
}
