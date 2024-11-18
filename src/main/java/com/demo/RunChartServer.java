package com.demo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

public class RunChartServer {
    public static int port = 3030;
    private final Server server;

    public RunChartServer(){
        //builder pattern grpcde çok kullanılır.
        //InsecureServerCredentials class'ı prodda çok tercih edilmez.
        var builder = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create());

        server = builder.addService(new RunChartService()).build();
    }

    //server başlatıldı mı? dinleniyor mu check
    public void start() throws IOException{
        server.start();
        System.out.println("Server started...");

        //shutdown mekanizması kuralım. Server'ın kapandıgından emin olmamız gerek. TCP Portlarının kapanmış olmasını bekleriz. Yanlışlıkla açık kalması üretim ortamında sıkıntı yaratabilir.
        Runtime.getRuntime().addShutdownHook(new Thread(){

            @Override
            public void run() {
                try{
                    RunChartServer.this.stop();
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            
        });
    }
    public void stop() throws InterruptedException{
        if(server != null){
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

}
