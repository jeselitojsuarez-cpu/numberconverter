package com.josh.numberconverter;

import java.util.Scanner;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class NumberConversion {

    // Maximum digits shown when a fractional conversion repeats forever.
    static final int MAX_FRACTION_DIGITS = 32;

    // Prevent accidental gigantic calculations such as 2^999999999.
    static final int MAX_EXPONENT_ABS = 10000;

    static class Fraction {
        BigInteger numerator;
        BigInteger denominator;

        Fraction(BigInteger numerator, BigInteger denominator) {
            if (denominator.equals(BigInteger.ZERO)) {
                throw new ArithmeticException("Denominator cannot be zero.");
            }

            if (denominator.signum() < 0) {
                numerator = numerator.negate();
                denominator = denominator.negate();
            }

            BigInteger gcd = numerator.gcd(denominator);
            this.numerator = numerator.divide(gcd);
            this.denominator = denominator.divide(gcd);
        }

        Fraction abs() {
            return new Fraction(numerator.abs(), denominator);
        }

        boolean isNegative() {
            return numerator.signum() < 0;
        }

        Fraction pow(int exponent) {
            if (exponent == 0) {
                return new Fraction(BigInteger.ONE, BigInteger.ONE);
            }

            int power = Math.abs(exponent);

            if (exponent > 0) {
                return new Fraction(
                    numerator.pow(power),
                    denominator.pow(power)
                );
            }

            if (numerator.equals(BigInteger.ZERO)) {
                throw new ArithmeticException("0 cannot be raised to a negative exponent.");
            }

            return new Fraction(
                denominator.pow(power),
                numerator.pow(power)
            );
        }
    }

    static class ParsedInput {
        String expressionText;   // number/expression after removing subscript base
        Integer annotatedBase;   // null when there is no subscript base

        ParsedInput(String expressionText, Integer annotatedBase) {
            this.expressionText = expressionText;
            this.annotatedBase = annotatedBase;
        }
    }

    static class ParsedExpression {
        String numberText;       // the base number before exponent
        boolean hasExponent;
        int exponent;
        String exponentDisplay;
        String error;

        ParsedExpression(String numberText) {
            this.numberText = numberText;
            this.hasExponent = false;
            this.exponent = 1;
            this.exponentDisplay = "";
            this.error = null;
        }
    }

    public static void main(String[] args) {

        Scanner scn = new Scanner(System.in);

        System.out.println("======================================");
        System.out.println("       NUMBER SYSTEM CONVERTER");
        System.out.println("======================================");

        System.out.print("Enter any number: ");
        String rawInput = scn.nextLine().trim().toUpperCase();

        if (rawInput.isEmpty()) {
            System.out.println("\nERROR: Number cannot be empty.");
            scn.close();
            return;
        }

        // First read a REAL base suffix using SUBSCRIPT notation only:
        // 101101₂, 77₈, 450₁₀, A3.F₁₆
        ParsedInput parsedInput = parseOptionalBaseSuffix(rawInput);

        // Then read an exponent, for example 2³, 5², 10⁴ or 2^3.
        ParsedExpression expression = parseExponentExpression(parsedInput.expressionText);

        if (expression.error != null) {
            System.out.println();
            System.out.println("ERROR: " + expression.error);
            System.out.println("Examples supported: 2³, 5², 10⁴, 2^3");
            System.out.println("For a number-system base, use SUBSCRIPT: 101101₂, 77₈, 1C2₁₆");
            scn.close();
            return;
        }

        System.out.print("Current base number (2, 8, 10, 16): ");

        if (!scn.hasNextInt()) {
            System.out.println("\nERROR: Base must be 2, 8, 10, or 16.");
            scn.close();
            return;
        }

        int baseNumber = scn.nextInt();
        scn.nextLine();

        if (!validBase(baseNumber)) {
            System.out.println();
            System.out.println("ERROR: Only Base 2, 8, 10, and 16 are supported.");
            scn.close();
            return;
        }

        System.out.println();
        System.out.print("Is there a conversion? (Y/N): ");
        String choice = scn.nextLine().trim().toUpperCase();

        // =====================================================
        // N = Same behavior you requested:
        // - Without a subscript base: entered expression is Decimal,
        //   and Current base number is the TARGET base.
        //   Example: 2³, base 2, N => 8 decimal -> 1000 binary.
        //
        // - With a subscript base: calculate that number/expression
        //   into Decimal.
        //   Example: 10³₂, base 10, N => (10₂)^3 = 8 decimal.
        // =====================================================
        if (choice.equals("N")) {

            int sourceBase;
            int targetBase;

            if (parsedInput.annotatedBase != null) {
                sourceBase = parsedInput.annotatedBase;
                targetBase = 10;
            } else {
                sourceBase = 10;
                targetBase = baseNumber;
            }

            if (!validBase(sourceBase)) {
                System.out.println("\nERROR: The subscript base must be 2, 8, 10, or 16.");
                scn.close();
                return;
            }

            if (!validNumber(expression.numberText, sourceBase)) {
                printInvalidNumber(expression.numberText, sourceBase);
                scn.close();
                return;
            }

            performConversion(expression, rawInput, sourceBase, targetBase);
            scn.close();
            return;
        }

        // =====================================================
        // Y = User explicitly chooses the conversion.
        // =====================================================
        if (choice.equals("Y")) {

            System.out.println();
            System.out.println("======================================");
            System.out.println("CONVERSION TYPES");
            System.out.println("======================================");
            System.out.println("B = Binary");
            System.out.println("O = Octal");
            System.out.println("D = Decimal");
            System.out.println("H = Hexadecimal");
            System.out.println();
            System.out.println("Examples: B -> O, O -> B, H -> D, D -> H");
            System.out.println();

            System.out.print("Enter conversion: ");
            String conversion = scn.nextLine()
                    .trim()
                    .toUpperCase()
                    .replace(" ", "");

            if (!conversion.matches("[BODH]->[BODH]")) {
                System.out.println();
                System.out.println("ERROR: Invalid conversion format.");
                System.out.println("Example: H -> D");
                scn.close();
                return;
            }

            int sourceBase = letterToBase(conversion.charAt(0));
            int targetBase = letterToBase(conversion.charAt(3));

            if (parsedInput.annotatedBase != null
                    && parsedInput.annotatedBase != sourceBase) {
                System.out.println();
                System.out.println("ERROR: The subscript base written on the number does not match the conversion.");
                System.out.println("Number says Base " + parsedInput.annotatedBase
                        + ", but your conversion starts from Base " + sourceBase + ".");
                scn.close();
                return;
            }

            if (!validNumber(expression.numberText, sourceBase)) {
                printInvalidNumber(expression.numberText, sourceBase);
                scn.close();
                return;
            }

            performConversion(expression, rawInput, sourceBase, targetBase);
            scn.close();
            return;
        }

        System.out.println();
        System.out.println("ERROR: Please enter Y or N.");
        scn.close();
    }

    // =====================================================
    // MAIN CONVERSION FLOW
    // =====================================================
    public static void performConversion(
            ParsedExpression expression,
            String originalInput,
            int sourceBase,
            int targetBase) {

        System.out.println();
        System.out.println("======================================");
        System.out.println("CONVERSION");
        System.out.println("======================================");
        System.out.println("Input: " + originalInput);
        System.out.println("Source base: " + sourceBase);
        System.out.println("Target base: " + targetBase);

        // Parse the number before the exponent exactly.
        Fraction baseValue = parseToFraction(expression.numberText, sourceBase);

        // Show source-base place values if source is not decimal.
        if (sourceBase != 10) {
            showSourceToDecimalSteps(expression.numberText, sourceBase, baseValue);
        }

        Fraction value = baseValue;

        // Evaluate exponent if one was entered.
        if (expression.hasExponent) {
            System.out.println();
            System.out.println("EXPONENT CALCULATION");
            System.out.println("--------------------------------------");
            System.out.println("Expression: " + expression.numberText
                    + " ^ " + expression.exponent);

            try {
                value = baseValue.pow(expression.exponent);
            } catch (ArithmeticException e) {
                System.out.println("ERROR: " + e.getMessage());
                return;
            }

            System.out.println("Result in decimal: "
                    + fractionToBaseString(value, 10, false));
        }

        String answer;

        if (sourceBase == targetBase && !expression.hasExponent) {
            answer = expression.numberText;
        } else if (targetBase == 10) {
            answer = fractionToBaseString(value, 10, false);
            System.out.println();
            System.out.println("Decimal value: " + answer);
        } else {
            answer = fractionToBaseString(value, targetBase, true);
        }

        System.out.println();
        printFinalAnswer(originalInput, sourceBase, answer, targetBase);
    }

    // =====================================================
    // BASE SUFFIX SUPPORT
    // IMPORTANT:
    // SUBSCRIPT means number-system base.
    // Examples: 101101₂, 77₈, 450₁₀, 1C2₁₆
    //
    // Superscripts are NOT bases here; they are exponents.
    // Example: 2³ means 2^3.
    // =====================================================
    public static ParsedInput parseOptionalBaseSuffix(String raw) {

        String text = raw.trim().toUpperCase();
        Integer base = null;

        String[][] suffixes = {
            {"₁₆", "16"},
            {"₁₀", "10"},
            {"₂", "2"},
            {"₈", "8"}
        };

        for (String[] entry : suffixes) {
            if (text.endsWith(entry[0])) {
                base = Integer.parseInt(entry[1]);
                text = text.substring(0, text.length() - entry[0].length()).trim();
                break;
            }
        }

        // Allow notation such as (101101)₂
        if (text.startsWith("(") && text.endsWith(")") && text.length() >= 2) {
            text = text.substring(1, text.length() - 1).trim();
        }

        return new ParsedInput(text, base);
    }

    // =====================================================
    // EXPONENT SUPPORT
    // Supports:
    // 2³, 5², 10⁴, 2⁻³
    // 2^3, 5^2, 10^4, 2^-3
    //
    // Exponent must be an integer.
    // =====================================================
    public static ParsedExpression parseExponentExpression(String text) {

        ParsedExpression result = new ParsedExpression(text.trim());
        String s = text.trim();

        if (s.isEmpty()) {
            result.error = "Number cannot be empty.";
            return result;
        }

        // ---------- ASCII caret notation, e.g. 2^3 ----------
        int firstCaret = s.indexOf('^');

        if (firstCaret >= 0) {
            if (firstCaret != s.lastIndexOf('^')) {
                result.error = "Only one exponent operator (^) is allowed.";
                return result;
            }

            String left = s.substring(0, firstCaret).trim();
            String right = s.substring(firstCaret + 1).trim();

            if (left.isEmpty() || right.isEmpty()) {
                result.error = "Invalid exponent expression. Example: 2^3";
                return result;
            }

            if (!right.matches("[+-]?\\d+")) {
                result.error = "Exponent must be a whole integer. Example: 2^3 or 2^-3";
                return result;
            }

            Integer exponent = parseSafeExponent(right);
            if (exponent == null) {
                result.error = "Exponent is too large. Maximum absolute exponent is "
                        + MAX_EXPONENT_ABS + ".";
                return result;
            }

            // Superscript characters cannot also appear elsewhere.
            if (containsSuperscript(s)) {
                result.error = "Do not mix ^ notation and superscript notation in one expression.";
                return result;
            }

            result.numberText = stripOuterParentheses(left);
            result.hasExponent = true;
            result.exponent = exponent;
            result.exponentDisplay = right;
            return result;
        }

        // ---------- Unicode superscript notation, e.g. 2³ ----------
        int start = findSuperscriptSuffixStart(s);

        if (start >= 0) {
            String left = s.substring(0, start).trim();
            String superscriptPart = s.substring(start);

            if (left.isEmpty()) {
                result.error = "Missing number before the exponent.";
                return result;
            }

            String normalExponent = superscriptToNormal(superscriptPart);

            if (normalExponent == null || normalExponent.equals("+") || normalExponent.equals("-")) {
                result.error = "Invalid superscript exponent.";
                return result;
            }

            Integer exponent = parseSafeExponent(normalExponent);
            if (exponent == null) {
                result.error = "Exponent is too large. Maximum absolute exponent is "
                        + MAX_EXPONENT_ABS + ".";
                return result;
            }

            // If there is another superscript earlier in the base number,
            // then it is embedded in the middle, e.g. 34²02.
            if (containsSuperscript(left)) {
                result.error = "Superscript exponent must be at the END of the number. Example: 2³";
                return result;
            }

            result.numberText = stripOuterParentheses(left);
            result.hasExponent = true;
            result.exponent = exponent;
            result.exponentDisplay = superscriptPart;
            return result;
        }

        // No exponent suffix. If superscript is still somewhere inside,
        // it is malformed, such as 34²02.
        if (containsSuperscript(s)) {
            result.error = "Superscript exponent must be at the END of the number. Example: 2³";
            return result;
        }

        result.numberText = stripOuterParentheses(s);
        return result;
    }

    public static Integer parseSafeExponent(String text) {
        try {
            long value = Long.parseLong(text);
            if (Math.abs(value) > MAX_EXPONENT_ABS) {
                return null;
            }
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int findSuperscriptSuffixStart(String s) {
        int i = s.length() - 1;

        if (i < 0 || !isSuperscriptDigit(s.charAt(i))) {
            return -1;
        }

        while (i >= 0 && isSuperscriptDigit(s.charAt(i))) {
            i--;
        }

        // Optional superscript + or - directly before digits.
        if (i >= 0 && (s.charAt(i) == '⁺' || s.charAt(i) == '⁻')) {
            i--;
        }

        return i + 1;
    }

    public static boolean isSuperscriptDigit(char ch) {
        return ch == '⁰' || ch == '¹' || ch == '²' || ch == '³'
            || ch == '⁴' || ch == '⁵' || ch == '⁶' || ch == '⁷'
            || ch == '⁸' || ch == '⁹';
    }

    public static boolean containsSuperscript(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (isSuperscriptDigit(ch) || ch == '⁺' || ch == '⁻') {
                return true;
            }
        }
        return false;
    }

    public static String superscriptToNormal(String superscript) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < superscript.length(); i++) {
            char ch = superscript.charAt(i);

            switch (ch) {
                case '⁰': out.append('0'); break;
                case '¹': out.append('1'); break;
                case '²': out.append('2'); break;
                case '³': out.append('3'); break;
                case '⁴': out.append('4'); break;
                case '⁵': out.append('5'); break;
                case '⁶': out.append('6'); break;
                case '⁷': out.append('7'); break;
                case '⁸': out.append('8'); break;
                case '⁹': out.append('9'); break;
                case '⁺': out.append('+'); break;
                case '⁻': out.append('-'); break;
                default: return null;
            }
        }

        return out.toString();
    }

    public static String stripOuterParentheses(String text) {
        String s = text.trim();
        if (s.startsWith("(") && s.endsWith(")") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    // =====================================================
    // VALID BASE
    // =====================================================
    public static boolean validBase(int base) {
        return base == 2 || base == 8 || base == 10 || base == 16;
    }

    // =====================================================
    // LETTER -> BASE
    // =====================================================
    public static int letterToBase(char letter) {
        switch (letter) {
            case 'B': return 2;
            case 'O': return 8;
            case 'D': return 10;
            case 'H': return 16;
            default: return -1;
        }
    }

    // =====================================================
    // VALIDATE WHOLE OR FRACTIONAL NUMBER
    // Examples accepted:
    // 110101.11, 239.304, A3.F, .101, 45.
    // =====================================================
    public static boolean validNumber(String number, int base) {

        if (number == null || number.isEmpty()) {
            return false;
        }

        int start = 0;
        if (number.charAt(0) == '+' || number.charAt(0) == '-') {
            start = 1;
        }

        if (start == number.length()) {
            return false;
        }

        boolean seenPoint = false;
        boolean seenDigit = false;

        for (int i = start; i < number.length(); i++) {
            char ch = number.charAt(i);

            if (ch == '.') {
                if (seenPoint) {
                    return false;
                }
                seenPoint = true;
                continue;
            }

            if (Character.digit(ch, base) == -1) {
                return false;
            }

            seenDigit = true;
        }

        return seenDigit;
    }

    public static void printInvalidNumber(String input, int base) {
        System.out.println();

        if (containsEmbeddedSubscriptBaseSymbol(input)) {
            System.out.println("ERROR: A subscript base symbol is only allowed at the END of the number.");
            System.out.println("Examples: 101101₂, 77₈, 450₁₀, 1C2₁₆");
            return;
        }

        System.out.println("ERROR: '" + input + "' is not a valid Base " + base + " number.");
        System.out.println("Allowed digits: " + allowedDigits(base));
        System.out.println("A single decimal point is also allowed for fractions.");
    }

    public static boolean containsEmbeddedSubscriptBaseSymbol(String text) {
        String symbols = "₂₈₁₀₆";
        for (int i = 0; i < text.length(); i++) {
            if (symbols.indexOf(text.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String allowedDigits(int base) {
        switch (base) {
            case 2:  return "0 and 1";
            case 8:  return "0 to 7";
            case 10: return "0 to 9";
            case 16: return "0 to 9 and A to F";
            default: return "";
        }
    }

    // =====================================================
    // PARSE SOURCE NUMBER EXACTLY AS A FRACTION
    // No double/float is used, so conversion is accurate.
    // =====================================================
    public static Fraction parseToFraction(String number, int base) {

        boolean negative = number.startsWith("-");
        boolean positiveSign = number.startsWith("+");

        if (negative || positiveSign) {
            number = number.substring(1);
        }

        String[] parts = number.split("\\.", -1);
        String integerPart = parts[0].isEmpty() ? "0" : parts[0];
        String fractionPart = (parts.length > 1) ? parts[1] : "";

        BigInteger integerValue = parseDigits(integerPart, base);

        BigInteger denominator = BigInteger.ONE;
        BigInteger fractionValue = BigInteger.ZERO;

        if (!fractionPart.isEmpty()) {
            denominator = BigInteger.valueOf(base).pow(fractionPart.length());
            fractionValue = parseDigits(fractionPart, base);
        }

        BigInteger numerator = integerValue.multiply(denominator).add(fractionValue);

        if (negative) {
            numerator = numerator.negate();
        }

        return new Fraction(numerator, denominator);
    }

    public static BigInteger parseDigits(String digits, int base) {
        BigInteger value = BigInteger.ZERO;
        BigInteger baseValue = BigInteger.valueOf(base);

        for (int i = 0; i < digits.length(); i++) {
            int digit = Character.digit(digits.charAt(i), base);
            value = value.multiply(baseValue).add(BigInteger.valueOf(digit));
        }

        return value;
    }

    // =====================================================
    // SHOW SOURCE BASE -> DECIMAL USING PLACE VALUES
    // =====================================================
    public static void showSourceToDecimalSteps(String input, int base, Fraction finalValue) {

        String number = input;
        if (number.startsWith("-") || number.startsWith("+")) {
            number = number.substring(1);
        }

        String[] parts = number.split("\\.", -1);
        String integerPart = parts[0].isEmpty() ? "0" : parts[0];
        String fractionPart = (parts.length > 1) ? parts[1] : "";

        System.out.println();
        System.out.println("STEP 1: Base " + base + " -> Decimal");
        System.out.println("--------------------------------------");
        System.out.println("Using place values:");
        System.out.println();

        for (int i = 0; i < integerPart.length(); i++) {
            char ch = integerPart.charAt(i);
            int digit = Character.digit(ch, base);
            int power = integerPart.length() - 1 - i;
            BigInteger term = BigInteger.valueOf(digit)
                    .multiply(BigInteger.valueOf(base).pow(power));

            System.out.println(displayDigit(ch, digit)
                    + " x " + base + "^" + power
                    + " = " + term);
        }

        for (int i = 0; i < fractionPart.length(); i++) {
            char ch = fractionPart.charAt(i);
            int digit = Character.digit(ch, base);
            int power = i + 1;

            Fraction term = new Fraction(
                    BigInteger.valueOf(digit),
                    BigInteger.valueOf(base).pow(power)
            );

            System.out.println(displayDigit(ch, digit)
                    + " x " + base + "^-" + power
                    + " = " + fractionToReadableDecimal(term));
        }

        System.out.println();
        System.out.println("Decimal value before exponent: "
                + fractionToBaseString(finalValue, 10, false));
    }

    public static String displayDigit(char ch, int digit) {
        if (digit >= 10) {
            return ch + "(" + digit + ")";
        }
        return String.valueOf(digit);
    }

    // =====================================================
    // EXACT FRACTION -> TARGET BASE
    // Whole part: repeated division.
    // Fraction part: repeated multiplication.
    // =====================================================
    public static String fractionToBaseString(Fraction value, int base, boolean showSteps) {

        boolean negative = value.isNegative();
        Fraction absolute = value.abs();

        BigInteger numerator = absolute.numerator;
        BigInteger denominator = absolute.denominator;

        BigInteger integerPart = numerator.divide(denominator);
        BigInteger remainder = numerator.remainder(denominator);

        String integerAnswer;

        if (showSteps) {
            System.out.println();
            System.out.println("STEP 2: Decimal -> Base " + base);
            System.out.println("--------------------------------");
            System.out.println("WHOLE-NUMBER PART");
            System.out.println("Repeated division by " + base);
            System.out.println();
        }

        if (integerPart.equals(BigInteger.ZERO)) {
            integerAnswer = "0";
            if (showSteps) {
                System.out.println("Whole-number part = 0");
            }
        } else {
            StringBuilder reversed = new StringBuilder();
            BigInteger current = integerPart;
            BigInteger baseValue = BigInteger.valueOf(base);

            List<String[]> divisionRows = new ArrayList<>();

            while (current.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] qr = current.divideAndRemainder(baseValue);
                int digitValue = qr[1].intValue();
                char digit = digitChar(digitValue);

                String remDisplay = digitValue >= 10
                        ? digitValue + " (" + digit + ")"
                        : String.valueOf(digitValue);

                if (showSteps) {
                    divisionRows.add(new String[] {
                            current + " / " + base,
                            qr[0].toString(),
                            remDisplay
                    });
                }

                reversed.append(digit);
                current = qr[0];
            }

            integerAnswer = reversed.reverse().toString();

            if (showSteps) {
                printAlignedTable(
                        new String[] {"Division", "Quotient", "Remainder"},
                        divisionRows,
                        5
                );

                System.out.println();
                System.out.println("Read the whole-number");
                System.out.println("remainders from bottom to top:");
                System.out.println(integerAnswer);
            }
        }

        String fractionAnswer = "";
        boolean repeatingOrCut = false;

        if (!remainder.equals(BigInteger.ZERO)) {

            if (showSteps) {
                System.out.println();
                System.out.println("FRACTIONAL PART");
                System.out.println("Repeated multiplication by " + base);
                System.out.println();
            }

            StringBuilder fracDigits = new StringBuilder();
            int count = 0;
            List<String[]> fractionRows = new ArrayList<>();

            while (!remainder.equals(BigInteger.ZERO)
                    && count < MAX_FRACTION_DIGITS) {

                BigInteger before = remainder;
                BigInteger multiplied = remainder.multiply(BigInteger.valueOf(base));
                BigInteger digitBI = multiplied.divide(denominator);
                remainder = multiplied.remainder(denominator);

                int digitValue = digitBI.intValue();
                char digit = digitChar(digitValue);
                fracDigits.append(digit);

                if (showSteps) {
                    String beforeText = rationalToReadableDecimal(before, denominator);
                    String productText = rationalToReadableDecimal(multiplied, denominator);
                    String digitText = digitValue >= 10
                            ? digitValue + " (" + digit + ")"
                            : String.valueOf(digitValue);

                    fractionRows.add(new String[] {
                            beforeText + " x " + base,
                            productText,
                            digitText
                    });
                }

                count++;
            }

            if (!remainder.equals(BigInteger.ZERO)) {
                repeatingOrCut = true;
            }

            fractionAnswer = fracDigits.toString();

            if (showSteps) {
                printAlignedTable(
                        new String[] {"Fraction x Base", "Product", "Digit"},
                        fractionRows,
                        5
                );

                System.out.println();
                System.out.println("Read the fractional digits");
                System.out.println("from top to bottom:");
                System.out.println(fractionAnswer + (repeatingOrCut ? "..." : ""));

                if (repeatingOrCut) {
                    System.out.println();
                    System.out.println("Note: The fraction repeats or continues.");
                    System.out.println("Showing the first " + MAX_FRACTION_DIGITS + " digits.");
                }
            }
        }

        String result = integerAnswer;
        if (!fractionAnswer.isEmpty()) {
            result += "." + fractionAnswer;
            if (repeatingOrCut) {
                result += "...";
            }
        }

        if (negative && !result.equals("0")) {
            result = "-" + result;
        }

        return result;
    }

    // =====================================================
    // CLEAN OUTPUT (NO EXPLANATION LABELS)
    // =====================================================
    public static String performCleanConversion(
            ParsedExpression expression,
            int sourceBase,
            int targetBase) {

        StringBuilder output = new StringBuilder();
        Fraction baseValue = parseToFraction(expression.numberText, sourceBase);
        Fraction value = baseValue;

        if (sourceBase != 10) {
            appendCleanSourceSteps(output, expression.numberText, sourceBase);
        }

        if (expression.hasExponent) {
            value = baseValue.pow(expression.exponent);
            appendCleanLine(output,
                    expression.numberText + " ^ " + expression.exponent
                            + " = " + fractionToBaseString(value, 10, false));
        }

        String answer;
        if (sourceBase == targetBase && !expression.hasExponent) {
            answer = expression.numberText;
        } else if (targetBase == 10) {
            answer = fractionToBaseString(value, 10, false);
        } else {
            answer = appendCleanTargetSteps(output, value, targetBase);
        }

        String originalInput = expression.numberText;
        if (expression.hasExponent) {
            originalInput += expression.exponentDisplay;
        }

        appendCleanLine(output, repeatChar('=', 32));
        appendCleanLine(output, "FINAL ANSWER");
        appendCleanLine(output, repeatChar('=', 32));
        appendCleanLine(output,
                "(" + originalInput + ")" + baseSubscript(sourceBase)
                        + " = (" + answer + ")" + baseSubscript(targetBase));
        return output.toString().trim();
    }

    private static void appendCleanSourceSteps(
            StringBuilder output,
            String input,
            int base) {

        String number = input;
        if (number.startsWith("-") || number.startsWith("+")) {
            number = number.substring(1);
        }

        String[] parts = number.split("\\.", -1);
        String integerPart = parts[0].isEmpty() ? "0" : parts[0];
        String fractionPart = parts.length > 1 ? parts[1] : "";

        for (int i = 0; i < integerPart.length(); i++) {
            char ch = integerPart.charAt(i);
            int digit = Character.digit(ch, base);
            int power = integerPart.length() - 1 - i;
            BigInteger term = BigInteger.valueOf(digit)
                    .multiply(BigInteger.valueOf(base).pow(power));
            appendCleanLine(output,
                    displayDigit(ch, digit) + " x " + base + "^" + power
                            + " = " + term);
        }

        for (int i = 0; i < fractionPart.length(); i++) {
            char ch = fractionPart.charAt(i);
            int digit = Character.digit(ch, base);
            int power = i + 1;
            Fraction term = new Fraction(
                    BigInteger.valueOf(digit),
                    BigInteger.valueOf(base).pow(power));
            appendCleanLine(output,
                    displayDigit(ch, digit) + " x " + base + "^-" + power
                            + " = " + fractionToReadableDecimal(term));
        }
    }

    private static String appendCleanTargetSteps(
            StringBuilder output,
            Fraction value,
            int base) {

        boolean negative = value.isNegative();
        Fraction absolute = value.abs();
        BigInteger numerator = absolute.numerator;
        BigInteger denominator = absolute.denominator;
        BigInteger integerPart = numerator.divide(denominator);
        BigInteger remainder = numerator.remainder(denominator);
        BigInteger baseValue = BigInteger.valueOf(base);

        StringBuilder reversedInteger = new StringBuilder();
        BigInteger current = integerPart;

        while (current.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] quotientAndRemainder = current.divideAndRemainder(baseValue);
            char digit = digitChar(quotientAndRemainder[1].intValue());
            appendCleanLine(output,
                    current + " / " + base + " = "
                            + quotientAndRemainder[0] + " R " + digit);
            reversedInteger.append(digit);
            current = quotientAndRemainder[0];
        }

        String integerAnswer = reversedInteger.length() == 0
                ? "0"
                : reversedInteger.reverse().toString();
        String signedInteger = negative && !integerAnswer.equals("0")
                ? "-" + integerAnswer
                : integerAnswer;

        appendCleanLine(output, "(" + signedInteger + ")" + baseSubscript(base));

        StringBuilder fractionAnswer = new StringBuilder();
        int count = 0;
        while (!remainder.equals(BigInteger.ZERO)
                && count < MAX_FRACTION_DIGITS) {

            BigInteger before = remainder;
            BigInteger multiplied = before.multiply(baseValue);
            BigInteger digitValue = multiplied.divide(denominator);
            remainder = multiplied.remainder(denominator);
            char digit = digitChar(digitValue.intValue());
            fractionAnswer.append(digit);

            appendCleanLine(output,
                    rationalToReadableDecimal(before, denominator)
                            + " x " + base + " = "
                            + rationalToReadableDecimal(multiplied, denominator)
                            + " " + digit);
            count++;
        }

        if (fractionAnswer.length() > 0) {
            String fractionText = fractionAnswer.toString();
            if (!remainder.equals(BigInteger.ZERO)) {
                fractionText += "...";
            }
            appendCleanLine(output,
                    "(" + fractionText + ")" + baseSubscript(base));
        }

        String result = signedInteger;
        if (fractionAnswer.length() > 0) {
            result += "." + fractionAnswer;
            if (!remainder.equals(BigInteger.ZERO)) {
                result += "...";
            }
        }
        return result;
    }

    private static void appendCleanLine(StringBuilder output, String line) {
        if (output.length() > 0) {
            output.append('\n');
        }
        output.append(line);
    }

    public static void printAlignedTable(String[] headers, List<String[]> rows, int gap) {
        int columnCount = headers.length;
        int[] widths = new int[columnCount];

        for (int i = 0; i < columnCount; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < columnCount && i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder headerLine = new StringBuilder();
        for (int i = 0; i < columnCount; i++) {
            headerLine.append(padRight(headers[i], widths[i]));
            if (i < columnCount - 1) {
                headerLine.append(spaces(gap));
            }
        }
        System.out.println(headerLine);

        int totalWidth = 0;
        for (int width : widths) {
            totalWidth += width;
        }
        totalWidth += gap * (columnCount - 1);
        System.out.println(repeatChar('-', totalWidth));

        for (String[] row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < columnCount; i++) {
                String value = i < row.length ? row[i] : "";
                line.append(padRight(value, widths[i]));
                if (i < columnCount - 1) {
                    line.append(spaces(gap));
                }
            }
            System.out.println(line);
        }
    }

    public static String padRight(String text, int width) {
        StringBuilder out = new StringBuilder(text == null ? "" : text);
        while (out.length() < width) {
            out.append(' ');
        }
        return out.toString();
    }

    public static String spaces(int count) {
        return repeatChar(' ', count);
    }

    public static String repeatChar(char ch, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append(ch);
        }
        return out.toString();
    }

    public static char digitChar(int value) {
        if (value >= 0 && value <= 9) {
            return (char) ('0' + value);
        }
        return (char) ('A' + (value - 10));
    }

    // =====================================================
    // DISPLAY HELPERS
    // =====================================================
    public static String fractionToReadableDecimal(Fraction value) {
        return rationalToReadableDecimal(value.numerator, value.denominator);
    }

    public static String rationalToReadableDecimal(BigInteger numerator, BigInteger denominator) {
        BigDecimal n = new BigDecimal(numerator);
        BigDecimal d = new BigDecimal(denominator);

        BigDecimal value = n.divide(d, 20, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        String text = value.toPlainString();
        return text.equals("-0") ? "0" : text;
    }

    public static void printFinalAnswer(String originalInput, int sourceBase,
                                        String answer, int targetBase) {
        System.out.println("================================");
        System.out.println("FINAL ANSWER");
        System.out.println("================================");
        System.out.println("(" + originalInput + ")" + baseSubscript(sourceBase)
                + " = (" + answer + ")" + baseSubscript(targetBase));
    }

    public static String baseSubscript(int base) {
        switch (base) {
            case 2: return "₂";
            case 8: return "₈";
            case 10: return "₁₀";
            case 16: return "₁₆";
            default: return " [base " + base + "]";
        }
    }
}
