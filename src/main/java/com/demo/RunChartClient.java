package com.demo;

import java.io.FileReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import com.demo.runchart.Runchart;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.demo.runchart.RunChartServiceGrpc;
import com.demo.runchart.RunChartServiceGrpc.RunChartServiceBlockingStub;
import com.demo.runchart.RunChartServiceGrpc.RunChartServiceStub;
import com.demo.runchart.Runchart.DataPoint;
import com.demo.runchart.Runchart.Empty;
import com.demo.runchart.Runchart.Warning ;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class RunChartClient {
  private final RunChartServiceBlockingStub blockingStub; //servisi çağıran stubdır. service donus yapana kadar client'ın onu beklemesini saglar
// newStub methodu asyn calisir. istek atılır async servera baglanır.
//featureStub, newStub gibi asyncdir. 
  private final RunChartServiceStub asyncStub; // stub, client ve channel'ın iletişimini sağlar
//3 cesit stub var grpcden gelen. 
  private final Channel channel; 
 
  public RunChartClient() {
    channel = ManagedChannelBuilder.forAddress("localhost", RunChartServer.port).usePlaintext().build(); //address ve tcp port connect. prodda plaintext kullanma cunku bu encrypt değil demek oluyor.

    blockingStub = RunChartServiceGrpc.newBlockingStub(channel); //bu sekilde serverla nasıl communicate edecegimizi belirledik. Bunu kullanacagiz.
    asyncStub = RunChartServiceGrpc.newStub(channel);
  }

  public Measurement getSnapShot() {
    var datapoint =  blockingStub.snapShot(Empty.newBuilder().build());

    return new Measurement(datapoint);
  }

  public void sendMeasurements() {
    //proto'ya sendMeasurements tanımladık, serverside'da bu methodun ne yapacağını belirttik. şimdi client'ta bu istek nasıl atılıyor onu tanımlayacağız.
    //async stub, biz mesajları yolluyoruz ama cevabın gelmesini beklemiyoruz
    var measurements = importMeasurements();

    var responseObserver = new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty empty) {} //logic yok burada o yuzden bos
      @Override
      public void onError(Throwable t) { //
        t.printStackTrace();
      }
      @Override
      public void onCompleted() {} //
    };
    var requestObserver  = asyncStub.sendMeasurements(responseObserver); // server'la iletişim kurmasını bekliyoruz
    //burada bir akış gönderdik responseObserver ile. Akış içinde bir hata, tamamlama, veya onNext aktivistesi handle edilir.
    try {
      for (var m  : measurements){
        requestObserver.onNext(m.toDataPoint());
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    finally{
      requestObserver.onCompleted(); //finally blogu actik cunku bu islemin her zaman onCompleted ile bitmesini bekliyoruz.
    }
  }
  public void monitor(){
    var pointStream  = blockingStub.monitor(Empty.newBuilder().build()); // blockingStub basitçe bir istek atar ve server'dan cevabı bekler.
    //asyncStub ise server'a bir istek atar ve server'dan cevap beklemek yerine, server'dan gelen cevapları handle edebileceğimiz bir StreamObserver döner.
    while ( pointStream.hasNext() ) {
      var point = pointStream.next();
      System.out.println(new Measurement(point));
    }
  }
  public void sendAndCheck(){
    var responseObserver = new StreamObserver<Warning>() {
      @Override
      public void onNext(Warning w) {}
      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }
      @Override
      public void onCompleted() {
        System.out.println("Server has received the measurements");
      }
    };
    var requestObserver = asyncStub.sendAndCheck(responseObserver);
  }
  private ArrayList<Measurement> importMeasurements() {
    var measurements = new ArrayList<Measurement>();

    var parser = new JSONParser(); 
    try {
      var jsonArray = (JSONArray) parser.parse(new FileReader("src/main/data.json"));
      for (Object jsonObject : jsonArray) {
        var o = (JSONObject) jsonObject;
        var m = new Measurement();
        m.setTimestamp(ZonedDateTime.parse((String) o.get("timestamp")));
        m.setPartNumber((String) o.get("part_number"));
        m.setNominal((double) o.get("nominal"));
        m.setTolerance((double) o.get("tolerance"));
        m.setMeasurement((double) o.get("measurement"));

        measurements.add(m);
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
    return measurements;
  }
}
