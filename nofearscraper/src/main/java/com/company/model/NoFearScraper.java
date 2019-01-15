package com.company.model;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class NoFearScraper {

    private static final String KEY_COLUMNS_NAMES = "COLUMN_NAMES";
    private static final String TAG_TABLE = "table";
    private static final String TAG_TABLE_BODY = "tbody";
    private static final String TAG_TABLE_DATA = "td";
    private static final String TAG_TABLE_ROW = "tr";

//    @Deprecated
    public static String scrape(final String theUrl) {
        final Document doc = getWebPage(theUrl);
        final Element table = getTable(doc);
        return getSubTablesAsCsv(table);
    }


    public static Map<String, String> scrapeSubTables(final String theUrl) {
        final Document doc = getWebPage(theUrl);
        final Element table = getTable(doc);
        final Map<String, String> subTables = getSubTables(table);
        return subTables;
    }

    /**
     * Returns the entirety of the table as a CSV. No consideration of empty rows or subtables exists.
     * @param theTable the jSoup table of which to parse
     * @return a CSV version of the table as a String
     */
    private static String getSubTablesAsCsv(final Element theTable) {
        final StringBuilder csv = new StringBuilder();
        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
        for (final Element row : rows) {
            for (final Element cellData : row.getElementsByTag(TAG_TABLE_DATA)) csv.append('"')
                    .append(cellData.text().replace('\u00a0',' ').trim()).append('"').append(',');
            csv.append('\n');
        }
        return csv.toString();
    }

    // TODO: Does not work if the tables don't have a null line between each table
    // TODO: Table 4 has a row without data as well. How to interpret? Also, table 5 is not preceded with a new line
    // TODO: Table 7 and 8 do not have a blank row for header data. Wtf people. Did you proofread your report?
    private static Map<String, String> getSubTables(final Element theTable) {
        final Map<String, String> subTables = new HashMap<>();
        final StringBuilder columnNames = new StringBuilder().append("\"\",");
        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
        final Elements firstRowData = rows.first().getElementsByTag(TAG_TABLE_DATA);
        String tableName = firstRowData.remove(0).text();

        /* Iterates through first row to find names of columns. Skips the first cell. */
        for (final Element columnName : firstRowData) columnNames.append('\"')
                .append(columnName.text().replace('\u00a0',' ').trim()).append('\"').append(',');
        subTables.put(KEY_COLUMNS_NAMES, columnNames.toString());

        StringBuilder tableCsv = new StringBuilder();
        boolean newTableFlag = false;
        for (final Element row : rows) {
            if (row.getElementsByTag(TAG_TABLE_DATA).text().replace('\u00a0', ' ').trim().equals("")) {
                /* If empty row, set flag so that the next row is handled by the new subtable logic */
                newTableFlag = true;
            } else if (newTableFlag) {
                subTables.put(tableName, tableCsv.toString());
                tableCsv = new StringBuilder().append(columnNames.toString());
                tableName = row.getElementsByTag(TAG_TABLE_DATA).first().text().replace('\u00a0', ' ').trim();
                newTableFlag = false;
            } else {
                for (final Element data : row.getElementsByTag(TAG_TABLE_DATA)) tableCsv.append('\"')
                        .append(data.text().replace('\u00a0', ' ').trim()).append('\"').append(',');
            }
            tableCsv.append('\n');
        }
        subTables.put(tableName, tableCsv.toString());
        return subTables;
    }

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
