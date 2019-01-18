package model;

import com.opencsv.CSVWriter;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import util.RomanNumeralUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Potentially flexible, but designed and tailored to scrape the DIA NoFEAR web page. If the utility does not work as
 * expected, please see archive.org's cache of the Q4 FY 2018 report to ensure that the formatting has not
 * dramatically changed.
 *
 * @author NeighbargerJ
 * @version 18 January 2019
 */
public final class NoFearScraper {

    /** String of illegal (non-ASCII) characters as a REGEX */
    private static final String CHARACTERS_ILLEGAL;
    /** A null string. Used to replace the CHARACTERS_ILLEGAL with a String.replaceAll method call */
    private static final String CHARACTERS_REPLACE_WITH;
    /** The filename of the output HTML file */
    private static final String FILENAME_HTML;
    /** The filename of the output lookup-description table file */
    private static final String FILENAME_LOOKUP_DESC;
    /** The filename of the output report table file */
    private static final String FILENAME_LOOKUP_REPORT;
    /** The filename of the output lookup-report table file */
    private static final String FILENAME_REPORT;
    /** The String literal tag of HTML table */
    private static final String TAG_TABLE;
    /** The String literal tag of HTML table body */
    private static final String TAG_TABLE_BODY;
    /** The String literal tag of HTML table data */
    private static final String TAG_TABLE_DATA;
    /** The String literal tag of HTML table row */
    private static final String TAG_TABLE_ROW;

    static {
        CHARACTERS_ILLEGAL = "[^\\\\u0000-\\\\uFFFF]";
        CHARACTERS_REPLACE_WITH = "";
        FILENAME_HTML = "html-source.html";
        FILENAME_LOOKUP_DESC = "desc-lookup.csv";
        FILENAME_LOOKUP_REPORT = "report-lookup.csv";
        FILENAME_REPORT = "report.csv";
        TAG_TABLE = "table";
        TAG_TABLE_BODY = "tbody";
        TAG_TABLE_DATA = "td";
        TAG_TABLE_ROW = "tr";
    }

    /**
     * Private constructor to avoid external instantiation of this class
     */
    private NoFearScraper() {}

    /**
     * Dynamically scrapes the No-FEAR data from the DIA web page. If the utility does not work as expected, please see
     * archive.org's cache of the Q4 FY 2018 report to ensure that the formatting has not dramatically changed. Writes
     * each output table to the passed paths.
     *
     * @param theUrl the URL from which to read the HTML data
     * @param theArchivePath the String path to which the HTML file is saved
     * @param theProcessedPath the String path to which the CSV files are saved
     */
    public static void scrape(final String theUrl, final String theArchivePath, final String theProcessedPath) {
        final Document doc = Objects.requireNonNull(getWebPage(theUrl));
        final Element table = getRelevantTable(doc);
        process(table, theProcessedPath);
        writeStringToFile(theArchivePath + FILENAME_HTML, doc.toString());
    }

