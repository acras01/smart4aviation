package com.example.smart4aviation;

public class Cargoes {
    public int flightId;
    public Baggage[] baggage;
    public Cargo[] cargo;

    public Cargoes(int id, Baggage[] bag, Cargo[] car) {
        this.flightId = id;
        this.baggage = bag;
        this.cargo = car;
    }

    public static class Baggage {
        public int id;
        public int weight;
        public String weightUnit;
        public int pieces;

        public Baggage(int id, int weight, String units, int pieces) {
            this.id = id;
            this.weight = weight;
            this.weightUnit = units;
            this.pieces = pieces;
        }
    }

    public static class Cargo {
        public int id;
        public int weight;
        public String weightUnit;
        public int pieces;

        public Cargo(int id, int weight, String units, int pieces) {
            this.id = id;
            this.weight = weight;
            this.weightUnit = units;
            this.pieces = pieces;
        }
    }
}
