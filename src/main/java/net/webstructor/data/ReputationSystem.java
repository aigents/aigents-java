/*
 * MIT License
 * 
 * Copyright (c) 2018-2020 Stichting SingularityNET
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.webstructor.data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/singnet/reputation/blob/master/reputation/reputation_api.py
 * @author akolonin
 */
public interface ReputationSystem {
	
	/**
	 * Set parameters for Reputation System 
	 * @param dict of all parameters that needs to be set (not listed parameters are not affected)
	 * @return 0 on success, integer error code on error
	 */
	public int set_parameters(Map<String,String> parameters);
	
	/**
	 * Delete entire contents of the ratings database
	 */
	public void clear_ratings();
	
	/**
	Rate (for implicit and explicit rates or stakes from any external sources)
		Input (array):
			From Id (who authoring the rating/staking record is may include both local id and name of system like cassio@google)
			Type (Stake, Rate, Transfer, Vote, Like, etc. - specific to given environment)
			To Id (who is being subject of the rating/staking is may include both local id and name of system like akolonin@google)
			Value (default/composite value, if multi-dimensional Data is not provided)
			Weight (like stake value or associated transaction value)
			Timestamp (Linux seconds or more precise to ms or nanos - TBD)
			Domains (array of 0 to many strings identifying categories e.g. House Cleaning, Text Clustering, etc.)
			Dimension Values (optional array)
				Dimension (identifying aspect e.g. Quality, Timeliness, etc.)
				Value for dimension (e.g. +1 or -1)
			Reference Id (like Id of associated transaction in the external system, including both id and system name like 0x12345@ethereum)
		Output (object)
			Result code (0 - success, error code otherwise)
	 * @param args
	 */
	public int put_ratings(Object[][] ratings);

	/**
	 * Query existing ratings
	 * @param ids - seed ids
	 * @param date - date
	 * @param period - number of days back
	 * @param range - link range
	 * @param threshold
	 * @param limit
	 * @param format
	 * @param links
	 * @return array of tuples of ratings [from type to value]
	 */
	//TODO: return weight and time
	public Object[][] get_ratings(String[] ids, Date date, int period, int range, int threshold, int limit, String format, String[] links);
	
	/**
	 * Delete entire contents of the ranks database
	 */
	public void clear_ranks();

	/**
	Update - to spawn/trigger background reputation update process, if needed to force externally
		Input (object)
			Timestamp - optional, default is current time (Linux seconds or more precise to ms or nanos - TBD)
			Domains - optional (array of 0 to many strings identifying categories e.g. House Cleaning, Text Clustering, etc.)
		Output (object)
			Result code (0 - success, 1 - in progress, 2 - consensus pending, error code otherwise)
	*/
	public int update_ranks(Date datetime, String[] domains);
	
	/**
	Retrieve (extracts current reputation computed by Update API or in background)
		Input (object)
			Timestamp - optional, default is current time (Linux seconds or more precise to ms or nanos - TBD)
			Domains (array) - in which categories (House Cleaning, Text Clustering, etc.) the reputation should be computed
			Dimensions (array) - which aspects (Quality, Timeliness, etc.) of the reputation should be retrieved 
			Ids (array) - which users should evaluated for their reputation (if not provided, all users are returned)
			Force Update - if true, forces update if not available by date/time
			From - starting which Id in the result set is to return results (default - 0)
			Length - home may Id-s is to return in results (default - all)
		Output (object)
			Result code (0 - success, 1 - in progress, 2 - consensus pending, error code otherwise)
			Percentage Completed (less than 100% if Result code is 1)
			Data (array, may not be sorted by Id)
				Id
				Ranks (array)
				Dimension
				Value
	 */
	public int get_ranks(Date datetime, String[] ids, String[] domains, String[] dimensions, boolean force, long at, long size, List results);

	/**
	 * Sets intital reputation state, if allowed
	 * @param datetime - reputation state date/time
	 * @param state array of tuples: id, value, optional array of per-dimension pairs of dimension and value: 
	 * @return
	 */
	public int put_ranks(Date datetime, Object[][] state);
}
