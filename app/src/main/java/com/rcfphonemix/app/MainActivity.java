package com.rcfphonemix.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_AUDIO_REQUEST = 1001;

    private MediaPlayer mediaPlayer;
    private Equalizer equalizer;

    private Button playButton;
    private TextView trackName;
    private LinearLayout eqContainer;

    private final int[] targetFrequencies = {
            80,
            500,
            2000,
            10000
    };

    private final String[] bandNames = {
            "LOW",
            "LOW MID",
            "HIGH MID",
            "HIGH"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildInterface();
    }

    private void buildInterface() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(12, 16, 22));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(30));

        TextView title = new TextView(this);
        title.setText("RCF PHONE MIX");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(20));
        root.addView(title);

        trackName = new TextView(this);
        trackName.setText("No music selected");
        trackName.setTextColor(Color.LTGRAY);
        trackName.setTextSize(16);
        trackName.setGravity(Gravity.CENTER);
        trackName.setPadding(0, dp(12), 0, dp(12));
        root.addView(trackName);

        Button chooseMusicButton = createButton("SELECT MUSIC");
        chooseMusicButton.setOnClickListener(view -> openMusicPicker());
        root.addView(chooseMusicButton);

        playButton = createButton("PLAY");
        playButton.setEnabled(false);
        playButton.setOnClickListener(view -> togglePlayback());
        root.addView(playButton);

        addSectionTitle(root, "4-BAND EQUALIZER");

        eqContainer = new LinearLayout(this);
        eqContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(eqContainer);

        TextView waitingText = new TextView(this);
        waitingText.setText("Select a song to activate the equalizer.");
        waitingText.setTextColor(Color.GRAY);
        waitingText.setTextSize(14);
        waitingText.setPadding(0, dp(10), 0, dp(10));
        eqContainer.addView(waitingText);

        TextView masterLabel = new TextView(this);
        masterLabel.setText("MASTER VOLUME: 100%");
        masterLabel.setTextColor(Color.WHITE);
        masterLabel.setTextSize(16);
        masterLabel.setPadding(0, dp(25), 0, dp(8));
        root.addView(masterLabel);

        SeekBar masterVolume = new SeekBar(this);
        masterVolume.setMax(100);
        masterVolume.setProgress(100);

        masterVolume.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        masterLabel.setText(
                                "MASTER VOLUME: " + progress + "%"
                        );

                        if (mediaPlayer != null) {
                            float volume = progress / 100f;
                            mediaPlayer.setVolume(volume, volume);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );

        root.addView(masterVolume);

        TextView note = new TextView(this);
        note.setText(
                "The equalizer processes music selected and played " +
                "inside RCF Phone Mix."
        );
        note.setTextColor(Color.GRAY);
        note.setTextSize(13);
        note.setPadding(0, dp(25), 0, 0);
        root.addView(note);

        scrollView.addView(root);
        setContentView(scrollView);
    }

    private Button createButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.rgb(34, 105, 170));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(56)
                );

        params.setMargins(0, dp(6), 0, dp(6));
        button.setLayoutParams(params);

        return button;
    }

    private void addSectionTitle(
            LinearLayout parent,
            String text
    ) {
        TextView sectionTitle = new TextView(this);
        sectionTitle.setText(text);
        sectionTitle.setTextColor(Color.rgb(70, 190, 255));
        sectionTitle.setTextSize(19);
        sectionTitle.setPadding(0, dp(28), 0, dp(10));
        parent.addView(sectionTitle);
    }

    private void openMusicPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");

        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (
                requestCode == PICK_AUDIO_REQUEST &&
                resultCode == RESULT_OK &&
                data != null &&
                data.getData() != null
        ) {
            Uri audioUri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                        audioUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            loadMusic(audioUri);
        }
    }

    private void loadMusic(Uri audioUri) {
        releaseAudio();

        trackName.setText("Loading music...");
        playButton.setEnabled(false);
        playButton.setText("PLAY");

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(this, audioUri);

            mediaPlayer.setOnPreparedListener(player -> {
                trackName.setText("Music ready");
                playButton.setEnabled(true);
                initialiseEqualizer();
            });

            mediaPlayer.setOnCompletionListener(player -> {
                playButton.setText("PLAY");

                try {
                    player.seekTo(0);
                } catch (Exception ignored) {
                }
            });

            mediaPlayer.setOnErrorListener(
                    (player, what, extra) -> {
                        Toast.makeText(
                                MainActivity.this,
                                "Unable to play this audio file.",
                                Toast.LENGTH_LONG
                        ).show();

                        trackName.setText("Audio loading failed");
                        playButton.setEnabled(false);

                        return true;
                    }
            );

            mediaPlayer.prepareAsync();

        } catch (Exception exception) {
            trackName.setText("Audio loading failed");
            playButton.setEnabled(false);

            Toast.makeText(
                    this,
                    "Could not open the selected music.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void initialiseEqualizer() {
        if (mediaPlayer == null) {
            return;
        }

        try {
            if (equalizer != null) {
                equalizer.release();
                equalizer = null;
            }

            equalizer = new Equalizer(
                    0,
                    mediaPlayer.getAudioSessionId()
            );

            equalizer.setEnabled(true);
            createEqualizerControls();

        } catch (Exception exception) {
            eqContainer.removeAllViews();

            TextView unsupportedText = new TextView(this);
            unsupportedText.setText(
                    "Equalizer is not supported on this device."
            );
            unsupportedText.setTextColor(Color.LTGRAY);
            unsupportedText.setTextSize(14);

            eqContainer.addView(unsupportedText);

            Toast.makeText(
                    this,
                    "Equalizer is not supported on this device.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void createEqualizerControls() {
        eqContainer.removeAllViews();

        if (equalizer == null) {
            return;
        }

        short[] levelRange = equalizer.getBandLevelRange();

        final short minimumLevel = levelRange[0];
        final short maximumLevel = levelRange[1];

        for (
                int index = 0;
                index < targetFrequencies.length;
                index++
        ) {
            final int bandIndex = index;

            final short equalizerBand =
                    equalizer.getBand(
                            targetFrequencies[bandIndex] * 1000
                    );

            final TextView label = new TextView(this);

            label.setText(
                    bandNames[bandIndex] +
                    "  •  " +
                    targetFrequencies[bandIndex] +
                    " Hz  •  0.0 dB"
            );

            label.setTextColor(Color.WHITE);
            label.setTextSize(15);
            label.setPadding(
                    0,
                    dp(12),
                    0,
                    dp(4)
            );

            eqContainer.addView(label);

            SeekBar seekBar = new SeekBar(this);

            int totalRange =
                    maximumLevel - minimumLevel;

            seekBar.setMax(totalRange);
            seekBar.setProgress(-minimumLevel);

            seekBar.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(
                                SeekBar seekBar,
                                int progress,
                                boolean fromUser
                        ) {
                            short level =
                                    (short) (
                                            progress +
                                            minimumLevel
                                    );

                            try {
                                if (equalizer == null) {
                                    return;
                                }

                                equalizer.setBandLevel(
                                        equalizerBand,
                                        level
                                );

                                float decibels =
                                        level / 100f;

                                label.setText(
                                        bandNames[bandIndex] +
                                        "  •  " +
                                        targetFrequencies[bandIndex] +
                                        " Hz  •  " +
                                        String.format(
                                                Locale.US,
                                                "%.1f dB",
                                                decibels
                                        )
                                );

                            } catch (Exception ignored) {
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(
                                SeekBar seekBar
                        ) {
                        }

                        @Override
                        public void onStopTrackingTouch(
                                SeekBar seekBar
                        ) {
                        }
                    }
            );

            eqContainer.addView(seekBar);
        }
    }

    private void togglePlayback() {
        if (mediaPlayer == null) {
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playButton.setText("PLAY");
            } else {
                mediaPlayer.start();
                playButton.setText("PAUSE");
            }
        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    "Playback could not be started.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private int dp(int value) {
        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(value * density);
    }

    private void releaseAudio() {
        if (equalizer != null) {
            try {
                equalizer.setEnabled(false);
                equalizer.release();
            } catch (Exception ignored) {
            }

            equalizer = null;
        }

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        releaseAudio();
        super.onDestroy();
    }
            }
