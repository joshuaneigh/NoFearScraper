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

    private static final String EXTENSION_CSV;
    private static final String EXTENSION_HTML;
    private static final String KEY_HTML;
    private static final String KEY_LOOKUP_DESC;
    private static final String KEY_LOOKUP_REPORT;
    private static final String KEY_REPORT;
    private static final String TAG_TABLE;
    private static final String TAG_TABLE_BODY;
    private static final String TAG_TABLE_DATA;
    private static final String TAG_TABLE_ROW;
    private static final char TAB_CHAR;

    static {
        EXTENSION_CSV = ".csv";
        EXTENSION_HTML = ".html";
        KEY_HTML = "html-source";
        KEY_LOOKUP_DESC = "desc-lookup";
        KEY_LOOKUP_REPORT = "report-lookup";
        KEY_REPORT = "report";
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

    public static void scrape(final String theUrl, final String theArchivePath, final String theProcessedPath) {
        final Document doc = Objects.requireNonNull(getWebPage(theUrl));
        final Element table = getRelevantTable(doc);
        process(table, theProcessedPath);
        writeStringToFile(theArchivePath + KEY_HTML + EXTENSION_HTML, doc.toString());
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

        int reportNumber = 1;
        int descriptionNumber = 1;
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
            writeListArraysToCsv(dataList, outputPath.concat(KEY_REPORT));
            writeListToCsv(reportList, outputPath.concat(KEY_LOOKUP_REPORT));
            writeListToCsv(descList, outputPath.concat(KEY_LOOKUP_DESC));
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
        final CSVWriter writer = new CSVWriter(new FileWriter(new File(thePath.concat(EXTENSION_CSV))));
        for (final String[] entry : theList) writer.writeNext(entry);
        writer.close();
    }

    private static void writeListToCsv(final List<?> theList, final String thePath) throws IOException {
        final CSVWriter writer = new CSVWriter(new FileWriter(new File(thePath.concat(EXTENSION_CSV))));
        final List<String> row = new ArrayList<>();
        for (int i = 0; i < theList.size(); i++) {
            row.add(Integer.toString(i));
            row.add(theList.get(i).toString());
            writer.writeNext(row.toArray(new String[0]));
            row.clear();
        }
        writer.close();
    }

    private static String thisTrim(final String text) {
        return text.replace(TAB_CHAR, ' ').trim();
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
            return "";
        } catch (final NumberFormatException e) {
            if (Character.isDigit(thisTrim(theText).charAt(0)))
                return Character.toString(thisTrim(theText).charAt(0));
            else
                return "";
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
