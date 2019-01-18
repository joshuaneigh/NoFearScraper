import model.NoFearScraper;

/**
 * Launches the NoFearDriver.
 *
 * @author NeighbargerJ
 * @version 16 January 2019
 */
public class Main {

    private static final String URL = "http://www.dia.mil/No-FEAR/";

    /**
     * The main method
     *
     * @param theArgs the command line arguments. The first argument is the path to the archive folder and the second
     *                argument is the path to the processed folder.
     */
    public static void main(final String[] theArgs) {
        NoFearScraper.scrape(URL, theArgs[0], theArgs[1]);
    }
}
