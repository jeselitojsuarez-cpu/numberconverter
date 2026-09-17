package com.josh.numberconverter;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private EditText inputNumber;
    private Spinner sourceSpinner;
    private Spinner targetSpinner;
    private TextView outputText;

    private final int[] bases = {2, 8, 10, 16};
    private final String[] baseLabels = {
            "Binary (Base 2)",
            "Octal (Base 8)",
            "Decimal (Base 10)",
            "Hexadecimal (Base 16)"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        int pad = dp(18);

        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(Color.rgb(245, 247, 251));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        page.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("NUMBER SYSTEM CONVERTER");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.rgb(24, 32, 48));
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap(0, 0, 0, dp(20)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Binary • Octal • Decimal • Hexadecimal\nFractions and exponents supported");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(0, 0, 0, dp(22)));

        addLabel(root, "Enter number / expression");
        inputNumber = new EditText(this);
        inputNumber.setHint("Examples: 110101.11, A3.F, 2³, (101101)₂");
        inputNumber.setSingleLine(true);
        inputNumber.setTextSize(18);
        inputNumber.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        inputNumber.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(inputNumber, matchWrap(0, 0, 0, dp(14)));

        LinearLayout symbolRow = new LinearLayout(this);
        symbolRow.setOrientation(LinearLayout.HORIZONTAL);
        symbolRow.setGravity(Gravity.CENTER);
        String[] symbols = {"²", "³", "⁻", "₂", "₈", "₁₀", "₁₆"};
        for (String symbol : symbols) {
            Button b = new Button(this);
            b.setText(symbol);
            b.setTextSize(15);
            b.setAllCaps(false);
            b.setOnClickListener(v -> appendSymbol(symbol));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            bp.setMargins(dp(2), 0, dp(2), 0);
            symbolRow.addView(b, bp);
        }
        root.addView(symbolRow, matchWrap(0, 0, 0, dp(18)));

        addLabel(root, "Source base");
        sourceSpinner = createBaseSpinner();
        sourceSpinner.setSelection(2); // Decimal default
        root.addView(sourceSpinner, matchWrap(0, 0, 0, dp(14)));

        addLabel(root, "Target base");
        targetSpinner = createBaseSpinner();
        targetSpinner.setSelection(0); // Binary default
        root.addView(targetSpinner, matchWrap(0, 0, 0, dp(18)));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button calculate = new Button(this);
        calculate.setText("CALCULATE");
        calculate.setTextSize(16);
        calculate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calculate.setOnClickListener(v -> calculate());
        LinearLayout.LayoutParams calcParams = new LinearLayout.LayoutParams(0, dp(54), 2f);
        calcParams.setMargins(0, 0, dp(6), 0);
        actionRow.addView(calculate, calcParams);

        Button swap = new Button(this);
        swap.setText("SWAP");
        swap.setAllCaps(false);
        swap.setOnClickListener(v -> swapBases());
        LinearLayout.LayoutParams swapParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        swapParams.setMargins(dp(6), 0, 0, 0);
        actionRow.addView(swap, swapParams);

        root.addView(actionRow, matchWrap(0, 0, 0, dp(18)));

        Button clear = new Button(this);
        clear.setText("Clear");
        clear.setAllCaps(false);
        clear.setOnClickListener(v -> {
            inputNumber.setText("");
            outputText.setText("Your step-by-step solution will appear here.");
        });
        root.addView(clear, matchWrap(0, 0, 0, dp(20)));

        TextView resultLabel = new TextView(this);
        resultLabel.setText("SOLUTION");
        resultLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        resultLabel.setTextSize(16);
        resultLabel.setTextColor(Color.rgb(24, 32, 48));
        root.addView(resultLabel, matchWrap(0, 0, 0, dp(8)));

        outputText = new TextView(this);
        outputText.setText("Your step-by-step solution will appear here.");
        outputText.setTextSize(14);
        outputText.setTextColor(Color.rgb(30, 35, 45));
        outputText.setTypeface(Typeface.MONOSPACE);
        outputText.setLineSpacing(dp(2), 1.08f);
        outputText.setTextIsSelectable(true);
        outputText.setPadding(dp(14), dp(14), dp(14), dp(14));
        outputText.setBackgroundColor(Color.WHITE);
        root.addView(outputText, matchWrap(0, 0, 0, dp(30)));

        return page;
    }

    private Spinner createBaseSpinner() {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                baseLabels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void addLabel(LinearLayout parent, String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextSize(14);
        label.setTextColor(Color.rgb(55, 62, 75));
        parent.addView(label, matchWrap(0, 0, 0, dp(6)));
    }

    private LinearLayout.LayoutParams matchWrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void appendSymbol(String symbol) {
        int start = Math.max(inputNumber.getSelectionStart(), 0);
        int end = Math.max(inputNumber.getSelectionEnd(), 0);
        String current = inputNumber.getText().toString();
        inputNumber.setText(current.substring(0, start) + symbol + current.substring(end));
        inputNumber.setSelection(start + symbol.length());
    }

    private void swapBases() {
        int source = sourceSpinner.getSelectedItemPosition();
        int target = targetSpinner.getSelectedItemPosition();
        sourceSpinner.setSelection(target);
        targetSpinner.setSelection(source);
    }

    private void calculate() {
        String rawInput = inputNumber.getText().toString().trim().toUpperCase();
        if (rawInput.isEmpty()) {
            inputNumber.setError("Enter a number first.");
            return;
        }

        int selectedSource = bases[sourceSpinner.getSelectedItemPosition()];
        int targetBase = bases[targetSpinner.getSelectedItemPosition()];

        NumberConversion.ParsedInput parsedInput = NumberConversion.parseOptionalBaseSuffix(rawInput);
        int sourceBase = parsedInput.annotatedBase != null ? parsedInput.annotatedBase : selectedSource;

        // If the number itself has a base suffix, use it automatically in the UI.
        if (parsedInput.annotatedBase != null) {
            setSpinnerToBase(sourceSpinner, sourceBase);
        }

        if (!NumberConversion.validBase(sourceBase) || !NumberConversion.validBase(targetBase)) {
            showError("Only Base 2, 8, 10, and 16 are supported.");
            return;
        }

        NumberConversion.ParsedExpression expression =
                NumberConversion.parseExponentExpression(parsedInput.expressionText);

        if (expression.error != null) {
            showError(expression.error + "\n\nExamples: 2³, 5², 10⁴, 2^3");
            return;
        }

        if (!NumberConversion.validNumber(expression.numberText, sourceBase)) {
            showError("'" + expression.numberText + "' is not a valid Base " + sourceBase
                    + " number.\nAllowed digits: " + NumberConversion.allowedDigits(sourceBase)
                    + "\nA single decimal point is allowed for fractions.");
            return;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8.name()));
            NumberConversion.performConversion(
                    expression,
                    rawInput,
                    sourceBase,
                    targetBase
            );
        } catch (Exception ex) {
            showError("Calculation error: " + ex.getMessage());
            return;
        } finally {
            System.setOut(originalOut);
        }

        String output = new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();
        if (output.isEmpty()) {
            output = "No output was produced.";
        }
        outputText.setText(output);
    }

    private void setSpinnerToBase(Spinner spinner, int base) {
        for (int i = 0; i < bases.length; i++) {
            if (bases[i] == base) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void showError(String message) {
        outputText.setText("ERROR\n\n" + message);
        Toast.makeText(this, "Check your input", Toast.LENGTH_SHORT).show();
    }
}
