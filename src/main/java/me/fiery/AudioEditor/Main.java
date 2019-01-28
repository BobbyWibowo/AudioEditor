package me.fiery.AudioEditor;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

class ReadAudioBytesResult {

    Exception exception = null;
    AudioFormat audioFormat = null;
    ArrayList<byte[]> array = null;
    int bufferSize = 0;
    long totalFramesRead = 0;
    AudioFormat oldAudioFormat = null;

}

public class Main {

    private static Form form;
    private static File file;
    private static ReadAudioBytesResult readAudioBytesResult;
    private static AudioBytesPlayer audioBytesPlayer;
    private static boolean isReading;
    private static boolean isApplyingEffect;

    public static void main(String[] args) {
        form = new Form();

        // Browse file button
        form.browseButton.addActionListener(actionEvent -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter defaultFilter = new FileNameExtensionFilter(
                    "Supported files", "mp3", "wave", "wav", "au", "aiff"
            );
            fileChooser.addChoosableFileFilter(defaultFilter);
            fileChooser.setFileFilter(defaultFilter);

            // Show dialog
            int returnValue = fileChooser.showOpenDialog(form.parentPanel);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                file = fileChooser.getSelectedFile();
                form.pathTextField.setText(file.getAbsolutePath());
                setGUIReadyState(false);
            }
        });

        // Read file button (as in to read its array of bytes)
        form.readButton.addActionListener(actionEvent -> {
            if (isReading)
                return;

            if (file == null) {
                form.showExceptionDialog("Please select a file to read!");
                return;
            }

            if (audioBytesPlayer != null) {
                audioBytesPlayer.stop();
                audioBytesPlayer = null;
            }

            isReading = true;
            String oldText = form.readButton.getText();
            toggleButton(form.readButton, "Reading...");

            new Thread(() -> {
                readAudioBytesResult = readAudioBytes(file);

                if (readAudioBytesResult.exception == null) {
                    System.out.println("DEBUG: array of byte[]:");
                    shortDebugArrayList(readAudioBytesResult.array);

                    long totalBytes = 0;
                    for (byte[] audioBytes : readAudioBytesResult.array)
                        totalBytes += (long) audioBytes.length;

                    long size = file.length();

                    String text = "";
                    text += "Directory: " + file.getParent() + "\n";
                    text += "File : " + file.getName() + "\n";
                    text += "Size : " + size + " bytes (" + humanReadableByteCount(size, false) + ")\n";
                    text += "Debug:\n";
                    text += "  Original : " + readAudioBytesResult.oldAudioFormat.toString() + "\n";
                    text += "  Converted: " + readAudioBytesResult.audioFormat.toString() + "\n";
                    text += "  array.size()   : " + readAudioBytesResult.array.size() + "\n";
                    text += "  bufferSize     : " + readAudioBytesResult.bufferSize + "\n";
                    text += "  totalFramesRead: " + readAudioBytesResult.totalFramesRead + "\n";
                    text += "  totalBytes     : " + totalBytes + "\n";
                    text += "\n";

                    form.logsTextArea.append(text);

                    // This feels like a nasty hack
                    String name = file.getName();
                    if (name.length() > 50)
                        name = name.substring(0, 50) + "...";
                    form.nameLabel.setText(name);

                    audioBytesPlayer = getAudioBytesPlayer(
                            readAudioBytesResult.audioFormat,
                            readAudioBytesResult.array
                    );
                } else {
                    if (!(readAudioBytesResult.exception instanceof UnsupportedAudioFileException))
                        readAudioBytesResult.exception.printStackTrace();
                    form.showExceptionDialog(readAudioBytesResult.exception.getMessage());
                }

                isReading = false;
                toggleButton(form.readButton, oldText);
            }).start();
        });

        // Play/Pause playback button
        form.playButton.addActionListener(actionEvent -> {
            if (audioBytesPlayer == null)
                return;

            if (audioBytesPlayer.getState() == AudioBytesPlayerThread.PLAYING) {
                audioBytesPlayer.pause();
                form.playButton.setText("Play");
            } else {
                audioBytesPlayer.play();
                form.playButton.setText("Pause");
            }

            form.stopButton.setEnabled(true);
        });

        // Stop playback button
        form.stopButton.addActionListener(actionEvent -> {
            if (audioBytesPlayer == null)
                return;

            audioBytesPlayer.stop();
            form.stopButton.setEnabled(false);
            form.playButton.setText("Play");
        });

        // Apply effect button
        form.effectButton.addActionListener(actionEvent -> {
            if (isApplyingEffect)
                return;

            if (readAudioBytesResult == null)
                return;

            if (audioBytesPlayer != null)
                audioBytesPlayer.stop();

            isApplyingEffect = true;
            String oldText = form.effectButton.getText();
            form.effectButton.setText("Applying...");
            setGUIReadyState(false);

            new Thread(() -> {
                ArrayList<short[]> paired = BytesUtil.pair(
                        readAudioBytesResult.audioFormat,
                        readAudioBytesResult.array
                );
                System.out.println("DEBUG: array of short[]:");
                shortDebugArrayList(paired);

                ArrayList<short[]> withEffect = AudioEffects.channelMixing(
                        paired,
                        readAudioBytesResult.audioFormat.getFrameRate()
                );
                System.out.println("DEBUG: array of short[] (with mono-stereo* effect):");
                shortDebugArrayList(withEffect);

                ArrayList<byte[]> severed = BytesUtil.sever(
                        readAudioBytesResult.audioFormat,
                        paired
                );

                audioBytesPlayer = getAudioBytesPlayer(
                        readAudioBytesResult.audioFormat,
                        severed
                );

                setGUIReadyState(true);
                form.effectButton.setText(oldText);
                form.effectButton.setEnabled(false);
                form.resetButton.setEnabled(true);

                isApplyingEffect = false;
            }).start();
        });

        // Reset applied effect button
        form.resetButton.addActionListener(actionEvent -> {
            if (audioBytesPlayer != null)
                audioBytesPlayer.stop();

            audioBytesPlayer = getAudioBytesPlayer(
                    readAudioBytesResult.audioFormat,
                    readAudioBytesResult.array
            );

            form.effectButton.setEnabled(true);
            form.resetButton.setEnabled(false);
        });

        // Clear textarea button
        form.clearButton.addActionListener(actionEvent -> form.logsTextArea.setText(""));
    }

    private static void setGUIReadyState(boolean isReady) {
        form.playButton.setText("Play");
        form.playButton.setEnabled(isReady);
        form.stopButton.setEnabled(false);
        form.effectButton.setEnabled(isReady);
        form.resetButton.setEnabled(false);
    }

    private static void toggleButton(JButton button, String newText) {
        button.setText(newText);
        button.setEnabled(!button.isEnabled());
    }

    private static AudioBytesPlayer getAudioBytesPlayer(AudioFormat audioFormat, ArrayList<byte[]> array) {
        AudioBytesPlayer audioBytesPlayer = null;

        try {
            audioBytesPlayer = new AudioBytesPlayer(audioFormat, array);

            float divider = audioFormat.getFrameRate() * audioFormat.getFrameSize();

            audioBytesPlayer.onProgress((streamedBytes, totalBytes) -> {
                int elapsed = (int) (streamedBytes / divider);
                int total = (int) (totalBytes / divider);
                form.progressLabel.setText(formatTime(elapsed) + " / " + formatTime(total));
            });

            audioBytesPlayer.onStopped((totalBytes) -> {
                int total = (int) (totalBytes / divider);
                form.progressLabel.setText("00:00 / " + formatTime(total));
            });

            setGUIReadyState(true);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            form.showExceptionDialog(e.getMessage());
        }

        return audioBytesPlayer;
    }

    private static ReadAudioBytesResult readAudioBytes(File file) {
        ReadAudioBytesResult result = new ReadAudioBytesResult();
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            result.oldAudioFormat = audioInputStream.getFormat();

            // We want uniform 16-bit, stereo, PCM_SIGNED
            final int ch = 2;
            final float rate = result.oldAudioFormat.getSampleRate();
            result.audioFormat = new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
            audioInputStream = AudioSystem.getAudioInputStream(result.audioFormat, audioInputStream);

            // Container for all buffers of 1024 frames
            result.array = new ArrayList<>();

            int bytesPerFrame = result.audioFormat.getFrameSize();
            int numBytesRead;
            int numFramesRead;

            // Set an arbitrary buffer size of 1024 frames
            result.bufferSize = 1024 * bytesPerFrame;
            byte[] audioBytes = new byte[result.bufferSize];
            while ((numBytesRead = audioInputStream.read(audioBytes, 0, audioBytes.length)) != -1) {
                // Calculate the number of frames actually read.
                numFramesRead = numBytesRead / bytesPerFrame;
                result.totalFramesRead += numFramesRead;

                // Add bytes array to ArrayList
                result.array.add(audioBytes);

                // Create a new empty bytes array
                audioBytes = new byte[result.bufferSize];
            }
        } catch (IOException | UnsupportedAudioFileException e) {
            result.exception = e;
        }
        return result;
    }

    private static String formatTime(int seconds) {
        if (seconds <= 0) return String.format("00:00");
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String stringifyArray(Object array) {
        if (array instanceof byte[])
            return Arrays.toString((byte[]) array);
        else if (array instanceof short[])
            return Arrays.toString((short[]) array);
        else if (array instanceof int[])
            return Arrays.toString((int[]) array);
        else if (array instanceof double[])
            return Arrays.toString((double[]) array);
        return null;
    }

    private static void shortDebugArrayList(ArrayList arrayList) {
        int mid = arrayList.size() / 2;
        int last = arrayList.size() - 1;
        int lastLen = String.valueOf(last).length();

        String zero = String.format("%0" + lastLen + "d", 0);
        String mids = String.format("%0" + lastLen + "d", mid);

        System.out.println(zero + ": " + stringifyArray(arrayList.get(0)));
        System.out.println(mids + ": " + stringifyArray(arrayList.get(mid)));
        System.out.println(last + ": " + stringifyArray(arrayList.get(last)));
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}