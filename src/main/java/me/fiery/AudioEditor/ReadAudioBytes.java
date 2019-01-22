package me.fiery.AudioEditor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

interface ReadAudioBytesCallback {
    void run(ReadAudioBytesResults readAudioBytesResults);
}

class ReadAudioBytesResults {
    Exception exception = null;
    AudioFormat audioFormat;
    ArrayList<byte[]> array;
    int numBytes;
    int totalFramesRead;
}

class ReadAudioBytes implements Runnable {
    private File file;
    private ReadAudioBytesCallback callback;

    ReadAudioBytes(File file, ReadAudioBytesCallback callback) {
        this.file = file;
        this.callback = callback;
    }

    public void run () {
        ReadAudioBytesResults results = new ReadAudioBytesResults();

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(this.file);
            AudioFormat audioFormat = audioInputStream.getFormat();
            System.out.println(audioFormat);

            // If not PCM_SIGNED, convert
            if (audioFormat.getEncoding() != PCM_SIGNED) {
                final int ch = audioFormat.getChannels();
                final float rate = audioFormat.getSampleRate();
                audioFormat = new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
                System.out.println(audioFormat);
                audioInputStream = AudioSystem.getAudioInputStream(audioFormat, audioInputStream);
            }

            int bytesPerFrame = audioFormat.getFrameSize();
            /* NOTE: I don't think this will ever happen since we convert to PCM_SIGNED prior
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                // some audio formats may have unspecified frame size
                // in that case we may read any amount of bytes
                bytesPerFrame = 1;
            }
            */

            int totalFramesRead = 0;
            ArrayList<byte[]> arrayList = new ArrayList<>();
            // Set an arbitrary buffer size of 1024 frames.
            int numBytesRead;
            int numFramesRead;

            // Try to read numBytes bytes from the file.
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

            results.audioFormat = audioFormat;
            results.array = arrayList;
            results.numBytes = numBytes;
            results.totalFramesRead = totalFramesRead;
        } catch (IOException | UnsupportedAudioFileException e) {
            results.exception = e;
        }

        this.callback.run(results);
    }
}