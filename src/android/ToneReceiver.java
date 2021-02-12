package com.cellules.cordova.audiofrequency;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;

public class ToneReceiver extends Thread {

    private int sampleRateInHz = 44100;

    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;

    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

    private AudioRecord recorder;

    private Handler handler;

    private Message message;

    private Bundle messageBundle = new Bundle();

    public ToneReceiver() {
        // use the mic with Auto Gain Control turned off
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateInHz, channelConfig, audioFormat, bufferSize);
    }

    public ToneReceiver(int bufferSizeInBytes) {
        if (bufferSizeInBytes > bufferSize) {
            bufferSize = bufferSizeInBytes;
        }

        // use the mic with Auto Gain Control turned off
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateInHz, channelConfig, audioFormat, bufferSize);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        int numReadBytes = 0;
        short audioBuffer[] = new short[bufferSize];
        DoubleFFT_1D fft = new DoubleFFT_1D(bufferSize);

        synchronized(this)
        {
            recorder.startRecording();

            while (!isInterrupted()) {
                numReadBytes = recorder.read(audioBuffer, 0, bufferSize);

                if (numReadBytes > 0) {
                    // Convert samples to double
                    double[] samples = new double[bufferSize];
                    for (int i = 0; i < bufferSize; i++) {
                        samples[i] = (double) audioBuffer[i];
                    }

                    // window the samples
                    samples = hammingWindow(samples);

                    // copying audio data to the fft data buffer, imaginary part is 0
                    double[] fftData = new double[bufferSize * 2];
                    for (int i = 0; i < bufferSize; i++) {
                        fftData[2*i] = samples[i];
                        fftData[2*i+1] = 0;
                    }

                    // FFT compute
                    fft.complexForward(fftData);

                    // magnitudes
                    double[] magnitude = magnitude(fftData);

                    // Get the largest magnitude peak
                    int peakIndex = peakIndex(magnitude, 0, magnitude.length );

                    // gets frequency value for peak index
                    double frequency = calculateFrequency(peakIndex);

                    int freq2 = (magnitude.length)-1;
                    int freq3 = (magnitude.length/2);

                    double frequency2 = calculateFrequency(freq2);
                    double frequency3 = calculateFrequency(freq3);

                    //calculamos los índices para las frecuencias que queremos obtener. Rango entre 17.000 - 18.000

                    
                    //Log.i("frequency: ", "magnitude: " + magnitude[1] + " fftData: " + fftData.length);
                    //Log.i("frequency: ", "magnitude 2: " + magnitude[2] + " fftData 2: " + fftData[2]);
                    Log.i("frecuencia1: ", ": " + Math.round(frequency));
                    //Log.i("frecuencia 2: ", ": " + freq2 + " value: " + Math.round(frequency2));
                    //Log.i("frecuencia 3: ", ": " + freq3 + " value: " + Math.round(frequency3));
                    //Log.i("sizes", " bufferSize: " + bufferSize + " fftData: " + fftData.length + " magnitude: " + magnitude.length);
                    Log.i("sample rate :" , "" + getIndex(11025));

                    int start = getIndex(16000);
                    int end = getIndex(18050);
                    int peakIndex2 = peakIndex(magnitude, start, end );
                    double frequency4 = calculateFrequency(peakIndex2);
                    Log.i("frecuencia rango: ", " peakIndex2: " + peakIndex2 + " value: " + Math.round(frequency4));

                    int end2 = getIndex(18100);
                    int peakIndex3 = peakIndex(magnitude, start, end2 );
                    double frequency5 = calculateFrequency(peakIndex3);
                    Log.i("frecuencia rango 2: ", " peakIndex3: " + peakIndex3 + " value: " + Math.round(frequency5));


                    // send frequency to handler
                    message = handler.obtainMessage();
                    messageBundle.putLong("frequency", Math.round(frequency));
                    message.setData(messageBundle);
                    handler.sendMessage(message);
                }
            }

            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }

            recorder.release();
            recorder = null;
        }
    }

    // Hann(ing) window
    // private double[] hannWindow(double[] samples) {
    //     for (int index = 0; index < samples.length; index++) {
    //         samples[index] *= 0.5 * (1 - (double) Math.cos(2 * Math.PI * index / (samples.length - 1)));
    //     }
    //     return samples;
    // }

    // Hamming window
    private double[] hammingWindow(double[] samples) {
        for (int index = 0; index < samples.length; index++) {
            samples[index] *= 0.54 - 0.46 * (double) Math.cos(2 * Math.PI * index / (samples.length - 1));
        }
        return samples;
    }

    // Blackman window
    // private double[] blackmanWindow(double[] samples) {
    //     for (int index = 0; index < samples.length; index++) {
    //         samples[index] *= 0.42 - 0.5 * (double) Math.cos(2 * Math.PI * index / (samples.length - 1)) + 0.08 * (double) Math.cos(4 * Math.PI * index / (samples.length - 1));
    //     }
    //     return samples;
    // }

    private double[] magnitude(double[] realData) {
         double[] magnitude = new double[bufferSize / 2];
         for (int i = 0; i < magnitude.length; i++) {
             double R = realData[2*i];
             double I = realData[2*i+1];
             // complex numbers -> vectors
             magnitude[i] = Math.sqrt(I*I + R*R);
         }
         return magnitude;
    }

    private int peakIndex(double[] data, int start, int end) {
        int peakIndex = start;
        double peak = data[start];
        //for(int i = 0; i < data.length; i++){
        for(int i = start; i < end; i++){
            if(peak < data[i]) {
                peak = data[i];
                peakIndex = i;
            }
        }
        return peakIndex;
    }

    private double calculateFrequency(double index) {
        return sampleRateInHz * index / bufferSize;
    }

    private int getIndex(int sampleHz){
        int rateHz = sampleRateInHz / 2;
        int mag = bufferSize / 2;

        return(sampleHz * mag) / rateHz;
    }
}
