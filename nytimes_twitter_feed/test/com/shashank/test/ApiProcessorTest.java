package com.shashank.test;

import com.shashank.main.ApiProcessor;
import com.shashank.main.ResultBuilder;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ApiProcessorTest
{
    @Test
    public void validApiCall()
    {
        List<String> queryParameters = new ArrayList<String>();
        queryParameters.add("Republic Debates");
        queryParameters.add("10");
        queryParameters.add("20");
        queryParameters.add("3");
        ApiProcessor processor = new ApiProcessor(queryParameters);
        processor.call();
    }

    @Test
    public void tweetLimitArgumentIsZero()
    {
        List<String> queryParameters = new ArrayList<String>();
        queryParameters.add("Republic Debates");
        queryParameters.add("10");
        queryParameters.add("0");
        queryParameters.add("3");
        ApiProcessor processor = new ApiProcessor(queryParameters);
        ResultBuilder result = processor.call();
        assert(result.getMapOfDateToTopicTweets().size() == 0);
    }

    @Test
    public void timeRangeArgumentIsZero()
    {
        List<String> queryParameters = new ArrayList<String>();
        queryParameters.add("Republic Debates");
        queryParameters.add("10");
        queryParameters.add("20");
        queryParameters.add("0");
        ApiProcessor processor = new ApiProcessor(queryParameters);
        ResultBuilder result = processor.call();
        assert(result.getMapOfDateToTopicTweets().size() == 0);
    }
}