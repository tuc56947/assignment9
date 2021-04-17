package com.example.bookshelf;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ControlFragment extends Fragment {

    private static final String ARG_CONTROL = "param1";
    private static final String ARG_PROGRESS = "param2";

    // TODO: Rename and change types of parameters
    private Book book;
    private int progress;

    TextView controlTextView;
    Button playButton, pauseButton, stopButton;
    SeekBar seekBar;

    ControlFragmentInterface controlFragmentInterface;

    public ControlFragment() {
        // Required empty public constructor
    }

    public static ControlFragment newInstance(Book book, int progress) {
        ControlFragment fragment = new ControlFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONTROL, book);
        args.putInt(ARG_PROGRESS, progress);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        controlFragmentInterface = (ControlFragmentInterface) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            book = getArguments().getParcelable(ARG_CONTROL);
        }
    }

    public void setNowPlaying(Book book) {
        this.book = book;
    }

    public void updateProgress(int progress) {
        this.progress = progress;
    }

    public void update() {
        controlTextView.setText(setControlTextView());
        this.seekBar.setProgress(getScaledProgress());
    }

    private String setControlTextView(){
        if (this.book == null)
            return "Choose a book";
        else
            return "Now Playing: " + this.book.getTitle();
    }

    private int getScaledProgress(){
        if(book == null)
            return 0;
        else return (int) ((double)progress / (double)book.getDuration())*100;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_control, container, false);

        controlTextView = view.findViewById(R.id.controlTextView);
        playButton = view.findViewById(R.id.playButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        stopButton = view.findViewById(R.id.stopButton);
        seekBar = view.findViewById(R.id.seekBar);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controlFragmentInterface.onPressPlayButton();
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                controlFragmentInterface.onPressPauseButton();
            }
        });

        stopButton.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controlFragmentInterface.onPressStopButton();
            }
        }));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                controlFragmentInterface.onSeekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        return view;
    }

    interface ControlFragmentInterface {
        void onPressPlayButton();
        void onPressPauseButton();
        void onPressStopButton();
        void onSeekTo(int position);
    }
}