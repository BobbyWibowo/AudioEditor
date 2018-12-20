import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

class ReadAudioBytesResults {
    ArrayList<byte[]> array;
    int bytesPerFrame;
    int numBytes;
    int totalFramesRead;
}

public class Main {

    private static File selectedFile;
    private static AudioFilePlayer playing;

    public static void main(String[] args) {
        // Form
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
            int returnVal = fileChooser.showOpenDialog(form.parentPanel);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                if (playing != null) {
                    playing.stop();
                    playing = null;
                }
                selectedFile = fileChooser.getSelectedFile();
                form.pathTextField.setText(selectedFile.getAbsolutePath());
                form.setPlaying(false);
            }
        });

        form.readButton.addActionListener(actionEvent -> {
            try {
                ReadAudioBytesResults results = readAudioBytes(selectedFile);

                long totalBytes = 0;
                for (byte[] audioBytes : results.array) {
                    // System.out.println(Arrays.toString(audioByte));
                    totalBytes += (long) audioBytes.length;
                }

                long size = selectedFile.length();
                int last = results.array.size() - 1;
                int lastLen = String.valueOf(last).length();
                String zero = String.format("%0" + lastLen + "d", 0);

                String text = "";
                text += "Directory: " + selectedFile.getParent() + "\n";
                text += "File: " + selectedFile.getName() + "\n";
                text += "Size: " + size + " bytes (" + humanReadableByteCount(size, false) + ")\n";
                text += "Debug:\n";
                text += "    array.size()   : " + results.array.size() + "\n";
                text += "    bytesPerFrame  : " + results.bytesPerFrame + "\n";
                text += "    numBytes       : " + results.numBytes + "\n";
                text += "    totalFramesRead: " + results.totalFramesRead + "\n";
                text += "    totalBytes     : " + totalBytes + "\n";
                text += "    " + zero + ": " + Arrays.toString(results.array.get(0)) + "\n";
                text += "    " + last + ": " + Arrays.toString(results.array.get(last)) + "\n\n";

                form.audioBytesTextArea.append(text);
            } catch (IOException | UnsupportedAudioFileException e) {
                if (!(e instanceof UnsupportedAudioFileException)) e.printStackTrace();
                form.showExceptionDialog(e.getMessage());
            }
        });

        form.playButton.addActionListener(actionEvent -> {
            try {
                form.setPlaying(true);
                playing = new AudioFilePlayer(selectedFile);
                playing.play();
            } catch (IOException | LineUnavailableException | UnsupportedAudioFileException e) {
                if (!(e instanceof UnsupportedAudioFileException)) e.printStackTrace();
                form.showExceptionDialog(e.getMessage());
            }
        });

        form.stopButton.addActionListener(actionEvent -> {
            if (playing == null) return;
            form.setPlaying(false);
            playing.stop();
            playing = null;
        });
    }

    private static ReadAudioBytesResults readAudioBytes(File file) throws IOException, UnsupportedAudioFileException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat audioFormat = audioInputStream.getFormat();

        int bytesPerFrame = audioFormat.getFrameSize();
        if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
            // some audio formats may have unspecified frame size
            // in that case we may read any amount of bytes
            bytesPerFrame = 1;
        }

        int totalFramesRead = 0;
        ArrayList<byte[]> arrayList = new ArrayList<>();
        // Set an arbitrary buffer size of 1024 frames.
        int numBytesRead;
        int numFramesRead;

        // Try to read numBytes bytes from the file.
        int numBytes = 1024 * bytesPerFrame;
        byte[] audioBytes = new byte[numBytes];
        while ((numBytesRead = audioInputStream.read(audioBytes)) != -1) {
            // Calculate the number of frames actually read.
            numFramesRead = numBytesRead / bytesPerFrame;
            totalFramesRead += numFramesRead;

            // Clone bytes array then add to ArrayList
            // byte[] audioBytesClone = audioBytes.clone();
            arrayList.add(audioBytes);

            // Create new empty array over the buffer array
            audioBytes = new byte[numBytes];
        }

        ReadAudioBytesResults results = new ReadAudioBytesResults();
        results.array = arrayList;
        results.bytesPerFrame = bytesPerFrame;
        results.numBytes = numBytes;
        results.totalFramesRead = totalFramesRead;
        return results;
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
