package net.morimori0317.ikisugilibvlc;


import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.callback.DefaultAudioCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;


public class Main {
    private static final String MEDIA_MRL = "https://cdn.discordapp.com/attachments/358878159615164416/1102695794475274350/nicovideo-sm20295725_1afd21670994d7069dfa1bb794b5f9e3b8116a9bf55e97e0d37de66db3809bc1.mp4";
    private static final int SAMPLE_RATE = 48000;
    private static final boolean SPATIAL = true;
    private static final int CHANNELS = SPATIAL ? 1 : 2;
    private static final int BIT = 16;
    private static int SOURCE;

    public static void main(String[] args) throws Exception {
        alInit();

        MediaPlayerFactory factory = new MediaPlayerFactory();
        EmbeddedMediaPlayer mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        mediaPlayer.audio().callback("S16N", SAMPLE_RATE, CHANNELS, new DefaultAudioCallbackAdapter(BIT / 8 * CHANNELS) {

            @Override
            protected void onPlay(MediaPlayer mediaPlayer, byte[] data, int sampleCount, long pts) {
                alUpdate(data);
            }

            @Override
            public void pause(MediaPlayer mediaPlayer, long pts) {
                System.out.println("Pause!");
            }

            @Override
            public void resume(MediaPlayer mediaPlayer, long pts) {
                System.out.println("Resume!");
            }

            @Override
            public void flush(MediaPlayer mediaPlayer, long pts) {
                System.out.println("Flush!");
            }

            @Override
            public void drain(MediaPlayer mediaPlayer) {
                System.out.println("Drain!");
            }

            @Override
            public void setVolume(float volume, boolean mute) {

            }
        });

        mediaPlayer.media().play(MEDIA_MRL);

        while (true) {
            Thread.sleep(10);
            alListenerUpdate();
        }
    }

    private static void alListenerUpdate() {
        float x = 10f;
        float y = 10f;

        float rot = (float) (Math.PI * 2f * ((float) (System.currentTimeMillis() % 10000) / 10000f));
        float rx = (float) (x * Math.cos(rot) - y * Math.sin(rot));
        float ry = (float) (x * Math.sin(rot) + y * Math.cos(rot));

        alListener3f(AL_POSITION, rx, 0, ry);
    }

    private static void alInit() {
        long device = alcOpenDevice((ByteBuffer) null);
        ALCCapabilities capabilities = ALC.createCapabilities(device);
        long context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(capabilities);

        SOURCE = alGenSources();
        alSourcei(SOURCE, AL_LOOPING, AL_FALSE);
        alSourcei(SOURCE, AL_SOURCE_RELATIVE, SPATIAL ? AL_FALSE : AL_TRUE);

        if (SPATIAL) {
            alSourcei(SOURCE, AL_DISTANCE_MODEL, 10);
            alSourcef(SOURCE, AL_MAX_DISTANCE, 10);
            alSourcef(SOURCE, AL_ROLLOFF_FACTOR, 1.0F);
            alSourcef(SOURCE, AL_REFERENCE_DISTANCE, 0.0F);
        }

        alSource3f(SOURCE, AL_POSITION, 0, 0, 0);
    }

    private static void alUpdate(byte[] bufferData) {
        int buffer = alGenBuffers();

        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bufferData.length);
        byteBuffer.put(bufferData);
        byteBuffer.flip();

        alBufferData(buffer, getFormat(CHANNELS, BIT), byteBuffer, SAMPLE_RATE);
        alSourceQueueBuffers(SOURCE, buffer);

        if (alGetSourcei(SOURCE, AL_SOURCE_STATE) == AL_INITIAL) {
            alSourcePlay(SOURCE);
        }

        alSourceUnqueueBuffers(SOURCE);
    }

    private static int getFormat(int channel, int bit) {
        if (channel == 1) {
            if (bit == 8) {
                return AL_FORMAT_MONO8;
            }
            if (bit == 16) {
                return AL_FORMAT_MONO16;
            }
        } else if (channel == 2) {
            if (bit == 8) {
                return AL_FORMAT_STEREO8;
            }

            if (bit == 16) {
                return AL_FORMAT_STEREO16;
            }
        }

        throw new RuntimeException("Unsupported format");
    }
}