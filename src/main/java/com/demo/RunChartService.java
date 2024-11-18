package com.demo;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.demo.runchart.RunChartServiceGrpc.RunChartServiceImplBase;
import com.demo.runchart.Runchart;
import com.demo.runchart.Runchart.Empty;
import com.demo.runchart.Runchart.DataPoint;
import com.demo.runchart.Runchart.Warning;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;

public class RunChartService extends RunChartServiceImplBase {
    List<Measurement> measurements =  new ArrayList<>();
    List<StreamObserver<DataPoint>> dataObservers = new ArrayList<>();
    List<StreamObserver<Warning>> warningObservers = new ArrayList<>();

    @Override
    public void snapShot(Empty request, StreamObserver<DataPoint> responStreamObserver) {

        var measurement = new Measurement();

        measurement.setTimestamp(ZonedDateTime.now());
        measurement.setPartNumber("111-22-3344");
        measurement.setNominal(10.);
        measurement.setTolerance(1.);
        measurement.setMeasurement(10.);

        responStreamObserver.onNext(measurement.toDataPoint());

        responStreamObserver.onCompleted();

    }

    // stream observer interfacetir. onNext --> javanın next message u nasıl handle
    // edeceğini söyler
    // onError --> client server arasında bir sorun çıkarsa
    // onCompleted --> stream tamamlandıgında bildrir.
    @Override
    public StreamObserver<DataPoint> sendMeasurements(StreamObserver<Empty> responseObserver) {
        return new StreamObserver<DataPoint>() {
            @Override
            public void onNext(DataPoint value){ // datapoint value geldikten sonra ne yapacak ?
                //measurement eklemesini bekleriz
                addMeasurement(value);
            }
            @Override
            public void onError(Throwable t){ // clienttan error alırsam ne yapacagım?
                t.printStackTrace();
            }
            @Override
            public void onCompleted(){
                //client, server'ın iletişimi bitirdiğinde bir haber vermesini bekler.
                responseObserver.onNext(Empty.newBuilder().build()); // single message yollar
                responseObserver.onCompleted(); // bitirdiğinde complete eder
                //bu bir patterndir.
            }
            
        };
    };
    private void addMeasurement(DataPoint point){
        var m = new Measurement(point);
        System.out.println("Measurement received by server" + m );

        measurements.add(m);
        for (var a: dataObservers){
            a.onNext(point);
        }
         var hasWarning = false;
        String warning = "";
        if(m.getMeasurement() > m.getNominal() + m.getTolerance()){
            hasWarning = true;
            warning = "Measurement is above tolerance";
        }
        else if(m.getMeasurement() < m.getNominal() - m.getTolerance()){
            hasWarning = true;
            warning = "Measurement is below tolerance";
        }
        if (hasWarning){
            var p = measurements.getLast();
            var w =Warning.newBuilder()
                                .setTimestamp(Timestamp.newBuilder().build())
                                .setPartNumber(p.getPartNumber())
                                .setSpecNominal(p.getNominal())
                                .setSpecTolerance(p.getTolerance())
                                .setWarning(warning)
                                .build();
            for (var s: warningObservers){
                s.onNext(w);
            }
        }
    }

    @Override
    public void monitor(Empty request, StreamObserver<DataPoint> responseObserver) {
        //ne bekliyorum kısmı onNext methoduna denk gelir. ne bekliyorum? stream response u gondermeyi bekliyorum
        //her data point geldiginde responseObserver ı gondermem lazım. ama bize datapoint gelmiyor. cunku empty request geliyor.

        dataObservers.add(responseObserver);       

    }

    @Override
    public StreamObserver<DataPoint> sendAndCheck(StreamObserver<Warning> responseObserver) {
        warningObservers.add(responseObserver);
        return new StreamObserver<DataPoint>() {
            @Override
            public void onNext(DataPoint value) {
                addMeasurement(value);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                for (var s: warningObservers){
                    s.onCompleted();
                }
            }
        };
    }
}
