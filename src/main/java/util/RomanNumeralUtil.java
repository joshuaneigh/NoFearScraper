package util;

import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to convert numbers to or from a Roman numeral.
 *
 * @author NeighbargerJ
 * @version 17 January 2019
 */
public final class RomanNumeralUtil {

    private static String PATTERN;
    private static String REGEX;
    private static String ROMAN_1;
    private static String ROMAN_4;
    private static String ROMAN_5;
    private static String ROMAN_9;
    private static String ROMAN_10;
    private static String ROMAN_40;
    private static String ROMAN_50;
    private static String ROMAN_90;
    private static String ROMAN_100;
    private static String ROMAN_400;
    private static String ROMAN_500;
    private static String ROMAN_900;
    private static String ROMAN_1000;
    private static String[] ROMAN_NUMERALS;

    static {
        PATTERN = "M|CM|D|CD|C|XC|L|XL|X|IX|V|IV|I";
        REGEX = "^(M{0,3})(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$";
        ROMAN_1 = "I";
        ROMAN_4 = "IV";
        ROMAN_5 = "V";
        ROMAN_9 = "IX";
        ROMAN_10 = "X";
        ROMAN_40 = "XL";
        ROMAN_50 = "L";
        ROMAN_90 = "XC";
        ROMAN_100 = "C";
        ROMAN_400 = "CD";
        ROMAN_500 = "D";
        ROMAN_900 = "CM";
        ROMAN_1000 = "M";
        ROMAN_NUMERALS = new String[]{ROMAN_1000, ROMAN_900, ROMAN_500, ROMAN_400, ROMAN_100, ROMAN_90, ROMAN_50,
                ROMAN_40, ROMAN_10, ROMAN_9, ROMAN_5, ROMAN_4, ROMAN_1};
    }

    /**
     * Private constructor to avoid external instantiation of this class
     */
    private RomanNumeralUtil() {}

    /**
     * Converts the passed int to a Roman numeral String
     *
     * @param theDecimal the int to convert
     * @return the Roman numeral String
     */
    public static String intToRoman(final int theDecimal) {
        if (theDecimal <= 0)
            throw new IllegalArgumentException("Integer must be positive and non-zero!");
        return String.join("", Collections.nCopies(theDecimal, ROMAN_1))
                .replace("IIIII", ROMAN_5)
                .replace("IIII", ROMAN_4)
                .replace("VV", ROMAN_10)
                .replace("VIV", ROMAN_9)
                .replace("XXXXX", ROMAN_50)
                .replace("XXXX", ROMAN_40)
                .replace("LL", ROMAN_100)
                .replace("LXL", ROMAN_90)
                .replace("CCCCC", ROMAN_500)
                .replace("CCCC", ROMAN_400)
                .replace("DD", ROMAN_1000)
                .replace("DCD", ROMAN_900);
    }

    /**
     * Converts the passed Roman numeral String to an int
     *
     * @param theRoman the Roman numeral String to convert
     * @return the converted int
     */
    public static int romanToInt(final String theRoman) {
        Objects.requireNonNull(theRoman);
        if (theRoman.isEmpty() || !theRoman.matches(REGEX))
            throw new NumberFormatException("Illegal number format. Passed value is not a Roman numeral!");

        final Matcher matcher = Pattern.compile(PATTERN).matcher(theRoman);
        final int[] decimalValues = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        final String[] romanNumerals = ROMAN_NUMERALS;

        int result = 0;
        while (matcher.find())
            for (int i = 0; i < romanNumerals.length; i++)
                if (romanNumerals[i].equals(matcher.group(0)))
                    result += decimalValues[i];

        return result;
    }
}
