package Converting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Converting {
    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(30);
    private static final int MAX_ERROR_TEXT = 6000;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Object cancellationLock = new Object();
    private volatile Process currentProcess;

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(int completed, int total, String currentFile);
    }

    public record ConversionResult(
            int total,
            int converted,
            List<String> failures,
            Path outputDirectory,
            boolean cancelled
    ) {
        public boolean isSuccessful() {
            return !cancelled && total > 0 && failures.isEmpty() && converted == total;
        }
    }

    public ConversionResult convertFolder(
            Path inputDirectory,
            String inputExtension,
            String outputExtension,
            ProgressListener progressListener
    ) throws IOException {
        return convertFolder(inputDirectory, inputExtension, outputExtension, progressListener, null);
    }

    public ConversionResult convertFolder(
            Path inputDirectory,
            String inputExtension,
            String outputExtension,
            ProgressListener progressListener,
            Path outputReservation
    ) throws IOException {
        Path input = inputDirectory.toAbsolutePath().normalize();
        if (!Files.isDirectory(input)) {
            throw new IOException("The selected folder does not exist or is not accessible: " + input);
        }

        String from = normalizeExtension(inputExtension);
        String to = normalizeExtension(outputExtension);
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("The input and output formats must be different.");
        }

        Path output = outputDirectoryFor(input, to);
        Path outputForScan = output;
        List<Path> files;
        try (Stream<Path> walk = Files.walk(input)) {
            files = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(outputForScan))
                    .filter(path -> hasExtension(path, from))
                    .sorted(Comparator.comparing(path -> input.relativize(path).toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        if (!files.isEmpty()) {
            verifyImageMagick();
            if (cancelled.get()) {
                return new ConversionResult(files.size(), 0, List.of(), output, true);
            }
            output = reserveOutputDirectory(input, to, outputReservation);
        }

        List<String> failures = new ArrayList<>();
        int converted = 0;
        int completed = 0;

        for (Path source : files) {
            if (cancelled.get()) break;
            Path relative = input.relativize(source);
            Path destination = output.resolve(withExtension(relative, to));
            try {
                convertOne(source, destination, to);
                converted++;
                synchronized (cancellationLock) {
                    if (cancelled.get()) break;
                    try {
                        Files.delete(source);
                    } catch (IOException exception) {
                        failures.add(input.relativize(source) + ": converted successfully, but the original could not be deleted: "
                                + (exception.getMessage() == null ? "access denied" : exception.getMessage()));
                    }
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Conversion was interrupted.", exception);
            } catch (Exception exception) {
                if (cancelled.get()) break;
                String detail = exception.getMessage();
                failures.add(input.relativize(source) + ": " + (detail == null ? exception.getClass().getSimpleName() : detail));
            }
            completed++;
            if (progressListener != null) {
                progressListener.onProgress(completed, files.size(), source.getFileName().toString());
            }
        }

        return new ConversionResult(files.size(), converted, List.copyOf(failures), output, cancelled.get());
    }

    public void cancel() {
        Process process;
        synchronized (cancellationLock) {
            cancelled.set(true);
            process = currentProcess;
        }
        if (process != null && process.isAlive()) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    public static Path outputDirectoryFor(Path inputDirectory, String outputExtension) {
        Path input = inputDirectory.toAbsolutePath().normalize();
        String extension = normalizeExtension(outputExtension).substring(1);
        String baseName = outputBaseName(input, extension);
        Path parent = outputParent(input);
        int suffix = 0;
        while (true) {
            String name = suffix == 0 ? baseName : baseName + "-" + suffix;
            Path candidate = parent.resolve(name);
            if (!Files.exists(candidate)) return candidate;
            suffix++;
        }
    }

    public static List<Path> existingOutputDirectoriesFor(Path inputDirectory, String outputExtension) {
        Path input = inputDirectory.toAbsolutePath().normalize();
        String extension = normalizeExtension(outputExtension).substring(1);
        String baseName = outputBaseName(input, extension);
        Path parent = outputParent(input);
        try (Stream<Path> children = Files.list(parent)) {
            return children
                    .filter(Files::isDirectory)
                    .filter(path -> outputSequence(path.getFileName().toString(), baseName) >= 0)
                    .sorted(Comparator.comparingInt(path -> outputSequence(path.getFileName().toString(), baseName)))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static Path reserveOutputDirectory(Path input, String extension, Path reservation) throws IOException {
        String baseName = outputBaseName(input, extension.substring(1));
        Path parent = outputParent(input);
        if (reservation != null) {
            Path normalizedReservation = reservation.toAbsolutePath().normalize();
            if (normalizedReservation.getParent() != null
                    && normalizedReservation.getParent().equals(parent)
                    && outputSequence(normalizedReservation.getFileName().toString(), baseName) >= 0
                    && Files.isDirectory(normalizedReservation)
                    && isEmptyDirectory(normalizedReservation)) {
                return normalizedReservation;
            }
        }
        int suffix = 0;
        while (true) {
            String name = suffix == 0 ? baseName : baseName + "-" + suffix;
            Path candidate = parent.resolve(name);
            try {
                Files.createDirectory(candidate);
                return candidate;
            } catch (FileAlreadyExistsException exception) {
                suffix++;
            }
        }
    }

    private static boolean isEmptyDirectory(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.findAny().isEmpty();
        } catch (IOException exception) {
            return false;
        }
    }

    private static String outputBaseName(Path input, String extension) {
        Path folderName = input.getFileName();
        return (folderName == null ? "converted" : folderName.toString()) + "-converted-" + extension;
    }

    private static Path outputParent(Path input) {
        Path parent = input.getParent();
        return parent == null ? input : parent;
    }

    private static int outputSequence(String candidateName, String baseName) {
        if (candidateName.equals(baseName)) return 0;
        String prefix = baseName + "-";
        if (!candidateName.startsWith(prefix)) return -1;
        try {
            int suffix = Integer.parseInt(candidateName.substring(prefix.length()));
            return suffix > 0 ? suffix : -1;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private void convertOne(Path source, Path destination, String outputExtension) throws Exception {
        if (cancelled.get()) throw new IOException("Conversion cancelled.");
        Path parent = destination.getParent();
        Files.createDirectories(parent);
        Path temporaryOutput = Files.createTempFile(parent, ".convert-", outputExtension);
        try {
            Process process = new ProcessBuilder(
                    "magick",
                    source.toString(),
                    "-auto-orient",
                    "-colorspace", "sRGB",
                    "-auto-level",
                    "-modulate", "100,130",
                    temporaryOutput.toString()
            ).redirectErrorStream(true).start();
            currentProcess = process;
            if (cancelled.get()) process.destroyForcibly();

            OutputCollector output = new OutputCollector();
            Thread outputReader = new Thread(() -> readOutput(process, output), "imagemagick-output-reader");
            outputReader.setDaemon(true);
            outputReader.start();

            boolean finished;
            try {
                finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                forceStop(process);
                Thread.currentThread().interrupt();
                throw new IOException("Conversion was interrupted.", exception);
            }

            if (!finished) {
                stopProcess(process);
                outputReader.join(5000);
                throw new IOException("Conversion timed out after 30 minutes.");
            }
            try {
                outputReader.join(5000);
            } catch (InterruptedException exception) {
                forceStop(process);
                Thread.currentThread().interrupt();
                throw new IOException("Conversion was interrupted.", exception);
            }

            if (process.exitValue() != 0) {
                String message = output.text();
                throw new IOException("ImageMagick exited with code " + process.exitValue()
                        + (message.isBlank() ? "." : ": " + message));
            }
            if (!Files.isRegularFile(temporaryOutput) || Files.size(temporaryOutput) == 0) {
                throw new IOException("ImageMagick did not create an output file.");
            }

            try {
                Files.move(temporaryOutput, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryOutput, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            currentProcess = null;
            Files.deleteIfExists(temporaryOutput);
        }
    }

    private void verifyImageMagick() throws IOException {
        if (cancelled.get()) return;
        Process process;
        try {
            process = new ProcessBuilder("magick", "-version").redirectErrorStream(true).start();
        } catch (IOException exception) {
            throw new IOException("ImageMagick was not found. Install it and add the magick command to PATH.", exception);
        }
        currentProcess = process;
        if (cancelled.get()) process.destroyForcibly();

        OutputCollector output = new OutputCollector();
        Thread outputReader = new Thread(() -> readOutput(process, output), "imagemagick-version-reader");
        outputReader.setDaemon(true);
        outputReader.start();

        try {
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                stopProcess(process);
                throw new IOException("ImageMagick did not respond within 20 seconds.");
            }
            outputReader.join(5000);
            if (process.exitValue() != 0 && !cancelled.get()) {
                throw new IOException("ImageMagick failed to start: " + output.text());
            }
        } catch (InterruptedException exception) {
            forceStop(process);
            Thread.currentThread().interrupt();
            throw new IOException("The ImageMagick check was interrupted.", exception);
        } finally {
            currentProcess = null;
        }
    }

    private static void readOutput(Process process, OutputCollector output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        } catch (IOException exception) {
            output.append(exception.getMessage() == null ? "Could not read ImageMagick output." : exception.getMessage());
        }
    }

    private static void stopProcess(Process process) throws InterruptedException {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            process.waitFor();
        }
    }

    private static void forceStop(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }

    private static String normalizeExtension(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("No file format was selected.");
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("\\.[a-z0-9]{2,8}")) {
            throw new IllegalArgumentException("Invalid file extension: " + extension);
        }
        return normalized;
    }

    private static boolean hasExtension(Path path, String extension) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension);
    }

    private static Path withExtension(Path path, String extension) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot <= 0 ? name : name.substring(0, dot);
        return path.resolveSibling(stem + extension);
    }

    private static final class OutputCollector {
        private final StringBuilder text = new StringBuilder();

        synchronized void append(String line) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(line);
            if (text.length() > MAX_ERROR_TEXT) {
                text.delete(0, text.length() - MAX_ERROR_TEXT);
            }
        }

        synchronized String text() {
            return text.toString().trim();
        }
    }
}
