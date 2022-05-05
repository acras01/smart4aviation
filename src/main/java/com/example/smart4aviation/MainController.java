package com.example.smart4aviation;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

public class MainController {

    public Label num_arr_flights;
    public Label num_dep_flights;
    public Label num_arr_pieces;
    public Label num_dep_pieces;
    public FlowPane iata_pane;
    public Button fetch_flight;
    public Button fetch_iata;
    public ChoiceBox<Integer> flight_list;
    public ChoiceBox<String> iata_list;
    public DatePicker flight_date;
    public DatePicker iata_date;
    public Label cargo_weight;
    public Label baggage_weight;
    public Label total_weight;

    List<Flights> flights = new ArrayList<>();
    List<Cargoes> cargo = new ArrayList<>();

    private final Executor executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "controller-thread");
        t.setDaemon(true);
        return t;
    });

    @FXML
    private void initialize() {

        iata_date.setDisable(true);
        fetch_iata.setDisable(true);

        flight_date.setDisable(true);
        fetch_flight.setDisable(true);

        ParseJSONTask<Utils.Data> task = new ParseJSONTask<>(new Utils.Data());
        task.setOnSucceeded(success -> {
            if (task.getValue() != null) {
                flights = task.getValue().flights;
                cargo = task.getValue().cargo;
            }
            List<Integer> flist = flights.stream().map(Flights::getFlightNumber).toList();
            flight_list.setItems(FXCollections.observableList(flist));

            List<String> alist = new ArrayList<>(flights.stream().map(Flights::getArrivalIATA).toList());
            alist.addAll(flights.stream().map(Flights::getDepartureIATA).toList());
            alist = new ArrayList<>(new HashSet<>(alist));
            iata_list.setItems(FXCollections.observableList(alist));
        });
        task.setOnFailed(error -> task.getException().printStackTrace());

        executor.execute(task);

        iata_list.getSelectionModel().selectedItemProperty()
                .addListener((ov, value, new_value) -> {
                    num_arr_flights.setVisible(false);
                    num_dep_flights.setVisible(false);
                    num_arr_pieces.setVisible(false);
                    num_dep_pieces.setVisible(false);
                    List<LocalDate> dates = new ArrayList<>();
                    for (Flights f : flights) {
                        if (new_value.equals(f.arrivalAirportIATACode) ||
                                new_value.equals(f.departureAirportIATACode)) {
                            dates.add(LocalDate.parse(f.departureDate, ISO_OFFSET_DATE_TIME));
                        }
                    }
                    restrictDatePicker(iata_date, dates);
                    iata_date.setValue(dates.get(0));
                    iata_date.setDisable(false);
                });

        flight_list.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, new_value) -> {
                    cargo_weight.setVisible(false);
                    baggage_weight.setVisible(false);
                    total_weight.setVisible(false);
                    List<LocalDate> dates = new ArrayList<>();
                    for (Flights f : flights) {
                        if (new_value.intValue() == f.flightId) {
                            dates.add(LocalDate.parse(f.departureDate, ISO_OFFSET_DATE_TIME));
                        }
                    }
                    restrictDatePicker(flight_date, dates);
                    flight_date.setValue(dates.get(0));
                    flight_date.setDisable(false);
                });

        iata_date.valueProperty().addListener((ov, oldDate, newDate) -> {
            fetch_iata.setDisable(false);
        });

        flight_date.valueProperty().addListener((ov, oldDate, newDate) -> {
            fetch_flight.setDisable(false);
        });
    }

    public void restrictDatePicker(DatePicker datePicker, List<LocalDate> dates) {
        final Callback<DatePicker, DateCell> dayCellFactory = new Callback<>() {
            @Override
            public DateCell call(final DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        setDisable(empty || !dates.contains(item));
                    }
                };
            }
        };
        datePicker.setDayCellFactory(dayCellFactory);
    }

    private static class ParseJSONTask<D> extends Task<Utils.Data> {

        public Utils.Data data;

        public ParseJSONTask(Utils.Data data) {
            this.data = data;
        }

        @Override
        protected Utils.Data call() throws Exception {
            return Utils.fetchData(data);
        }

    }
    @FXML
    protected void onIATAButtonClick(ActionEvent actionEvent) {
        String iata = iata_list.getSelectionModel().getSelectedItem();
        String date = iata_date.getValue().format(ISO_DATE);
        int dep_weight_pieces = 0;
        int arr_weight_pieces = 0;
        int arr_flights = 0;
        int dep_flights = 0;
        List <Flights> flightsToDate = flights.stream().filter(f -> LocalDate.parse(f.departureDate, ISO_OFFSET_DATE_TIME).format(ISO_DATE).equals(date)).toList();
        for (Flights f : flightsToDate) {
            List<Cargoes> cargo_flight = cargo.stream().filter(c -> c.flightId == f.flightId).toList();
            if (f.arrivalAirportIATACode.equals(iata)) {
                arr_flights++;
                for (Cargoes c : cargo_flight) {
                    for (Cargoes.Baggage b : c.baggage) {
                        arr_weight_pieces += b.pieces;
                    }
                }
            } else if (f.departureAirportIATACode.equals(iata)) {
                dep_flights++;
                for (Cargoes c : cargo_flight) {
                    for (Cargoes.Baggage b : c.baggage) {
                        dep_weight_pieces += b.pieces;
                    }
                }
            }
        }
        num_arr_flights.setText("Number of arriving flights: " + arr_flights);
        num_arr_flights.setVisible(true);
        num_dep_flights.setText("Number of departing flights: " + dep_flights);
        num_dep_flights.setVisible(true);
        num_arr_pieces.setText("Number of baggage peaces (arriving): " + arr_weight_pieces);
        num_arr_pieces.setVisible(true);
        num_dep_pieces.setText("Number of baggage pieces (departing): " + dep_weight_pieces);
        num_dep_pieces.setVisible(true);

    }

    public void onFlightButtonClick(ActionEvent actionEvent) {
        int flight = flight_list.getSelectionModel().getSelectedItem();
        String date = flight_date.getValue().format(ISO_DATE);
        int cargolbs = 0;
        int baggagelbs = 0;
        int totallbs = 0;
        int cargokg = 0;
        int baggagekg = 0;
        int totalkg = 0;
        List<Flights> flightsToDate = flights
                .stream()
                .filter(f -> LocalDate.parse(f.departureDate, ISO_OFFSET_DATE_TIME).format(ISO_DATE).equals(date))
                .filter(f -> f.flightNumber == flight)
                .toList();
        for (Flights f : flightsToDate) {
            List<Cargoes> cargo_flight = cargo.stream().filter(c -> c.flightId == f.flightId).toList();
            for (Cargoes c : cargo_flight) {
                for (Cargoes.Baggage b : c.baggage) {
                    switch (b.weightUnit) {
                        case ("lb") -> baggagelbs += b.weight;
                        case ("kg") -> baggagekg += b.weight;
                    }
                }
                for (Cargoes.Cargo car : c.cargo) {
                    switch (car.weightUnit) {
                        case ("lb") -> cargolbs += car.weight;
                        case ("kg") -> cargokg += car.weight;
                    }
                }
            }
        }
        totallbs = baggagelbs + cargolbs;
        totalkg = baggagekg + cargokg;
        cargo_weight.setText("Weight of cargo in lb: " + cargolbs + ". Weight of cargo in kg: " + cargokg);
        cargo_weight.setVisible(true);
        baggage_weight.setText("Weight of baggage in lb: " + baggagelbs + ". Weight of baggage in kg: " + baggagekg);
        baggage_weight.setVisible(true);
        total_weight.setText("Total weight in lb: " + totallbs + ". Total weight in kg: " + totalkg);
        total_weight.setVisible(true);
    }
}