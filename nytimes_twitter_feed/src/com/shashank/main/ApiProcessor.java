package com.shashank.main;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of the API Processor that makes API calls to New York Times and Twitter based on the search topic.
 * It implements the Callable interface to perform tasks submitted by the ExecutorService.
 * It also generates the Result object based on the tweets obtained from the Twitter API.
 */
public class ApiProcessor implements Callable<ResultBuilder>
{
    private final static Logger LOGGER = Logger.getLogger(ApiProcessor.class.getName());
    private final List<String> queryParameters;
    private final int RESULTS_PER_PAGE = 10;
    private final SimpleDateFormat stringFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());
    private final SimpleDateFormat newYorkTimesDateFormatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat twitterDateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public ApiProcessor(List<String> queryParameters)
    {
        this.queryParameters = queryParameters;
    }

    /**
     * @return Returns a Result object based on tweets obtained from the Twitter API.
     */
    public ResultBuilder call()
    {
        final String searchTopic = queryParameters.get(0);
        int articleLimit = 10;
        int tweetLimit = 1500;
        int timeRangeInDays = 6;
        final Map<String, List<JSONObject>> mapOfArticleTitleToTweets = new HashMap<String, List<JSONObject>>();
        if (queryParameters.get(1) != null)
        {
            articleLimit = Integer.parseInt(queryParameters.get(1));
        }
        if (queryParameters.get(2) != null)
        {
            tweetLimit = Integer.parseInt(queryParameters.get(2));
        }
        if (queryParameters.get(3) != null)
        {
            timeRangeInDays = Integer.parseInt(queryParameters.get(3));
        }
        if (timeRangeInDays == 0 || tweetLimit == 0)
        {
            return new ResultBuilder(new HashMap<String, List<JSONObject>>(), new ArrayList<JSONObject>());
        }
        final List<JSONObject> listOfTopicTweets = getTweetsFromTwitter(searchTopic, tweetLimit, timeRangeInDays);
        if (articleLimit != 0)
        {
            final List<String> listOfArticleTitles = getArticleTitlesFromNewYorkTimes(searchTopic, articleLimit, timeRangeInDays);
            for (final String articleTitle : listOfArticleTitles)
            {
                final List<JSONObject> listOfArticleTweets = getTweetsFromTwitter(articleTitle, tweetLimit, timeRangeInDays);
                mapOfArticleTitleToTweets.put(articleTitle, listOfArticleTweets);
            }
        }
        return new ResultBuilder(mapOfArticleTitleToTweets, listOfTopicTweets);
    }

    /**
     * Get the list of article titles, given the search parameters.
     *
     * @param searchTopic     the topic to search articles for.
     * @param articleLimit    the number of articles to limit the search to.
     * @param timeRangeInDays timeRangeInDays the number of days to limit the search to.
     * @return The list of article titles.
     */
    private List<String> getArticleTitlesFromNewYorkTimes(final String searchTopic, final int articleLimit, final int timeRangeInDays)
    {
        LOGGER.log(Level.INFO, "Method call to getArticleTitlesFromNewYorkTimes with search topic [" + searchTopic + "]");
        int maxOffset;
        String beginDate;
        final Calendar now = Calendar.getInstance();
        now.add(Calendar.DATE, 1);
        final String endDate = newYorkTimesDateFormatter.format(now.getTime());
        maxOffset = (articleLimit / RESULTS_PER_PAGE);
        final Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, -(timeRangeInDays - 1));
        beginDate = newYorkTimesDateFormatter.format(today.getTime());
        return getListOfArticleTitles(searchTopic, beginDate, endDate, maxOffset, articleLimit);
    }

    /**
     * Get the list of article titles, given the search parameters.
     *
     * @param searchTopic  the topic to search articles for.
     * @param beginDate    the date to begin the search from.
     * @param endDate      the date to end the search.
     * @param maxOffset    the number of pages from the New York Times API.
     * @param articleLimit the number of articles to limit the search to.
     * @return the list of article titles.
     */
    private List<String> getListOfArticleTitles(final String searchTopic, final String beginDate, final String endDate, final int maxOffset, final int articleLimit)
    {
        final List<String> listOfArticleTitles = new ArrayList<String>();
        for (int i = 0; i <= maxOffset; i++)
        {
            final String newYorkTimesBaseUri = "http://api.nytimes.com/svc/search/v1/article";
            final String newYorkTimesApiKey = "228368a1f5a3142348db3c227923f577:15:65714731";
            final String encodedSearchTopic = encodeString(searchTopic);
            final String articleSearchUrl = newYorkTimesBaseUri + "?format=json&query=" + encodedSearchTopic + "&offset=" + Integer.toString(i) +
                    "&begin_date=" + beginDate + "&end_date=" + endDate + "&api-key=" + newYorkTimesApiKey;
            try
            {
                final String articleSearchJson = executeApiCall(articleSearchUrl);
                final JSONObject articleSearchJsonObject = new JSONObject(articleSearchJson);
                final JSONArray resultsJsonArray = new JSONArray(articleSearchJsonObject.getString("results"));
                for (int j = 0; j < resultsJsonArray.length(); j++)
                {
                    if (listOfArticleTitles.size() >= articleLimit)
                    {
                        break;
                    }
                    final JSONObject resultJsonObject = resultsJsonArray.getJSONObject(j);
                    listOfArticleTitles.add(resultJsonObject.getString("title"));
                }
                if (articleSearchJsonObject.getInt("total") < RESULTS_PER_PAGE * (i + 1))
                {
                    break;
                }
            }
            catch (JSONException e)
            {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
        return listOfArticleTitles;
    }

    private String encodeString(final String item)
    {
        String encodedItem = null;
        try
        {
            encodedItem = URLEncoder.encode(item, "UTF8");
        }
        catch (UnsupportedEncodingException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return encodedItem;
    }

    /**
     * Executes the API call.
     *
     * @param searchUrl the url for the search API call.
     * @return the article json response.
     */
    private String executeApiCall(final String searchUrl)
    {
        LOGGER.log(Level.INFO, "Method call to executeApiCall with search url [" + searchUrl + "]");
        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet(searchUrl);
        final StringBuilder builder = new StringBuilder();
        try
        {
            final HttpResponse response = client.execute(request);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            for (String line; (line = reader.readLine()) != null; )
            {
                builder.append(line);
            }
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return builder.toString();
    }

    /**
     * Gets the list of tweets for a given search query string and a set of parameters.
     *
     * @param searchItem      the search query string.
     * @param tweetLimit      the number of tweets to limit the search to.
     * @param timeRangeInDays the number of days to limit the search to.
     * @return the list of tweets.
     */
    private List<JSONObject> getTweetsFromTwitter(final String searchItem, final int tweetLimit, final int timeRangeInDays)
    {
        LOGGER.log(Level.INFO, "Method call to getTweetsFromTwitter with search item [" + searchItem + "]");
        final List<JSONObject> listOfTweets = new ArrayList<JSONObject>();
        String beginDate;
        boolean haveMoreResults = true;
        final Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, -(timeRangeInDays - 1));
        beginDate = twitterDateFormatter.format(today.getTime());
        final String encodedSearchItem = encodeString(searchItem);
        String nextPageParameter = null;
        String twitterSearchUrl = "http://search.twitter.com/search.json?q=" + encodedSearchItem + "&rpp=" + 100;
        if (beginDate != null)
        {
            twitterSearchUrl = twitterSearchUrl.concat("&since=" + beginDate);
        }
        while (haveMoreResults)
        {
            if (nextPageParameter != null)
            {
                twitterSearchUrl = "http://search.twitter.com/search.json" + nextPageParameter;
            }
            final String twitterSearchJson = executeApiCall(twitterSearchUrl);
            try
            {
                final JSONObject twitterSearchJsonObject = new JSONObject(twitterSearchJson);
                final JSONArray resultsJsonArray = new JSONArray(twitterSearchJsonObject.getString("results"));
                for (int j = 0; j < resultsJsonArray.length(); j++)
                {
                    final JSONObject resultJsonObject = resultsJsonArray.getJSONObject(j);
                    try
                    {
                        if (stringFormatter.parse(resultJsonObject.getString("created_at")).before(twitterDateFormatter.parse(beginDate)))
                        {
                            haveMoreResults = false;
                            break;
                        }
                        listOfTweets.add(resultJsonObject);
                    }
                    catch (ParseException e)
                    {
                        LOGGER.log(Level.SEVERE, e.getMessage());
                    }
                    if (listOfTweets.size() >= tweetLimit)
                    {
                        haveMoreResults = false;
                        break;
                    }
                }
                nextPageParameter = twitterSearchJsonObject.getString("next_page");
            }
            catch (JSONException e)
            {
                LOGGER.log(Level.INFO, "No more tweets could be found.");
                return listOfTweets;
            }
        }
        return listOfTweets;
    }
}