    /**
     * Parses the data into their corresponding tables. The primary table is saved in FILENAME_REPORT, which is
     * normalized to maximize utility and flexibility. The names of the report and descriptions of each data metric are
     * saved in the FILENAME_LOOKUP_REPORT and FILENAME_LOOKUP_DESC respectively.
     *
     * @param theTable the HTML table as a jSoup Element
     * @param outputPath the path to which the CSV files will be saved
     */
    private static void process(final Element theTable, final String outputPath) {
        final Elements rows = theTable.getElementsByTag(TAG_TABLE_ROW);
        final List<String[]> dataList = new ArrayList<>();
        final List<String> reportList = new ArrayList<>();
        final List<String> descList = new ArrayList<>();
        final List<String> headers = new ArrayList<>();

        for (int i = 0; i < rows.first().getElementsByTag(TAG_TABLE_DATA).size(); i++) {
            if (i > 0) headers.add(rows.first().getElementsByTag(TAG_TABLE_DATA).get(i).text());
        }

        int reportNumber = 0;
        int descriptionNumber = 0;
        for (final Element row : rows) {

            if (thisTrim(row.getElementsByTag(TAG_TABLE_DATA).first().text())
                    .startsWith(RomanNumeralUtil.intToRoman(reportNumber + 1))) {
                reportNumber++;
                reportList.add(row.getElementsByTag(TAG_TABLE_DATA).first().text());
            } else if (!thisTrim(row.getElementsByTag(TAG_TABLE_DATA).first().text()).isEmpty()) {
                descriptionNumber++;
                descList.add(row.getElementsByTag(TAG_TABLE_DATA).first().text());
            }
            for (int i = 0; i < row.getElementsByTag(TAG_TABLE_DATA).size(); i++) {
                if (i > 0) { /* The first column never holds data to be inserted into this table. */
                    final Element cell = row.getElementsByTag(TAG_TABLE_DATA).get(i);
                    if (!thisTrim(cell.text()).isEmpty()) {
                        if (isDate(cell.text())) break;
                        dataList.add(new String[]{String.valueOf(reportNumber), String.valueOf(descriptionNumber),
                                getYear(headers.get(i - 1)), getQuarter(headers.get(i - 1)), thisTrim(cell.text())});
                    }
                }
            }
        }

        try {
            writeListArraysToCsv(dataList, outputPath.concat(FILENAME_REPORT));
            writeListToCsv(reportList, outputPath.concat(FILENAME_LOOKUP_REPORT));
            writeListToCsv(descList, outputPath.concat(FILENAME_LOOKUP_DESC));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns if the passed string represents a year or a year and quarter.
     *
     * @param theText the text to parse
     * @return if the passed string is a date
     */
    private static boolean isDate(final String theText) {
        try {
            final int a = Integer.parseInt(thisTrim(theText));
            return a >= 2000;
        } catch (final NumberFormatException e) {
            return true;
        }
    }

    /**
     * Writes a single string to the specified file.
     *
     * @param theFilePath the path to which the file is written
     * @param theData the data in the file
     */
    private static void writeStringToFile(final String theFilePath, final String theData) {
        final Path path = Paths.get(theFilePath);
        try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(theData);
        } catch (final FileSystemException e) {
            System.err.println(e.getFile() + e.getMessage());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes the passed List of String[] to the passed file.
     *
     * @param theList the list to write
     * @param thePath the path to which the file is written
     * @throws IOException if the directory does not exist or if the file is not writable
     */
    private static void writeListArraysToCsv(final List<String[]> theList, final String thePath) throws IOException {
        final CSVWriter writer = new CSVWriter(new FileWriter(new File(thePath)));
        for (final String[] entry : theList) writer.writeNext(entry);
        writer.close();
    }

    /**
     * Writes a List of generic items to the passed file.
     *
     * @param theList the list to write
     * @param thePath the path to which the file is written
     * @throws IOException if the directory does not exist or if the file is not writable
     */
    private static void writeListToCsv(final List<?> theList, final String thePath) throws IOException {
        final CSVWriter writer = new CSVWriter(new FileWriter(new File(thePath)));
        final List<String> row = new ArrayList<>();
        for (int i = 0; i < theList.size(); i++) {
            row.add(Integer.toString(i+1));
            row.add(theList.get(i).toString());
            writer.writeNext(row.toArray(new String[0]));
            row.clear();
        }
        writer.close();
    }

    /**
     * Trims and returns all white space from the passed String on its left and right sides. Intended to keep code
     * cleaner throughout this class, as this method is more verbose than String.trim().
     *
     * @param text the text to trim as a String
     * @return the trimmed text as a String
     */
    private static String thisTrim(final String text) {
        return text.replaceAll(CHARACTERS_ILLEGAL, CHARACTERS_REPLACE_WITH).trim();
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

    /**
     * Returns the quarter from the passed date. A null String if no quarter is found.
     *
     * @param theText the text to parse
     * @return the quarter if found or a null String
     */
    private static String getQuarter(final String theText) {
        try {
            Integer.parseInt(thisTrim(theText));
            return CHARACTERS_REPLACE_WITH;
        } catch (final NumberFormatException e) {
            if (Character.isDigit(thisTrim(theText).charAt(0)))
                return Character.toString(thisTrim(theText).charAt(0));
            else
                return CHARACTERS_REPLACE_WITH;
        }
    }

    /**
     * Returns the year from the passed date.
     *
     * @param theText the text to parse
     * @return the year as a String
     */
    private static String getYear(final String theText) {
        try {
            final int i = Integer.parseInt(thisTrim(theText));
            return String.valueOf(i);
        } catch (final NumberFormatException e) {
            return thisTrim(theText).substring(thisTrim(theText).length() - 4);
        }
    }
}
