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

public final class NoFearScraper {

    private static final String CHARACTERS_ILLEGAL;
    private static final String CHARACTERS_REPLACE_WITH;
    private static final String FILENAME_HTML;
    private static final String FILENAME_LOOKUP_DESC;
    private static final String FILENAME_LOOKUP_REPORT;
    private static final String FILENAME_REPORT;
    private static final String TAG_TABLE;
    private static final String TAG_TABLE_BODY;
    private static final String TAG_TABLE_DATA;
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

    public static void scrape(final String theUrl, final String theArchivePath, final String theProcessedPath) {
        final Document doc = Objects.requireNonNull(getWebPage(theUrl));
        final Element table = getRelevantTable(doc);
        process(table, theProcessedPath);
        writeStringToFile(theArchivePath + FILENAME_HTML, doc.toString());
    }

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

    private static boolean isDate(final String text) {
        try {
            final int a = Integer.parseInt(thisTrim(text));
            return a >= 2000;
        } catch (final NumberFormatException e) {
            return true;
        }
    }

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

    private static void writeListArraysToCsv(final List<String[]> theList, final String thePath) throws IOException {
        final CSVWriter writer = new CSVWriter(new FileWriter(new File(thePath)));
        for (final String[] entry : theList) writer.writeNext(entry);
        writer.close();
    }

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

    private static String thisTrim(final String text) {
        return text.replaceAll(CHARACTERS_ILLEGAL, CHARACTERS_REPLACE_WITH).trim();
    }

    private static Element getRelevantTable(final Document theDoc) {
        return Objects.requireNonNull(theDoc)
                .getElementsByTag(TAG_TABLE).first()
                .getElementsByTag(TAG_TABLE_BODY).first();
    }

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

    private static String getYear(final String theText) {
        try {
            final int i = Integer.parseInt(thisTrim(theText));
            return String.valueOf(i);
        } catch (final NumberFormatException e) {
            return thisTrim(theText).substring(thisTrim(theText).length() - 4);
        }
    }
}
