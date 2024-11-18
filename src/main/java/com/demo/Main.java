package com.demo;

import java.sql.ClientInfoStatus;

import org.checkerframework.checker.units.qual.C;

public class Main {
    public static void main(String[] args) {

        try {
            new RunChartServer().start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        var client = new RunChartClient();
        var monitorThread = new Thread(new Thread( new Runnable() {

            @Override
            public void run() {
                client.monitor();
            }
        }) );

        var measurementThread = new Thread(new Thread( new Runnable() {

            @Override
            public void run() {
                client.sendMeasurements();
            }
        }) );

        var sendAndCheckThread = new Thread(new Thread( new Runnable() {

            @Override
            public void run() {
                client.sendAndCheck();
            }
        }) );

        measurementThread.start();
        monitorThread.start();
        sendAndCheckThread.start();

        var measurement = client.getSnapShot();

        try {
            measurementThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }

        

        System.out.println(measurement);
    }
}