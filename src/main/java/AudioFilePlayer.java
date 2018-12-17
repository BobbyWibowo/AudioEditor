import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import static javax.sound.sampled.AudioSystem.getAudioInputStream;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

class Playing implements Runnable {
    private boolean stopped = false;
    private AudioInputStream stream;
    private AudioFormat format;
    private SourceDataLine line;

    Playing(AudioInputStream stream, AudioFormat format, SourceDataLine line) {
        this.stream = stream;
        this.format = format;
        this.line = line;
    }

    public void run() {
        try {
            line.open(format);
            line.start();
            this.stream();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    void stop() {
        this.stopped = true;
    }

    private void stream() throws IOException {
        final byte[] buffer = new byte[4096];
        final AudioInputStream audioInputStream = getAudioInputStream(this.format, this.stream);
        for (int n = 0; n != -1; n = audioInputStream.read(buffer, 0, buffer.length)) {
            if (!this.stopped) line.write(buffer, 0, n);
        }
    }
}

class AudioFilePlayer {

    private Playing playing;
    private Thread thread;

    AudioFilePlayer(File file) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        AudioInputStream audioInputStream = getAudioInputStream(file);
        AudioFormat _format = audioInputStream.getFormat();
        final int ch = _format.getChannels();
        final float rate = _format.getSampleRate();
        AudioFormat audioFormat = new AudioFormat(PCM_SIGNED, rate, 16, ch, ch * 2, rate, false);
        Info info = new Info(SourceDataLine.class, audioFormat);
        SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
        playing = new Playing(audioInputStream, audioFormat, sourceDataLine);
        thread = new Thread(playing);
    }

    void play() {
        thread.start();
    }

    void stop() {
        playing.stop();
    }
}