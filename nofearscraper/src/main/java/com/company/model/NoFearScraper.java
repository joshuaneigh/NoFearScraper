package com.company.model;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class NoFearScraper {

//    private static final String KEY_COLUMNS_NAMES = "COLUMN_NAMES";
    private static final String TAG_TABLE = "table";
    private static final String TAG_TABLE_BODY = "tbody";
    private static final String TAG_TABLE_DATA = "td";
    private static final String TAG_TABLE_ROW = "tr";

    public static Object scrape(final String theUrl) {
        final Document doc = getWebPage(theUrl);
        final Element table = getTable(doc);
//        final Map<String, List<String>> subTables = getSubTables(table);
        return getSubTablesAsCsv(table);
    }

    private static String getSubTablesAsCsv(final Element theTable) {
        final StringBuilder csv = new StringBuilder();
        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
        for (final Element row : rows) {
            csv.append('"');
            for (final Element cellData : row.getElementsByTag(TAG_TABLE_DATA)) {
                csv.append(cellData.text().trim()).append('"').append(',').append('"');
            }
            csv.append('"').append('\n');
        }
        return csv.toString();
    }

//    private static Map<String, List<String>> getSubTables(final Element theTable) {
//        final Map<String, List<String>> subTables = new HashMap<>();
//        final List<String> columnNames = new ArrayList<>();
//        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
//        final Elements firstRowData = rows.first().getElementsByTag(TAG_TABLE_DATA);
//        String lastKey = firstRowData.remove(0).text();
//
//        /* Iterates through first row to find names of columns. Skips the first cell. */
//        subTables.put(KEY_COLUMNS_NAMES, columnNames);
//        for (final Element columnName : firstRowData) {
//            columnNames.add(columnName.text().trim());
//        }
//
//        List<String> lastList = new ArrayList<>();
//        subTables.put(lastKey, lastList);
//        for (final Element row : rows)
//
//        return subTables;
//    }

    private static Element getTable(final Document theDoc) {
        return Objects.requireNonNull(theDoc)
                .getElementsByTag(TAG_TABLE).first()
                .getElementsByTag(TAG_TABLE_BODY).first();
    }

    /**
     * Queries web page for full HTML data and handles potential exceptions.
     * @return the Document containing the HTML data
     */
    private static Document getWebPage(final String theUrl) {
        try {
            return Jsoup.connect(theUrl).get();
        } catch (final HttpStatusException e) {
            System.err.println(e.getMessage());
            return null;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
