package me.fiery.AudioEditor;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

class AudioBytesPlayerThread implements Runnable {

    static final int STOPPED = 0;
    static final int PLAYING = 1;
    static final int PAUSED = 2;

    private ArrayList<byte[]> array;
    private AudioFormat format;
    private SourceDataLine line;
    private int state = STOPPED;

    AudioBytesPlayerThread(ArrayList<byte[]> bytesArray, AudioFormat audioFormat, SourceDataLine sourceDataLine) {
        this.array = bytesArray;
        this.format = audioFormat;
        this.line = sourceDataLine;
    }

    @Override
    public void run() {
        try {
            line.open(format);
            line.start();
            this.state = PLAYING;
            this.stream();
            line.stop();
            line.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stream() {
        for (byte[] buffer : this.array) {
            if (this.state == STOPPED)
                return;
            // If it's stupid but it works, it ain't stupid
            while (this.state == PAUSED) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            line.write(buffer, 0, buffer.length);
        }
    }

    int getState() {
        return state;
    }

    void setState(int state) {
        this.state = state;
    }

}

class AudioBytesPlayer {

    private AudioBytesPlayerThread audioBytesPlayerThread;
    private Thread thread = null;

    AudioBytesPlayer(AudioFormat audioFormat, ArrayList<byte[]> bytesArray) throws LineUnavailableException {
        Info info = new Info(SourceDataLine.class, audioFormat);
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        audioBytesPlayerThread = new AudioBytesPlayerThread(bytesArray, audioFormat, sourceDataLine);
    }

    int getState() {
        return audioBytesPlayerThread.getState();
    }

    void play() {
        if (audioBytesPlayerThread.getState() == AudioBytesPlayerThread.STOPPED) {
            thread = new Thread(this.audioBytesPlayerThread);
            thread.start();
        } else {
            audioBytesPlayerThread.setState(AudioBytesPlayerThread.PLAYING);
        }
    }

    void pause() {
        audioBytesPlayerThread.setState(AudioBytesPlayerThread.PAUSED);
    }

    void stop()  {
        audioBytesPlayerThread.setState(AudioBytesPlayerThread.STOPPED);
        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}