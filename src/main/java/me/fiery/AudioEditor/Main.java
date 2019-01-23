package me.fiery.AudioEditor;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {

    private static File file = null;
    private static AudioBytesPlayer audioBytesPlayer = null;
    private static boolean isReading = false;

    public static void main(String[] args) {
        Form form = new Form();

        JFrame frame = new JFrame("Form");
        frame.setTitle("AudioEditor");
        frame.setContentPane(form.parentPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        form.browseButton.addActionListener(actionEvent -> {
            // Create file chooser
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
                form.setReady(false);
            }
        });

        form.readButton.addActionListener(actionEvent -> {
            if (isReading)
                return;

            if (file == null) {
                form.showExceptionDialog("Please select a file to read!");
                return;
            }

            isReading = true;
            form.readButton.setText("Reading...");
            form.readButton.setEnabled(false);

            new Thread(new ReadAudioBytes(file, result -> {
                if (result.exception == null) {

                    long totalBytes = 0;
                    for (byte[] audioBytes : result.array)
                        totalBytes += (long) audioBytes.length;

                    long size = file.length();

                    String text = "";
                    text += "Directory: " + file.getParent() + "\n";
                    text += "File: " + file.getName() + "\n";
                    text += "Size: " + size + " bytes (" + humanReadableByteCount(size, false) + ")\n";
                    text += "Debug:\n";
                    text += "    arraySize  : " + result.array.size() + "\n";
                    text += "    frameSize  : " + result.audioFormat.getFrameSize() + "\n";
                    text += "    numBytes   : " + result.numBytes + "\n";
                    text += "    framesRead : " + result.totalFramesRead + "\n";
                    text += "    totalBytes : " + totalBytes + "\n";
                    text += "\n";

                    form.audioBytesTextArea.append(text);

                    try {
                        form.setReady(true);
                        audioBytesPlayer = new AudioBytesPlayer(result.audioFormat, result.array);
                    } catch (LineUnavailableException e) {
                        e.printStackTrace();
                        form.showExceptionDialog(e.getMessage());
                    }

                    // Debugging arrays
                    shortDebugArrayList(result.array);
                    ArrayList<short[]> paired = BytesVoodoo.pair(
                            result.array,
                            result.audioFormat.getFrameSize(),
                            result.audioFormat.getChannels(),
                            result.audioFormat.isBigEndian()
                    );
                    shortDebugArrayList(paired);
                    /*
                    ArrayList<byte[]> severed = BytesVoodoo.sever(
                            paired,
                            result.audioFormat.getFrameSize(),
                            result.audioFormat.getChannels(),
                            result.audioFormat.isBigEndian()
                    );
                    shortDebugArrayList(severed);
                    */
                } else {
                    if (!(result.exception instanceof UnsupportedAudioFileException))
                        result.exception.printStackTrace();
                    form.showExceptionDialog(result.exception.getMessage());
                }

                isReading = false;
                form.readButton.setText("Read");
                form.readButton.setEnabled(true);
            })).start();
        });

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

        form.stopButton.addActionListener(actionEvent -> {
            if (audioBytesPlayer == null)
                return;

            audioBytesPlayer.stop();
            form.stopButton.setEnabled(false);
            form.playButton.setText("Play");
        });

        form.clearButton.addActionListener(actionEvent -> form.audioBytesTextArea.setText(""));
    }

    private static String stringifyArray(Object array) {
        if (array instanceof byte[])
            return Arrays.toString((byte[]) array);
        else if (array instanceof short[])
            return Arrays.toString((short[]) array);
        else if (array instanceof int[])
            return Arrays.toString((int[]) array);
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