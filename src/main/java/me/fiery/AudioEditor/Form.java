package me.fiery.AudioEditor;

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
    JButton clearButton;

    void setReady(boolean isReady) {
        playButton.setEnabled(isReady);
        stopButton.setEnabled(isReady);
    }

    void showExceptionDialog (String message) {
        JOptionPane.showMessageDialog(parentPanel, message, "An error occurred!", JOptionPane.ERROR_MESSAGE);
    }
}
