package com.example.smart4aviation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static class Data {
        public List<Flight> flights;
        public List<Cargoes> cargo;

        public Data() {
            this.flights = null;
            this.cargo = null;
        }
    }
    public static Data fetchData(Data res) throws FileNotFoundException {
        String file = System.getProperty("user.dir") + "/flights.json";
        BufferedReader br = new BufferedReader(new FileReader(file));
        Gson gson = new Gson();
        JsonElement jsonElement = JsonParser.parseReader(br);
        Type type = new TypeToken<ArrayList<Flight>>() {}.getType();
        res.flights = gson.fromJson(jsonElement, type);

        file = System.getProperty("user.dir") + "/cargo.json";
        br = new BufferedReader(new FileReader(file));
        jsonElement = JsonParser.parseReader(br);
        type = new TypeToken<ArrayList<Cargoes>>() {}.getType();
        res.cargo = gson.fromJson(jsonElement, type);

        return res;
    }

}
