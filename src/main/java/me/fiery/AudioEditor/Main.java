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
    int numBytes = 0;
    long totalFramesRead = 0;

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
                if (audioBytesPlayer != null) {
                    audioBytesPlayer.stop();
                    audioBytesPlayer = null;
                }

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

            isReading = true;
            String oldText = form.readButton.getText();
            toggleButton(form.readButton, "Reading...");

            new Thread(() -> {
                readAudioBytesResult = readAudioBytes(file);

                if (readAudioBytesResult.exception == null) {
                    long totalBytes = 0;
                    for (byte[] audioBytes : readAudioBytesResult.array)
                        totalBytes += (long) audioBytes.length;

                    long size = file.length();

                    String text = "";
                    text += "Directory: " + file.getParent() + "\n";
                    text += "File: " + file.getName() + "\n";
                    text += "Size: " + size + " bytes (" + humanReadableByteCount(size, false) + ")\n";
                    text += "Debug:\n";
                    text += "    arraySize  : " + readAudioBytesResult.array.size() + "\n";
                    text += "    frameSize  : " + readAudioBytesResult.audioFormat.getFrameSize() + "\n";
                    text += "    numBytes   : " + readAudioBytesResult.numBytes + "\n";
                    text += "    framesRead : " + readAudioBytesResult.totalFramesRead + "\n";
                    text += "    totalBytes : " + totalBytes + "\n";
                    text += "\n";

                    form.audioBytesTextArea.append(text);

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
                        readAudioBytesResult.array,
                        readAudioBytesResult.audioFormat.getSampleSizeInBits(),
                        readAudioBytesResult.audioFormat.getChannels(),
                        readAudioBytesResult.audioFormat.isBigEndian()
                );
                shortDebugArrayList(paired);

                // Fourier Transform is too confusing
                final int DURATION = 5;
                double volumeStep = 1.0 / (readAudioBytesResult.audioFormat.getFrameRate() * DURATION);
                double volume = 1.0;

                boolean left = true;
                for (short[] pair : paired) {
                    for (int i = 0; i < pair.length; i += 2) {
                        if (left) {
                            pair[i] = (short) (pair[i] * volume);
                            pair[i + 1] = (short) (pair[i + 1] * (1.0 - volume));
                        } else {
                            pair[i] = (short) (pair[i] * (1.0 - volume));
                            pair[i + 1] = (short) (pair[i + 1] * volume);
                        }

                        volume -= volumeStep;

                        if (volume <= 0) {
                            left = !left;
                            volume = 1.0;
                        }
                    }
                }
                shortDebugArrayList(paired);

                ArrayList<byte[]> severed = BytesUtil.sever(
                        paired,
                        readAudioBytesResult.audioFormat.getSampleSizeInBits(),
                        readAudioBytesResult.audioFormat.getChannels(),
                        readAudioBytesResult.audioFormat.isBigEndian()
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
        form.clearButton.addActionListener(actionEvent -> form.audioBytesTextArea.setText(""));
    }

    private static void setGUIReadyState(boolean isReady) {
        form.playButton.setText("Play");
        form.playButton.setEnabled(isReady);
        form.stopButton.setEnabled(isReady);
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
            AudioFormat audioFormat = audioInputStream.getFormat();
            System.out.println(audioFormat);

            // We want uniform 16-bit, stereo, PCM_SIGNED
            final int ch = audioFormat.getChannels();
            final float rate = audioFormat.getSampleRate();
            audioFormat = new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
            audioInputStream = AudioSystem.getAudioInputStream(audioFormat, audioInputStream);
            System.out.println(audioFormat);

            // Container for all buffers of 1024 frames
            ArrayList<byte[]> arrayList = new ArrayList<>();

            int bytesPerFrame = audioFormat.getFrameSize();
            int numBytesRead;
            int numFramesRead;
            long totalFramesRead = 0;

            // Set an arbitrary buffer size of 1024 frames
            int numBytes = 1024 * bytesPerFrame;
            byte[] audioBytes = new byte[numBytes];
            while ((numBytesRead = audioInputStream.read(audioBytes, 0, audioBytes.length)) != -1) {
                // Calculate the number of frames actually read.
                numFramesRead = numBytesRead / bytesPerFrame;
                totalFramesRead += numFramesRead;

                // Add bytes array to ArrayList
                arrayList.add(audioBytes);

                // Create a new empty bytes array
                audioBytes = new byte[numBytes];
            }

            result.audioFormat = audioFormat;
            result.array = arrayList;
            result.numBytes = numBytes;
            result.totalFramesRead = totalFramesRead;
        } catch (IOException | UnsupportedAudioFileException e) {
            result.exception = e;
        }
        return result;
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