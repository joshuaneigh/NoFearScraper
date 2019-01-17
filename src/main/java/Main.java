package main.java;

import main.java.model.NoFearDriver;

/**
 * Launches the NoFearDriver.
 *
 * @author NeighbargerJ
 * @version 16 January 2019
 */
public class Main {

    /**
     * The main method
     *
     * @param theArgs the command line arguments. The first argument is the path to the archive folder and the second
     *                argument is the path to the processed folder.
     */
    public static void main(final String[] theArgs) {
        NoFearDriver.update(theArgs[0], theArgs[1]);
    }
}
