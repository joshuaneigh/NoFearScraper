package com.company.model;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

/**
 * Potentially flexible, but designed and tailored to scrape the DIA NoFEAR web page. There are some formatting
 * inconsistencies which prompt further use cases, but are decoupled to the best of my ability. If the utility does not
 * work as expected, please see archive.org's cache of the Q4 FY 2018 report to ensure that the formatting has not
 * dramatically changed.
 *
 * @version 16 January 2019
 * @author NeighbargerJ
 */
@SuppressWarnings("SameParameterValue")
public final class NoFearScraper {

    /** Name of Table V as printed in the Q4 FY 2018 DIA No-FEAR report */
    private static final String TABLE_V_NAME;
    /** Name of Table VII as printed in the Q4 FY 2018 DIA No-FEAR report */
    private static final String TABLE_VII_NAME;
    /** Name of Table VIII as printed in the Q4 FY 2018 DIA No-FEAR report */
    private static final String TABLE_VIII_NAME;
    /** The String for the HTML table tag */
    private static final String TAG_TABLE;
    /** The String for the HTML table body tag */
    private static final String TAG_TABLE_BODY;
    /** The String for the HTML table data tag */
    private static final String TAG_TABLE_DATA;
    /** The String for the HTML table row tag */
    private static final String TAG_TABLE_ROW;
    /** The literal for the HTML "&nbsp;" character. Defined statically to prevent accidental mis-typing. */
    private static final char TAB_CHAR;

    static {
        TABLE_V_NAME = "V.";
        TABLE_VII_NAME = "VII.";
        TABLE_VIII_NAME = "VIII.";
        TAG_TABLE = "table";
        TAG_TABLE_BODY = "tbody";
        TAG_TABLE_DATA = "td";
        TAG_TABLE_ROW = "tr";
        TAB_CHAR = '\u00a0';
    }

    /**
     * Private constructor to avoid external instantiation of this class
     */
    private NoFearScraper() {}

    /**
     * Deprecated starting 15 January 2019. Scrapes the entirety of the HTML table into a single CSV. Does not consider
     * any sub-tables or any context of what the data means. Intended as a proof-of-concept, not as a usable solution.
     *
     * @param theUrl the URL from which to read the HTML data
     * @return the CSV as a String
     */
    @Deprecated
    public static String scrape(final String theUrl) {
        final Document doc = getWebPage(theUrl);
        final Element table = getRelevantTable(doc);
        return processTable(table);
    }

    /**
     * Dynamically scrapes the No-FEAR data from the DIA web page. Does not consider improper formatting, with some
     * exceptions, including the lack of a null line preceding table 5 and the data not leaving space for headers in
     * tables 7 and 8. See the archived Q4 FY 2018 report for a visual explanation.
     *
     * @param theUrl the URL from which to read the HTML data
     * @param theHtmlKey the key which the HTML source data will be put to for use by the Driver
     * @return a Map with the table name as the Key (i.e.: "III. Issues of Complaints Filed:") and the CSV of that
     *      sub-table in a String. The table cells are wrapped in \" characters and are delimited using \' characters.
     *      The first cell is null in each table, followed by the header of each column. The data follows normally.
     */
    static Map<String, String> scrapeSubTables(final String theUrl, final String theHtmlKey) {
        final Document doc = Objects.requireNonNull(getWebPage(theUrl));
        final Element table = getRelevantTable(doc);
        final Map<String, String> subTableMap = processSubTables(table);
        subTableMap.put(theHtmlKey, doc.toString());
        return subTableMap;
    }

    /**
     * Handles the processing of the table into a CSV file. No consideration of empty rows or sub-tables exists.
     *
     * @param theTable the jSoup table of which to parse
     * @return a CSV version of the table as a String
     */
    private static String processTable(final Element theTable) {
        final StringBuilder csv = new StringBuilder();
        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
        for (final Element row : rows) {
            for (final Element cellData : row.getElementsByTag(TAG_TABLE_DATA)) csv.append('"')
                    .append(thisTrim(cellData.text())).append('"').append(',');
            csv.append('\n');
        }
        return csv.toString();
    }

