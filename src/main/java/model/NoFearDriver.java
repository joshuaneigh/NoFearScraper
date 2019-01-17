package main.java.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Driver for the NoFearScraper. Intended to request the No-FEAR data and save each response. These include the original
 * HTML data in the archive directory and the processed CSV(s) in the processed directory.
 *
 * @author NeighbargerJ
 * @version 16 January 2019
 */
public class NoFearDriver {

    private static final String CHARACTERS_ILLEGAL;
    private static final String CHARACTERS_REPLACE_WITH;
    /** The CSV extension declared statically */
    private static final String EXTENSION_CSV;
    /** The HTML extension declared statically */
    private static final String EXTENSION_HTML;
    /** Arbitrary name of the key which corresponds to the HTML source data. Ultimately becomes name of file */
    private static final String KEY_HTML_SOURCE;
    /** The URL to the original HTML report, either hosted on or pulled from the DIA No-FEAR web page */
    private static final String ORIGINAL_REPORT_URL;

    static {
        CHARACTERS_ILLEGAL = "[\\\\/:*?\"<>|\u00a0]";
        CHARACTERS_REPLACE_WITH = "";
        EXTENSION_CSV = ".csv";
        EXTENSION_HTML = ".html";
        KEY_HTML_SOURCE = "No-FEAR-Source";
        ORIGINAL_REPORT_URL = "http://www.dia.mil/No-FEAR/";
    }

    /**
     * Private constructor to avoid external instantiation of this class
     */
    private NoFearDriver() {}

    /**
     * Prompts this driver to generate new CSV and HTML files. CSV(s) are saved in the PATH_PROCESSED and the original
     * HTML document is saved in the PATH_ARCHIVE with intent of maintaining the original and processed data separately.
     * This allows further assurance that systems are working properly, and provides safety if they are not.
     *
     * @param theArchivePath the passed path to the archive folder
     * @param theProcessedPath the passed path to the processed folder
     */
    public static void update(final String theArchivePath, final String theProcessedPath) {
        final Map<String, String> tables = NoFearScraper.scrapeSubTables(ORIGINAL_REPORT_URL, KEY_HTML_SOURCE);
        for (final String key : tables.keySet())
            writeToFile(keyToFileName(key, theArchivePath, theProcessedPath), tables.get(key));
    }

    /**
     * Processes the passed key and converts it into the proper file path, including processing which directory to save
     * the file and what the appropriate extension should be.
     *
     * @param theKey the key to process
     * @param theArchivePath the passed path to the archive folder
     * @param theProcessedPath the passed path to the processed folder
     * @return the String path of the file
     */
    private static String keyToFileName(final String theKey, final String theArchivePath,
                                        final String theProcessedPath) {
        final StringBuilder path = new StringBuilder();
        final String extension;
        if (theKey.equals(KEY_HTML_SOURCE)) {
            path.append(theArchivePath);
            extension = EXTENSION_HTML;
        } else {
            path.append(theProcessedPath);
            extension = EXTENSION_CSV;
        }
        return path.append(theKey.replaceAll(CHARACTERS_ILLEGAL, CHARACTERS_REPLACE_WITH)
                .replace(' ', '_').trim()).append(extension).toString();
    }

    /**
     * Writes the passed data into a file at the passed path and filename.
     *
     * @param theFilePath the path at which the data will be written
     * @param theData the data to write
     */
    private static void writeToFile(final String theFilePath, final String theData) {
        final Path path = Paths.get(theFilePath);
        try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(theData);
        } catch (final FileSystemException e) {
            System.err.println(e.getFile() + e.getMessage());
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
