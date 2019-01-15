package com.company;

import com.company.model.NoFearScraper;

import java.util.Objects;

public class Main {

    private static final String URL = "http://www.dia.mil/No-FEAR/";

    public static void main(final String[] theArgs) {
        System.out.println(NoFearScraper.scrape(URL));
    }
}
