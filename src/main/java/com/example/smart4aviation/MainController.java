package com.example.smart4aviation;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    List<Flight> flights = new ArrayList<>();
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
            List<Integer> flist = flights.stream()
                    .map(Flight::getFlightNumber)
                    .toList();
            flight_list.setItems(FXCollections.observableList(flist));

            List<String> alist = new ArrayList<>(flights.stream()
                    .map(Flight::getArrivalIATA)
                    .toList());
            alist.addAll(flights.stream()
                    .map(Flight::getDepartureIATA)
                    .toList());
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
                    List<LocalDate> dates = flights.stream()
                            .filter(flight -> new_value.equals(flight.arrivalAirportIATACode) ||
                                    new_value.equals(flight.departureAirportIATACode))
                            .map(flight -> LocalDate.parse(flight.departureDate, ISO_OFFSET_DATE_TIME))
                            .collect(Collectors.toList());
                    restrictDatePicker(iata_date, dates);
                    iata_date.setValue(dates.get(0));
                    iata_date.setDisable(false);
                });

        flight_list.getSelectionModel().selectedIndexProperty()
                .addListener((ov, value, new_value) -> {
                    cargo_weight.setVisible(false);
                    baggage_weight.setVisible(false);
                    total_weight.setVisible(false);
                    List<LocalDate> dates = flights.stream()
                            .filter(flight -> new_value.intValue() == flight.flightId)
                            .map(flight -> LocalDate.parse(flight.departureDate, ISO_OFFSET_DATE_TIME))
                            .collect(Collectors.toList());
                    restrictDatePicker(flight_date, dates);
                    flight_date.setValue(dates.get(0));
                    flight_date.setDisable(false);
                });

        iata_date.valueProperty().addListener((ov, oldDate, newDate) -> fetch_iata.setDisable(false));

        flight_date.valueProperty().addListener((ov, oldDate, newDate) -> fetch_flight.setDisable(false));
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
        List <Flight> flightsToDate = flights.stream()
                .filter(f -> LocalDate.parse(f.departureDate, ISO_OFFSET_DATE_TIME)
                        .format(ISO_DATE).equals(date))
                .toList();
        long arr_flights = flightsToDate.stream()
                .filter(f -> f.arrivalAirportIATACode.equals(iata))
                .count();
        long dep_flights = flightsToDate.stream()
                .filter(f -> f.departureAirportIATACode.equals(iata))
                .count();
        for (Flight f : flightsToDate) {
            List<Cargoes> cargo_flight = cargo.stream()
                    .filter(c -> c.flightId == f.flightId)
                    .toList();
            if (f.arrivalAirportIATACode.equals(iata)) {
                arr_weight_pieces += sumWeightPieces.apply(cargo_flight);
            } else if (f.departureAirportIATACode.equals(iata)) {
                dep_weight_pieces += sumWeightPieces.apply(cargo_flight);
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

    @FXML
    public void onFlightButtonClick(ActionEvent actionEvent) {
        int flight = flight_list.getSelectionModel().getSelectedItem();
        String date = flight_date.getValue().format(ISO_DATE);
        int[] cargoAndBaggage = {0, 0, 0, 0};
        List<Flight> flightsToDate = flights
                .stream()
                .filter(f -> LocalDate.parse(f.departureDate, ISO_OFFSET_DATE_TIME)
                        .format(ISO_DATE).equals(date))
                .filter(f -> f.flightNumber == flight)
                .toList();
        flightsToDate.forEach( (f) -> {
            List<Cargoes> cargo_flight = cargo.stream()
                    .filter(c -> c.flightId == f.flightId)
                    .toList();
            cargoAndBaggage[0] += sumWeightBaggageUnit.apply(cargo_flight, "lb");
            cargoAndBaggage[1] += sumWeightBaggageUnit.apply(cargo_flight, "kg");
            cargoAndBaggage[2] += sumWeightCargoUnit.apply(cargo_flight, "lb");
            cargoAndBaggage[3] += sumWeightCargoUnit.apply(cargo_flight, "kg");
        });
        int totallbs = cargoAndBaggage[0] + cargoAndBaggage[2];
        int totalkg = cargoAndBaggage[1] + cargoAndBaggage[3];
        cargo_weight.setText("Weight of cargo in lb: " + cargoAndBaggage[2] + ". Weight of cargo in kg: " + cargoAndBaggage[3]);
        cargo_weight.setVisible(true);
        baggage_weight.setText("Weight of baggage in lb: " + cargoAndBaggage[0] + ". Weight of baggage in kg: " + cargoAndBaggage[1]);
        baggage_weight.setVisible(true);
        total_weight.setText("Total weight in lb: " + totallbs + ". Total weight in kg: " + totalkg);
        total_weight.setVisible(true);
    }

    Function<List<Cargoes>, Integer> sumWeightPieces = (cargoes ->
            cargoes.stream()
            .map(cargo -> cargo.baggage)
            .map(baggages -> Arrays.stream(baggages).
                    map(baggage -> baggage.pieces)
                    .reduce(Integer::sum)
                    .orElse(0))
            .reduce(Integer::sum)
            .orElse(0));

    BiFunction<List<Cargoes>, String, Integer> sumWeightBaggageUnit = ((cargoes, unit) ->
            cargoes.stream()
                    .map(cargo -> cargo.baggage)
                    .map(baggages -> Arrays.stream(baggages)
                            .filter(baggage -> baggage.weightUnit.equals(unit))
                            .map(baggage -> baggage.weight)
                            .reduce(Integer::sum)
                            .orElse(0))
                    .reduce(Integer::sum)
                    .orElse(0));

    BiFunction<List<Cargoes>, String, Integer> sumWeightCargoUnit = ((cargoes, unit) ->
            cargoes.stream()
                    .map(cargo -> cargo.cargo)
                    .map(baggages -> Arrays.stream(baggages)
                            .filter(baggage -> baggage.weightUnit.equals(unit))
                            .map(baggage -> baggage.weight)
                            .reduce(Integer::sum)
                            .orElse(0))
                    .reduce(Integer::sum)
                    .orElse(0));
}