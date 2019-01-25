package me.fiery.AudioEditor;

import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

interface OnProgressCallback {
    void run(long streamedBytes, long totalBytes);
}

class AudioBytesPlayerThread implements Runnable {

    static final int STOPPED = 0;
    static final int PLAYING = 1;
    static final int PAUSED = 2;

    private ArrayList<byte[]> arrayList;
    private AudioFormat audioFormat;
    private SourceDataLine sourceDataLine;
    private int state = STOPPED;
    private long totalBytes = 0;
    private OnProgressCallback onProgressCallback;

    AudioBytesPlayerThread(ArrayList<byte[]> arrayList, AudioFormat audioFormat, SourceDataLine sourceDataLine) {
        this.arrayList = arrayList;
        this.audioFormat = audioFormat;
        this.sourceDataLine = sourceDataLine;
        for (byte[] buffer : arrayList)
            totalBytes += buffer.length;
    }

    @Override
    public void run() {
        try {
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
            this.state = PLAYING;
            this.stream();
            sourceDataLine.stop();
            sourceDataLine.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void stream() {
        long streamedBytes = 0;
        for (byte[] buffer : this.arrayList) {
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
            sourceDataLine.write(buffer, 0, buffer.length);
            streamedBytes += buffer.length;
            if (this.onProgressCallback != null)
                this.onProgressCallback.run(streamedBytes, this.totalBytes);
        }
    }

    int getState() {
        return state;
    }

    void setState(int state) {
        this.state = state;
    }

    void setOnProgressCallback(OnProgressCallback onProgressCallback) {
        this.onProgressCallback = onProgressCallback;
    }

}

class AudioBytesPlayer {

    private AudioBytesPlayerThread audioBytesPlayerThread;
    private Thread thread;

    AudioBytesPlayer(AudioFormat audioFormat, ArrayList<byte[]> arrayList) throws LineUnavailableException {
        Info info = new Info(SourceDataLine.class, audioFormat);
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        audioBytesPlayerThread = new AudioBytesPlayerThread(arrayList, audioFormat, sourceDataLine);
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

    void onProgress(OnProgressCallback onProgressCallback) {
        audioBytesPlayerThread.setOnProgressCallback(onProgressCallback);
    }

}