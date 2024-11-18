package com.demo;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import com.demo.runchart.Runchart.DataPoint;
import com.google.protobuf.Timestamp; 

public class Measurement {

  private ZonedDateTime timestamp;
  private String partNumber;
  private Double nominal;
  private Double tolerance;
  private Double measurement;

  public Measurement() {}

  public Measurement(DataPoint point) {
    timestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(point.getTimestamp().getSeconds()), TimeZone.getTimeZone("UTC").toZoneId());
    partNumber = point.getPartNumber();
    nominal = point.getSpecNominal();
    tolerance = point.getSpecTolerance();
    measurement = point.getMeasurement();
  }

  public DataPoint toDataPoint() {
    return DataPoint.newBuilder()
      .setTimestamp(Timestamp.newBuilder().setSeconds(timestamp.toEpochSecond()))
      .setPartNumber(partNumber)
      .setSpecNominal(nominal)
      .setSpecTolerance(tolerance)
      .setMeasurement(measurement)
      .build();
  }

  public String toString() {
    return String.format("Timestamp: %tc\nPart number: %s\nSpec: %f±%f\nMeasurent: %f\n", timestamp, partNumber, nominal, tolerance, measurement);
  }

  public ZonedDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(ZonedDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getPartNumber() {
    return partNumber;
  }

  public void setPartNumber(String partNumber) {
    this.partNumber = partNumber;
  }

  public Double getNominal() {
    return nominal;
  }

  public void setNominal(Double nominal) {
    this.nominal = nominal;
  }

  public Double getTolerance() {
    return tolerance;
  }

  public void setTolerance(Double tolerance) {
    this.tolerance = tolerance;
  }

  public Double getMeasurement() {
    return measurement;
  }

  public void setMeasurement(Double measurement) {
    this.measurement = measurement;
  }
}
