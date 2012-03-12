package com.shashank.main;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes the list of tweets for both articles and the topic to generate a per-day count of tweets for the topic and each article.
 */
public class ResultBuilder
{
    private final static Logger LOGGER = Logger.getLogger(ResultBuilder.class.getName());
    private final Map<String, List<JSONObject>> mapOfArticleTitleToTweets;
    private final List<JSONObject> listOfTopicTweets;
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat stringFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.getDefault());

    public ResultBuilder(Map<String, List<JSONObject>> mapOfArticleTitleToTweets, List<JSONObject> listOfTopicTweets)
    {
        this.mapOfArticleTitleToTweets = mapOfArticleTitleToTweets;
        this.listOfTopicTweets = listOfTopicTweets;
    }

    /**
     * @return Returns a map containing the article title as the key and day-by-day mapping of tweet counts as the value.
     */
    public Map<String, Map<String, Integer>> getMapOfArticleTitleToMapOfDateToArticleTweets()
    {
        Map<String, Map<String, Integer>> mapOfArticleTitleToMapOfDateToArticleTweets = new HashMap<String, Map<String, Integer>>();
        for (String articleTitle : mapOfArticleTitleToTweets.keySet())
        {
            mapOfArticleTitleToMapOfDateToArticleTweets.put(articleTitle, getMapOfDateToArticleTweets(articleTitle));
        }
        return mapOfArticleTitleToMapOfDateToArticleTweets;
    }

    private Map<String, Integer> getMapOfDateToArticleTweets(String articleTitle)
    {
        Map<String, Integer> mapOfDateToTweetCount = new HashMap<String, Integer>();
        for (JSONObject topicTweet : mapOfArticleTitleToTweets.get(articleTitle))
        {
            updateMap(topicTweet, mapOfDateToTweetCount);
        }
        return mapOfDateToTweetCount;
    }

    /**
     * @return Returns a mapping containing the tweet count for an observed day.
     */
    public Map<String, Integer> getMapOfDateToTopicTweets()
    {
        Map<String, Integer> mapOfDateToTweetCount = new HashMap<String, Integer>();
        for (JSONObject topicTweet : listOfTopicTweets)
        {
            updateMap(topicTweet, mapOfDateToTweetCount);
        }
        return mapOfDateToTweetCount;
    }

    private void updateMap(JSONObject tweet, Map<String, Integer> map)
    {
        String createdDate = null;
        try
        {
            createdDate = tweet.getString("created_at");
        }
        catch (JSONException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        String date = null;
        try
        {
            date = dateFormatter.format(stringFormatter.parse(createdDate));
        }
        catch (ParseException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        if (map.containsKey(date))
        {
            int currentTotal = map.get(date);
            map.put(date, ++currentTotal);
        }
        else
        {
            map.put(date, 1);
        }
    }
}
