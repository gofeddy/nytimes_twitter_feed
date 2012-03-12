package com.shashank.main;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Execution of the program begins.
 */
public class MainApp
{
    private static final int THREAD_COUNT = 10;
    private static final Logger LOGGER = Logger.getLogger(MainApp.class.getName());

    public static void main(final String[] args)
    {
        MainApp app = new MainApp();
        LOGGER.log(Level.INFO, "Starting up executor.");
        final ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        boolean status = app.begin(args, executor);
        if (!status)
        {
            System.exit(1);
        }
        LOGGER.log(Level.INFO, "Shutting down executor.");
        executor.shutdown();
    }

    public boolean begin(final String[] args, final ExecutorService executor)
    {
        /* Invalid command line argument count */
        if (args.length < 1)
        {
            System.out.println("No topic provided.");
            System.out.println("Usage: java -jar nytsp.jar topic_name article_limit<optional> tweet_limit" +
                    "<optional> time_range<optional>");
            return false;
        }
        /* Invalid command line argument count */
        else if (args.length > 4)
        {
            System.out.println("Too many arguments provided.");
            System.out.println("Usage: java -jar nytsp.jar topic_name article_limit<optional> tweet_limit" +
                    "<optional> time_range<optional>");
            return false;
        }
        /* Evaluating command line arguments individually */
        else
        {
            LOGGER.log(Level.INFO, "Topic: " + args[0]);
            try
            {
                /* Check if arguments can be parsed to an integer */
                if (args[1] != null)
                {
                    Integer.parseInt(args[1]);
                    LOGGER.log(Level.INFO, "Article Limit: " + args[1]);
                }
                if (args[2] != null)
                {
                    Integer.parseInt(args[2]);
                    LOGGER.log(Level.INFO, "Tweet Limit: " + args[2]);
                }
                if (args[3] != null)
                {
                    final int timeRange = Integer.parseInt(args[3]);
                    if (timeRange > 6)
                    {
                        System.out.println("Provide a time range value less than 6");
                        return false;
                    }
                    LOGGER.log(Level.INFO, "Time Range: " + args[3] + " days");
                }
            }
            /* Unable to parse command line arguments */
            catch (NumberFormatException e)
            {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, e.getMessage());
                return false;
            }
            final List<String> queryParameters = new ArrayList<String>();
            Collections.addAll(queryParameters, args);
            /* Process individual search request in a new thread */
            Callable<ResultBuilder> worker = new ApiProcessor(queryParameters);
            LOGGER.log(Level.INFO, "Submitting a new search request.");
            /* Process the response from individual thread */
            final Future<ResultBuilder> submit = executor.submit(worker);
            try
            {
                /* Parse and print the results obtained */
                final ResultBuilder result = submit.get();
                LOGGER.log(Level.INFO, "Received response from search request.");
                System.out.println("----------------" + args[0] + "----------------");
                System.out.println("Date:" + "\t\t" + "Tweets");
                int totalTopicTweets = 0;
                for (Map.Entry<String, Integer> entry : result.getMapOfDateToTopicTweets().entrySet())
                {
                    System.out.println(entry.getKey() + "\t\t" + entry.getValue());
                    totalTopicTweets += entry.getValue();
                }
                System.out.println("----------------");
                System.out.println("Total tweets: " + "\t\t" + totalTopicTweets);
                for (final Map.Entry<String, Map<String, Integer>> entry : result.getMapOfArticleTitleToMapOfDateToArticleTweets().entrySet())
                {
                    System.out.println("----------------" + entry.getKey() + "----------------");
                    System.out.println("Date:" + "\t\t" + "Tweets:");
                    int totalArticleTweets = 0;
                    for (final Map.Entry<String, Integer> entry_2 : entry.getValue().entrySet())
                    {
                        System.out.println(entry_2.getKey() + "\t\t" + entry_2.getValue());
                        totalArticleTweets += entry_2.getValue();
                    }
                    System.out.println("----------------");
                    System.out.println("Total tweets: " + "\t\t" + totalArticleTweets);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, e.getMessage());
                return false;
            }
            catch (ExecutionException e)
            {
                e.printStackTrace();
                LOGGER.log(Level.SEVERE, e.getMessage());
                return false;
            }
        }
        return true;
    }
}
