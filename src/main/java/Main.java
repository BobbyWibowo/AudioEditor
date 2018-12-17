import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.util.ArrayList;

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
                ArrayList<byte[]> audioBytes = readAudioBytes(selectedFile);

                long totalBytes = 0;
                for (byte[] audioByteArr : audioBytes) {
                    // System.out.println(Arrays.toString(audioByte));
                    totalBytes += (long) audioByteArr.length;
                }

                JTextArea textArea = form.audioBytesTextArea;
                textArea.append("Directory: " + selectedFile.getParent() + "\n");
                textArea.append("File: " + selectedFile.getName() + "\n");
                long size = selectedFile.length();
                textArea.append("Size: " + size + " bytes (" + humanReadableByteCount(size, false) + ")\n");
                textArea.append("Debug:\n");
                textArea.append("    audioBytes.size(): " + audioBytes.size() + "\n");
                textArea.append("    audioBytes.get(0).length: " + audioBytes.get(0).length + "\n");
                textArea.append("    totalBytes: " + totalBytes + "\n\n");
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
                e.printStackTrace();
            }
        });

        form.stopButton.addActionListener(actionEvent -> {
            if (playing == null) return;
            form.setPlaying(false);
            playing.stop();
            playing = null;
        });
    }

    private static ArrayList<byte[]> readAudioBytes(File file) throws IOException, UnsupportedAudioFileException {
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
            byte[] audioBytesClone = audioBytes.clone();
            arrayList.add(audioBytesClone);
        }

        // System.out.println("totalFramesRead: " + totalFramesRead);
        return arrayList;
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
