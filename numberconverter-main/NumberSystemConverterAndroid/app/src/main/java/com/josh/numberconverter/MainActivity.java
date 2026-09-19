package com.josh.numberconverter;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int BACKGROUND = Color.rgb(10, 16, 31);
    private static final int CARD = Color.rgb(19, 29, 49);
    private static final int CARD_LIGHT = Color.rgb(27, 40, 65);
    private static final int BORDER = Color.rgb(49, 67, 96);
    private static final int PRIMARY = Color.rgb(91, 104, 255);
    private static final int TEXT = Color.rgb(244, 247, 255);
    private static final int MUTED = Color.rgb(154, 169, 197);
    private static final int SUCCESS = Color.rgb(86, 220, 169);

    private EditText inputNumber;
    private Spinner sourceSpinner;
    private Spinner targetSpinner;
    private Switch showSolutionToggle;
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
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(buildUi());
    }

    private View buildUi() {
        int pagePad = dp(18);

        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(BACKGROUND);
        page.setClipToPadding(false);
        page.setPadding(0, 0, 0, dp(12));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pagePad, dp(22), pagePad, dp(28));
        page.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = label("JJS • STUDY TOOL", 12, PRIMARY, true);
        eyebrow.setLetterSpacing(0.12f);
        root.addView(eyebrow, matchWrap(0, 0, 0, dp(8)));

        // Keep the requested title exactly as provided by the user.
        TextView title = label("JayVie Jabai Scrept", 29, TEXT, true);
        root.addView(title, matchWrap(0, 0, 0, dp(5)));

        TextView subtitle = label("Convert number systems with clean answers and guided steps.", 14, MUTED, false);
        subtitle.setLineSpacing(0, 1.1f);
        root.addView(subtitle, matchWrap(0, 0, 0, dp(22)));

        LinearLayout inputCard = card();
        addSectionLabel(inputCard, "INPUT NUMBER / EXPRESSION");

        inputNumber = new EditText(this);
        inputNumber.setHint("Example: 110101, A3.F, 2³ or (101101)₂");
        inputNumber.setHintTextColor(Color.rgb(111, 130, 162));
        inputNumber.setTextColor(TEXT);
        inputNumber.setSingleLine(true);
        inputNumber.setTextSize(17);
        inputNumber.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        inputNumber.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        inputNumber.setPadding(dp(14), dp(14), dp(14), dp(14));
        inputNumber.setBackground(outlineBackground(CARD_LIGHT, BORDER, dp(14)));
        inputCard.addView(inputNumber, matchWrap(0, 0, 0, dp(13)));

        TextView symbolsHint = label("QUICK SYMBOLS", 11, MUTED, true);
        symbolsHint.setLetterSpacing(0.08f);
        inputCard.addView(symbolsHint, matchWrap(0, 0, 0, dp(7)));

        HorizontalScrollView symbolScroller = new HorizontalScrollView(this);
        symbolScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout symbolRow = new LinearLayout(this);
        symbolRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] symbols = {"²", "³", "⁻", "₂", "₈", "₁₀", "₁₆"};
        for (String symbol : symbols) {
            TextView chip = chip(symbol);
            chip.setOnClickListener(v -> appendSymbol(symbol));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(dp(48), dp(42));
            chipParams.setMargins(0, 0, dp(8), 0);
            symbolRow.addView(chip, chipParams);
        }
        symbolScroller.addView(symbolRow, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inputCard.addView(symbolScroller);
        root.addView(inputCard, matchWrap(0, 0, 0, dp(14)));

        LinearLayout conversionCard = card();
        addSectionLabel(conversionCard, "CONVERT FROM  →  TO");

        LinearLayout baseRow = new LinearLayout(this);
        baseRow.setGravity(Gravity.CENTER_VERTICAL);
        baseRow.setOrientation(LinearLayout.HORIZONTAL);

        sourceSpinner = createBaseSpinner();
        sourceSpinner.setSelection(2);
        baseRow.addView(sourceSpinner, weightWrap(1, 0, dp(5), 0));

        TextView swapIcon = label("⇄", 27, PRIMARY, true);
        swapIcon.setGravity(Gravity.CENTER);
        swapIcon.setContentDescription("Swap source and target bases");
        swapIcon.setOnClickListener(v -> swapBases());
        baseRow.addView(swapIcon, new LinearLayout.LayoutParams(dp(48), dp(52)));

        targetSpinner = createBaseSpinner();
        targetSpinner.setSelection(0);
        baseRow.addView(targetSpinner, weightWrap(1, dp(5), 0, 0));
        conversionCard.addView(baseRow);

        LinearLayout solutionRow = new LinearLayout(this);
        solutionRow.setGravity(Gravity.CENTER_VERTICAL);
        solutionRow.setPadding(0, dp(14), 0, 0);

        LinearLayout solutionText = new LinearLayout(this);
        solutionText.setOrientation(LinearLayout.VERTICAL);
        TextView solutionTitle = label("Show solution", 15, TEXT, true);
        TextView solutionSubtitle = label("Display the step-by-step process", 12, MUTED, false);
        solutionText.addView(solutionTitle);
        solutionText.addView(solutionSubtitle, matchWrap(0, dp(3), 0, 0));
        solutionRow.addView(solutionText, weightWrap(1, 0, dp(8), 0));

        showSolutionToggle = new Switch(this);
        showSolutionToggle.setChecked(true);
        showSolutionToggle.setContentDescription("Show solution");
        showSolutionToggle.setButtonTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{PRIMARY, MUTED}));
        solutionRow.addView(showSolutionToggle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(48)));
        conversionCard.addView(solutionRow);
        root.addView(conversionCard, matchWrap(0, 0, 0, dp(14)));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);

        Button calculate = actionButton("Convert", PRIMARY, TEXT);
        calculate.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calculate.setOnClickListener(v -> calculate());
        actionRow.addView(calculate, weightWrap(1, 0, dp(5), 0));

        Button clear = actionButton("Clear", CARD, TEXT);
        clear.setBackground(outlineBackground(CARD, BORDER, dp(14)));
        clear.setOnClickListener(v -> {
            inputNumber.setText("");
            outputText.setText("Your converted answer will appear here.");
        });
        actionRow.addView(clear, weightWrap(1, dp(5), 0, 0));
        root.addView(actionRow, matchWrap(0, 0, 0, dp(18)));

        LinearLayout resultCard = card();
        LinearLayout resultHeader = new LinearLayout(this);
        resultHeader.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout resultTitleGroup = new LinearLayout(this);
        resultTitleGroup.setOrientation(LinearLayout.VERTICAL);
        TextView resultLabel = label("RESULT", 12, SUCCESS, true);
        resultLabel.setLetterSpacing(0.1f);
        TextView resultSubtitle = label("Your answer and solution", 13, MUTED, false);
        resultTitleGroup.addView(resultLabel);
        resultTitleGroup.addView(resultSubtitle, matchWrap(0, dp(3), 0, 0));
        resultHeader.addView(resultTitleGroup, weightWrap(1, 0, dp(8), 0));

        Button copy = smallButton("Copy");
        copy.setOnClickListener(v -> copyResult());
        resultHeader.addView(copy, new LinearLayout.LayoutParams(dp(74), dp(40)));
        resultCard.addView(resultHeader, matchWrap(0, 0, 0, dp(12)));

        outputText = new TextView(this);
        outputText.setText("Your converted answer will appear here.");
        outputText.setTextSize(14);
        outputText.setTextColor(TEXT);
        outputText.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        outputText.setLineSpacing(dp(3), 1.08f);
        outputText.setTextIsSelectable(true);
        outputText.setHorizontallyScrolling(true);
        outputText.setMinWidth(dp(260));
        outputText.setPadding(dp(14), dp(14), dp(14), dp(14));
        outputText.setBackground(outlineBackground(Color.rgb(14, 22, 39), BORDER, dp(12)));

        HorizontalScrollView outputScroller = new HorizontalScrollView(this);
        outputScroller.setFillViewport(true);
        outputScroller.setHorizontalScrollBarEnabled(true);
        outputScroller.addView(outputText, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        resultCard.addView(outputScroller, matchWrap(0, 0, 0, 0));
        root.addView(resultCard, matchWrap(0, 0, 0, dp(14)));

        TextView tip = label("TIP  •  Turn on Show solution when studying the conversion process.", 12, MUTED, false);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(dp(4), dp(4), dp(4), dp(4));
        root.addView(tip, matchWrap(0, 0, 0, 0));

        return page;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(15), dp(15), dp(15), dp(15));
        layout.setBackground(outlineBackground(CARD, BORDER, dp(18)));
        return layout;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    private void addSectionLabel(LinearLayout parent, String text) {
        TextView view = label(text, 11, MUTED, true);
        view.setLetterSpacing(0.08f);
        parent.addView(view, matchWrap(0, 0, 0, dp(10)));
    }

    private TextView chip(String text) {
        TextView view = label(text, 16, TEXT, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(rippleBackground(CARD_LIGHT, BORDER, dp(12)));
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }

    private Button actionButton(String text, int color, int textColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rippleBackground(color, color, dp(14)));
        return button;
    }

    private Button smallButton(String text) {
        Button button = actionButton(text, CARD_LIGHT, TEXT);
        button.setTextSize(12);
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    private Spinner createBaseSpinner() {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, baseLabels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = spinnerText(getItem(position));
                view.setTextColor(TEXT);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = spinnerText(getItem(position));
                view.setTextColor(TEXT);
                view.setBackgroundColor(CARD_LIGHT);
                return view;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setBackground(outlineBackground(CARD_LIGHT, BORDER, dp(13)));
        spinner.setPadding(dp(8), 0, dp(7), 0);
        return spinner;
    }

    private TextView spinnerText(String text) {
        TextView view = label(text, 13, TEXT, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setPadding(dp(8), 0, dp(4), 0);
        return view;
    }

    private GradientDrawable outlineBackground(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private RippleDrawable rippleBackground(int fill, int stroke, int radius) {
        return new RippleDrawable(
                ColorStateList.valueOf(Color.argb(45, 255, 255, 255)),
                outlineBackground(fill, stroke, radius),
                null
        );
    }

    private LinearLayout.LayoutParams matchWrap(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(l, t, r, b);
        return p;
    }

    private LinearLayout.LayoutParams weightWrap(float weight, int l, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(52), weight);
        p.setMargins(l, 0, r, b);
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
        inputNumber.requestFocus();
    }

    private void swapBases() {
        int source = sourceSpinner.getSelectedItemPosition();
        int target = targetSpinner.getSelectedItemPosition();
        sourceSpinner.setSelection(target);
        targetSpinner.setSelection(source);
    }

    private void copyResult() {
        String result = outputText == null ? "" : outputText.getText().toString();
        if (result.trim().isEmpty() || result.startsWith("Your converted answer")) {
            Toast.makeText(this, "Convert a number first", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Number conversion result", result));
        Toast.makeText(this, "Result copied", Toast.LENGTH_SHORT).show();
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

        if (!showSolutionToggle.isChecked()) {
            try {
                outputText.setText(NumberConversion.performCleanConversion(
                        expression,
                        sourceBase,
                        targetBase
                ));
            } catch (Exception ex) {
                showError("Calculation error: " + ex.getMessage());
            }
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
