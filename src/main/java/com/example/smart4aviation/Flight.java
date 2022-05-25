package com.example.smart4aviation;

public class Flight {
    public int flightId;
    public int flightNumber;
    public String departureAirportIATACode;
    public String arrivalAirportIATACode;
    public String departureDate;

    public Flight(int id, int number, String departure, String arrival, String date) {
        this.flightId = id;
        this.flightNumber = number;
        this.departureAirportIATACode = departure;
        this.arrivalAirportIATACode = arrival;
        this.departureDate = date;
    }

    public int getFlightId() {
        return flightId;
    }

    public int getFlightNumber() {
        return flightNumber;
    }
    public String getDepartureIATA() {
        return departureAirportIATACode;
    }

    public String getArrivalIATA() {
        return arrivalAirportIATACode;
    }

    public String getDepartureDate() {
        return departureDate;
    }
}
