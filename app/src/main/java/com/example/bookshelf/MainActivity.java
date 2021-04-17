package com.example.bookshelf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import edu.temple.audiobookplayer.AudiobookService;

public class MainActivity extends AppCompatActivity implements BookListFragment.BookSelectedInterface, ControlFragment.ControlFragmentInterface {

    FragmentManager fm;

    boolean twoPane;
    BookDetailsFragment bookDetailsFragment;
    Book selectedBook;

    ControlFragment controlFragment;
    Book bookPlaying;

    AudiobookService.MediaControlBinder mediaControlBinder;

    int currentProgress = 0;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediaControlBinder = (AudiobookService.MediaControlBinder) service;
            mediaControlBinder.setProgressHandler(playerProgressHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    Handler playerProgressHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            AudiobookService.BookProgress progress = ((AudiobookService.BookProgress) msg.obj);
            try {
                currentProgress = progress.getProgress();
                controlFragment.updateProgress(progress.getProgress());
                controlFragment.update();
            } catch (Exception e) {
                System.out.println(e);
            }
            return true;
        }
    });

    Intent bindIntent;

    private final String TAG_BOOKLIST = "booklist", TAG_BOOKDETAILS = "bookdetails", TAG_CONTROLL = "control";
    private final String KEY_SELECTED_BOOK = "selectedBook";
    private final String KEY_BOOKLIST = "searchedook";
    private final String KEY_PLAYING_BOOK = "bookPlaying";
    private final String KEY_PROGRESS = "currentProgress";
    private final int BOOK_SEARCH_REQUEST_CODE = 123;

    BookList bookList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.searchDialogButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(MainActivity.this, BookSearchActivity.class), BOOK_SEARCH_REQUEST_CODE);
            }
        });

        if (savedInstanceState != null) {
            // Fetch selected book if there was one
            selectedBook = savedInstanceState.getParcelable(KEY_SELECTED_BOOK);

            // Fetch previously searched books if one was previously retrieved
            bookList = savedInstanceState.getParcelable(KEY_BOOKLIST);

            // Fetch book playing if there was one
            bookPlaying = savedInstanceState.getParcelable(KEY_PLAYING_BOOK);
            currentProgress = savedInstanceState.getInt(KEY_PROGRESS);
        }else {
            // Create empty booklist if
            bookList = new BookList();
        }

        twoPane = findViewById(R.id.container2) != null;

        fm = getSupportFragmentManager();

        Fragment fragment1;
        fragment1 = fm.findFragmentById(R.id.container_1);


        // At this point, I only want to have BookListFragment be displayed in container_1
        if (fragment1 instanceof BookDetailsFragment) {
            fm.popBackStack();
        } else if (!(fragment1 instanceof BookListFragment))
            fm.beginTransaction()
                    .add(R.id.container_1, BookListFragment.newInstance(bookList), TAG_BOOKLIST)
            .commit();

        /*
        If we have two containers available, load a single instance
        of BookDetailsFragment to display all selected books
         */
        bookDetailsFragment = (selectedBook == null) ? new BookDetailsFragment() : BookDetailsFragment.newInstance(selectedBook);
        if (twoPane) {
            fm.beginTransaction()
                    .replace(R.id.container2, bookDetailsFragment, TAG_BOOKDETAILS)
                    .commit();
        } else if (selectedBook != null) {
            /*
            If a book was selected, and we now have a single container, replace
            BookListFragment with BookDetailsFragment, making the transaction reversible
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, bookDetailsFragment, TAG_BOOKDETAILS)
                    .addToBackStack(null)
                    .commit();
        }

        //Code for controller
        controlFragment = ControlFragment.newInstance(bookPlaying, currentProgress);

        if (!(fm.findFragmentById(R.id.container_control) instanceof ControlFragment)){
            fm.beginTransaction()
                    .replace(R.id.container_control, controlFragment, TAG_CONTROLL)
                    .commit();
        }

        bindIntent = new Intent(this, AudiobookService.class);
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    @Override
    public void bookSelected(int index) {
        // Store the selected book to use later if activity restarts
        selectedBook = bookList.get(index);

        if (twoPane)
            /*
            Display selected book using previously attached fragment
             */
            bookDetailsFragment.displayBook(selectedBook);
        else {
            /*
            Display book using new fragment
             */
            fm.beginTransaction()
                    .replace(R.id.container_1, BookDetailsFragment.newInstance(selectedBook), TAG_BOOKDETAILS)
                    // Transaction is reversible
                    .addToBackStack(null)
                    .commit();
        }
    }

    /**
     * Display new books when retrieved from a search
     */
    private void showNewBooks() {
        if ((fm.findFragmentByTag(TAG_BOOKDETAILS) instanceof BookDetailsFragment)) {
            fm.popBackStack();
        }
        ((BookListFragment) fm.findFragmentByTag(TAG_BOOKLIST)).showNewBooks();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_BOOK, selectedBook);
        outState.putParcelable(KEY_BOOKLIST, bookList);
        outState.putParcelable(KEY_PLAYING_BOOK, bookPlaying);
        outState.putInt(KEY_PROGRESS, currentProgress);
    }

    @Override
    public void onBackPressed() {
        // If the user hits the back button, clear the selected book
        selectedBook = null;
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BOOK_SEARCH_REQUEST_CODE && resultCode == RESULT_OK) {
            bookList.clear();
            bookList.addAll((BookList) data.getParcelableExtra(BookSearchActivity.BOOKLIST_KEY));
            if (bookList.size() == 0) {
                Toast.makeText(this, getString(R.string.error_no_results), Toast.LENGTH_SHORT).show();
            }
            showNewBooks();
        }
    }

    //For controller interface
    @Override
    public void onPressPlayButton() {
        if(selectedBook == null) return;

        bookPlaying = selectedBook;

        mediaControlBinder.stop();
        mediaControlBinder.play(bookPlaying.getId());
        controlFragment.setNowPlaying(bookPlaying);
        controlFragment.update();

    }

    @Override
    public void onPressPauseButton() {
        mediaControlBinder.pause();
    }

    @Override
    public void onPressStopButton() {
        mediaControlBinder.stop();
        currentProgress = 0;
        controlFragment.updateProgress(0);
        controlFragment.setNowPlaying(null);
        controlFragment.update();

    }

    @Override
    public void onSeekTo(int position) {
        mediaControlBinder.seekTo((int) ((double)position * bookPlaying.getDuration() / 100));
        System.out.println((int) ((double)position * bookPlaying.getDuration() / 100));
    }
}
