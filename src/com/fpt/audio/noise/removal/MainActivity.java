package com.fpt.audio.noise.removal;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final int RECORDER_BPP = 16;
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	int bufferSizeInBytes;
	int numberOfReadBytes = 0;
	byte audioBuffer[];
	boolean recording = false;
	float tempFloatBuffer[] = new float[3];
	int tempIndex = 0;
	int totalReadBytes = 0;
	byte totalByteBuffer[] = new byte[60 * 44100 * 2];
	float totalAbsValue = 0.0f;
	short sample = 0;
	boolean save = true;
	// public static int THRESHOLD = 1000;
	// private static long currentTime = 0;
	// private static final int DUARATION = 1200;
	// private static final int TIMEOUT = 5000;
	// public static long timeStartRecord = 0;
	private static AudioRecord audioRecorder;
	Button btStart, btStop;
	TextView tvLog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btStart = (Button) findViewById(R.id.btnStart);
		btStop = (Button) findViewById(R.id.btnStop);
		tvLog = (TextView) findViewById(R.id.tv_log);
		btStart.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				tvLog.setText("");
//				try {
//					ByteArrayOutputStream out = new ByteArrayOutputStream();
//					BufferedInputStream in = new BufferedInputStream(
//							new FileInputStream(new File(
//									"/mnt/sdcard/test1.wav")));
//
//					int read;
//					byte[] buff = new byte[1024];
//					while ((read = in.read(buff)) > 0) {
//						out.write(buff, 0, read);
//					}
//					out.flush();
//					byte[] audioBytes = out.toByteArray();
//					int length = audioBytes.length;
//					String s="";
//					for (int i=0; i<length; i++) 
//						s = s+audioBytes[i] +"\n";//Log.e("abc", audioBytes[i] +"");//addLog(audioBytes[i]+"");
//					writeToFile("abc", s);
//				} catch (Exception e) {
//				}
				 start();
			}
		});
		btStop.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 stopRecord("/mnt/sdcard/test1111.wav");
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public  void writeToFile(String fileName, String body)
    {
        FileOutputStream fos = null;

        try {
            final File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" );

//            if (!dir.exists())
//            {
//                dir.mkdirs(); 
//            }

            final File myFile = new File(dir, fileName + ".txt");

            if (!myFile.exists()) 
            {    
                myFile.createNewFile();
            } 

            fos = new FileOutputStream(myFile);

            fos.write(body.getBytes());
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
	public void start() {
		save = true;
		totalReadBytes = 0;
		// Get the minimum buffer size required for the successful creation of
		// an AudioRecord object.
		bufferSizeInBytes = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
		// Initialize Audio Recorder.
		audioRecorder = new AudioRecord(
				MediaRecorder.AudioSource.VOICE_RECOGNITION,
				RECORDER_SAMPLERATE, RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, bufferSizeInBytes);
		audioBuffer = new byte[bufferSizeInBytes];
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		am.setParameters("noise_suppression=auto");
		// NoiseSuppressor.create(audioRecorder.getAudioSessionId());
		// Start Recording.
		try {
			audioRecorder.startRecording();
		} catch (IllegalStateException e) {
			return;
		}
		save = false;
		numberOfReadBytes = 0;
		audioBuffer = new byte[bufferSizeInBytes];
		recording = false;
		tempFloatBuffer = new float[3];
		tempIndex = 0;
		totalReadBytes = 0;
		totalByteBuffer = new byte[60 * 44100 * 2];
		new Thread(new Runnable() {
			@Override
			public void run() {
				// While data come from microphone.
				while (!save) {
					numberOfReadBytes = audioRecorder.read(audioBuffer, 0,
							bufferSizeInBytes);
					// Analyze Sound.
					for (int i = 0; i < bufferSizeInBytes; i += 2) {
						sample = (short) ((audioBuffer[i]) | audioBuffer[i + 1] << 8);
						totalAbsValue += Math.abs(sample)
								/ (numberOfReadBytes / 2);
					}
					// Analyze temp buffer.
					tempFloatBuffer[tempIndex % 3] = totalAbsValue;
					float temp = 0.0f;
					for (int i = 0; i < 3; ++i)
						temp += tempFloatBuffer[i];
					// addLog("totalAbsValue: " + totalAbsValue + ", temp: "
					// + temp);
					// addLog("numberOfReadBytes: " + numberOfReadBytes);
					for (int i = 0; i < numberOfReadBytes; i++) {
						totalByteBuffer[totalReadBytes + i] = audioBuffer[i];
						// if (totalReadBytes+i < 200)
						// addLog("i: " + totalReadBytes+i +", finalBuffer: " +
						// audioBuffer[i]);
					}
					// addLog("totalReadBytes: " + totalReadBytes);
					totalReadBytes += numberOfReadBytes;
					tempIndex++;
				}
				audioRecorder.stop();
				audioRecorder.release();
			}
		}).start();

	}

	private void stopRecord(String filePath) {
		if (!save) {
			save = true;
			// Save audio to file.
			long totalAudioLen = 0;
			long totalDataLen = totalAudioLen + 36;
			long longSampleRate = RECORDER_SAMPLERATE;
			int channels = 1;
			long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;
			totalAudioLen = totalReadBytes;
			totalDataLen = totalAudioLen + 36;
			byte finalBuffer[] = new byte[totalReadBytes + 44];

			finalBuffer[0] = 'R'; // RIFF/WAVE header
			finalBuffer[1] = 'I';
			finalBuffer[2] = 'F';
			finalBuffer[3] = 'F';
			finalBuffer[4] = (byte) (totalDataLen & 0xff);
			finalBuffer[5] = (byte) ((totalDataLen >> 8) & 0xff);
			finalBuffer[6] = (byte) ((totalDataLen >> 16) & 0xff);
			finalBuffer[7] = (byte) ((totalDataLen >> 24) & 0xff);
			finalBuffer[8] = 'W';
			finalBuffer[9] = 'A';
			finalBuffer[10] = 'V';
			finalBuffer[11] = 'E';
			finalBuffer[12] = 'f'; // 'fmt ' chunk
			finalBuffer[13] = 'm';
			finalBuffer[14] = 't';
			finalBuffer[15] = ' ';
			finalBuffer[16] = 16; // 4 bytes: size of 'fmt ' chunk
			finalBuffer[17] = 0;
			finalBuffer[18] = 0;
			finalBuffer[19] = 0;
			finalBuffer[20] = 1; // format = 1
			finalBuffer[21] = 0;
			finalBuffer[22] = (byte) channels;
			finalBuffer[23] = 0;
			finalBuffer[24] = (byte) (longSampleRate & 0xff);
			finalBuffer[25] = (byte) ((longSampleRate >> 8) & 0xff);
			finalBuffer[26] = (byte) ((longSampleRate >> 16) & 0xff);
			finalBuffer[27] = (byte) ((longSampleRate >> 24) & 0xff);
			finalBuffer[28] = (byte) (byteRate & 0xff);
			finalBuffer[29] = (byte) ((byteRate >> 8) & 0xff);
			finalBuffer[30] = (byte) ((byteRate >> 16) & 0xff);
			finalBuffer[31] = (byte) ((byteRate >> 24) & 0xff);
			finalBuffer[32] = (byte) (2 * 16 / 8); // block align
			finalBuffer[33] = 0;
			finalBuffer[34] = RECORDER_BPP; // bits per sample
			finalBuffer[35] = 0;
			finalBuffer[36] = 'd';
			finalBuffer[37] = 'a';
			finalBuffer[38] = 't';
			finalBuffer[39] = 'a';
			finalBuffer[40] = (byte) (totalAudioLen & 0xff);
			finalBuffer[41] = (byte) ((totalAudioLen >> 8) & 0xff);
			finalBuffer[42] = (byte) ((totalAudioLen >> 16) & 0xff);
			finalBuffer[43] = (byte) ((totalAudioLen >> 24) & 0xff);
			String s = "";
			addLog(totalReadBytes +"");
			for (int i = 0; i < totalReadBytes; ++i) {
				finalBuffer[44 + i] = totalByteBuffer[i];
				// s = "i: "+ i + ",value: "+ totalByteBuffer[i] +'\n';
			}
			// addLog(s);
			FileOutputStream out;
			try {
				out = new FileOutputStream(filePath);
				try {
					out.write(finalBuffer);
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private void addLog(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String old = tvLog.getText().toString();
				if ((old != null) && (!TextUtils.isEmpty(old))) {
					old = old + "\n";
				}
				tvLog.setText(old + text);
			}
		});
	}
}
