package com.shashank.test;

import com.shashank.main.MainApp;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class MainAppTest
{
    @Test
    public void allCommandLineArgumentsAreValid()
    {
        MainApp app = new MainApp();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] args = {"Republic Debates", "10", "20", "3"};
        boolean status = app.begin(args, executor);
        assertEquals(status, true);
    }

    @Test
    public void noCommandLineArgumentsSpecified()
    {
        MainApp app = new MainApp();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] args = {};
        boolean status = app.begin(args, executor);
        assertEquals(status, false);
    }

    @Test
    public void oneCommandLineArgumentInvalid()
    {
        MainApp app = new MainApp();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] args = {"Republic Debates", "10f", "20", "3"};
        boolean status = app.begin(args, executor);
        assertEquals(status, false);
    }

    @Test
    public void timeRangeCommandLineArgumentExceedsSix()
    {
        MainApp app = new MainApp();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] args = {"Republic Debates", "10f", "20", "8"};
        boolean status = app.begin(args, executor);
        assertEquals(status, false);
    }

    @Test
    public void moreThanFourCommandLineArgumentsSpecified()
    {
        MainApp app = new MainApp();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        String[] args = {"Republic Debates", "10f", "20", "3", "4"};
        boolean status = app.begin(args, executor);
        assertEquals(status, false);
    }
}