    /**
     * Handles the processing of the table into a CSV file. Takes sub-tables into consideration and implements some
     * controls for inconsistent formatting problems commonly observed through the current No-FEAR report as of code
     * production and as observed through archive.org's WayBackMachine. May not work on other internet networks aside
     * from the World Wide Web, and may break for future reports of the No-FEAR data.
     *
     * @param theTable the jSoup table of which to parse
     * @return a Map with the table name as the Key (i.e.: "III. Issues of Complaints Filed:") and the CSV of that
     *      sub-table in a String. The table cells are wrapped in \" characters and are delimited using \' characters.
     *      The first cell is null in each table, followed by the header of each column. The data follows normally.
     */
    private static Map<String, String> processSubTables(final Element theTable) {
        final Map<String, String> subTables = new HashMap<>();
        final StringBuilder columnNames = new StringBuilder().append("\"\",");
        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
        final Elements firstRowData = rows.first().getElementsByTag(TAG_TABLE_DATA);

        /* Iterates through first row to find names of columns. Skips the first cell. */
        String tableName = firstRowData.remove(0).text();
        for (final Element columnName : firstRowData)
            columnNames.append('\"').append(thisTrim(columnName.text())).append('\"').append(',');

        /* The main loop within this method. Processes all other data. */
        StringBuilder tableCsv = new StringBuilder();
        boolean newTableFlag = false;
        boolean firstLineOfFileFlag = true;
        for (final Element row : rows) {
            if (firstLineOfFileFlag) firstLineOfFileFlag = false;
            else if (isRowEmpty(row)) newTableFlag = true;
            else if (newTableFlag || isHotFixTableV(row)) {
                subTables.put(tableName, tableCsv.toString());
                tableCsv = new StringBuilder().append(columnNames);
                tableName = thisTrim(row.getElementsByTag(TAG_TABLE_DATA).first().text());
                newTableFlag = false;
                if (isHotFixTableVII(row) || isHotFixTableVIII(row)) {
                    tableCsv.append('\n');
                    for (final Element data : row.getElementsByTag(TAG_TABLE_DATA))
                        tableCsv.append('\"').append(thisTrim(data.text())).append('\"').append(',');
                }
            } else {
                for (final Element data : row.getElementsByTag(TAG_TABLE_DATA))
                    tableCsv.append('\"').append(thisTrim(data.text())).append('\"').append(',');
            }
            tableCsv.append('\n');
        }
        subTables.put(tableName, tableCsv.toString());
        return subTables;
    }

    /**
     * Trims and returns all white space from the passed String on its left and right sides. Intended to keep code
     * cleaner throughout this class, as this method is more verbose than String.trim(), as it also removes the HTML
     * TAB_CHAR, as defined in the static fields of this class.
     *
     * @param text the text to trim as a String
     * @return the trimmed text as a String
     */
    private static String thisTrim(final String text) {
        return text.replace(TAB_CHAR, ' ').trim();
    }

    /**
     * Determines if the passed row is empty (contains no data or headers). Pulled into dedicated method to keep code
     * within main loop clean.
     *
     * @param row the row of which to interpret as a jSoup Element
     * @return boolean if the row is empty
     */
    private static boolean isRowEmpty(Element row) {
        return row.getElementsByTag(TAG_TABLE_DATA).text().replace(TAB_CHAR, ' ').trim().equals("");
    }

    /**
     * Determines if the passed row is the start of Table V. Enters alternate logic within the master loop to handle
     * inconsistent formatting in the No-FEAR web page. May break on future No-FEAR reports. Works as of Q4 FY 2018
     * report, and with nearly every report found through archive.org's WayBackMachine.
     *
     * @param row the row of which to interpret as a jSoup Element
     * @return boolean if the row is the start of Table V.
     */
    private static boolean isHotFixTableV(final Element row) {
        return thisTrim(row.text()).startsWith(TABLE_V_NAME);
    }

    /**
     * Determines if the passed row is the start of Table VII. Enters alternate logic within the master loop to handle
     * inconsistent formatting in the No-FEAR web page. May break on future No-FEAR reports. Works as of Q4 FY 2018
     * report, and with nearly every report found through archive.org's WayBackMachine.
     *
     * @param row the row of which to interpret as a jSoup Element
     * @return boolean if the row is the start of Table VII.
     */
    private static boolean isHotFixTableVII(final Element row) {
        return thisTrim(row.text()).startsWith(TABLE_VII_NAME);
    }

    /**
     * Determines if the passed row is the start of Table VIII. Enters alternate logic within the master loop to handle
     * inconsistent formatting in the No-FEAR web page. May break on future No-FEAR reports. Works as of Q4 FY 2018
     * report, and with nearly every report found through archive.org's WayBackMachine.
     *
     * @param row the row of which to interpret as a jSoup Element
     * @return boolean if the row is the start of Table VIII.
     */
    private static boolean isHotFixTableVIII(final Element row) {
        return thisTrim(row.text()).startsWith(TABLE_VIII_NAME);
    }

    /**
     * Gets and returns the relevant table from the DIA No-FEAR HTML scrape.
     *
     * @param theDoc the jSoup Document from which to pull the table
     * @return the relevant HTML table as a jSoup Element
     */
    private static Element getRelevantTable(final Document theDoc) {
        return Objects.requireNonNull(theDoc)
                .getElementsByTag(TAG_TABLE).first()
                .getElementsByTag(TAG_TABLE_BODY).first();
    }

    /**
     * Queries web page for full HTML data and handles potential exceptions.
     *
     * @return the jSoup Document containing the HTML data
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
