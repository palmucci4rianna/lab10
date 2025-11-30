package it.unibo.mvc;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Application entry point for DrawNumber game.
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    //private static final int MIN = 0;
    //private static final int MAX = 100;
    //private static final int ATTEMPTS = 10;

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * Constructor.
     *
     * @param configFile
     *            the configuration file path
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final String configFile, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration.Builder configurationBuilder = new Configuration.Builder();
        try (
            var fileData = new BufferedReader(
                new InputStreamReader(
                    Objects.requireNonNull(ClassLoader.getSystemResourceAsStream(configFile)),
                    StandardCharsets.UTF_8
                )
            )
        ) { 
            for (var configLine = fileData.readLine(); 
                configLine != null; 
                configLine = fileData.readLine()) {
                final String[] lineElements = configLine.split(":");
                if (lineElements.length == 2) {
                    final int value = Integer.parseInt(lineElements[1].trim());
                    //setta la configurazione in base alla chiave linea
                    if (lineElements[0].contains("max")) {
                        configurationBuilder.withMax(value);
                    } else if (lineElements[0].contains("min")) {
                        configurationBuilder.withMin(value);
                    } else if (lineElements[0].contains("attempts")) {
                        configurationBuilder.withAttempts(value);
                    }
                } else {
                    displayError("I cannot understand \"" + configLine + '"');
                }
            }
        } catch (IOException | NumberFormatException e) {
            displayError(e.getMessage());
        }
        final Configuration configuration = configurationBuilder.build();
        if (configuration.isConsistent()) {
            this.model = new DrawNumberImpl(configuration);
        } else {
            displayError("Inconsistent configuration: using default values");
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
        //this.model = new DrawNumberImpl(MIN, MAX, ATTEMPTS);
    }

    private void displayError(final String err) {
        for (final DrawNumberView view: views) {
            view.displayError(err);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (final IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    @SuppressFBWarnings(
        value = "DM_EXIT",
        justification = "Acceptable for exercising purposes."
    )
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * Application entry point.
     *
     * @param args
     *            ignored
     * @throws FileNotFoundException if the configuration file cannot be fetched
     */
    public static void main(final String... args) throws FileNotFoundException {
        //new DrawNumberApp(new DrawNumberViewImpl());
        new DrawNumberApp(
            "config.yml", // is part of classpath
            new DrawNumberViewImpl(),
            new PrintStreamView(System.out),
            new PrintStreamView("output.log")
        );
    }

}
