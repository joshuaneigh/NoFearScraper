package com.company;

import com.company.model.NoFearScraper;

import java.util.Map;

public class Main {

    private static final String URL = "http://www.dia.mil/No-FEAR/";

    public static void main(final String[] theArgs) {
        final Map<String, String> tables = NoFearScraper.scrapeSubTables(URL);
        System.out.println(tables.keySet());
        System.out.println(tables.get(tables.keySet().toArray()[9]));
    }
}
