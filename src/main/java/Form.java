import javax.swing.*;

public class Form {

    JPanel parentPanel;
    JTextField pathTextField;
    JButton browseButton;
    JButton playButton;
    JButton stopButton;
    JButton readButton;
    JPanel audioBytesPanel;
    JScrollPane audioBytesScrollPane;
    JTextArea audioBytesTextArea;

    void setPlaying(boolean isPlaying) {
        playButton.setEnabled(!isPlaying);
        stopButton.setEnabled(isPlaying);
    }

    void showExceptionDialog (String message) {
        JOptionPane.showMessageDialog(parentPanel, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
