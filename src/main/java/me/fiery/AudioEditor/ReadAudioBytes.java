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
    void run(ReadAudioBytesResult readAudioBytesResults);
}

class ReadAudioBytesResult {
    Exception exception = null;
    AudioFormat audioFormat = null;
    ArrayList<byte[]> array = null;
    int numBytes = 0;
    long totalFramesRead = 0;
}

class ReadAudioBytes implements Runnable {
    private File file;
    private ReadAudioBytesCallback callback;

    ReadAudioBytes(File file, ReadAudioBytesCallback callback) {
        this.file = file;
        this.callback = callback;
    }

    public void run () {
        ReadAudioBytesResult result = new ReadAudioBytesResult();

        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(this.file);
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

        this.callback.run(result);
    }
}