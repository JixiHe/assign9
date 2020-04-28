package com.example.bookshelf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import edu.temple.audiobookplayer.AudiobookService;

public class MainActivity extends AppCompatActivity implements BookDetailsFragment.PlayListener {
    AudiobookService.MediaControlBinder binder;
    private ArrayList<Book> books = new ArrayList<>();
    private BookListFragment listFragment;
    private BookDetailsFragment detailFragment;
    private EditText Search;
    private ViewPagerFragment viewpagerFragment;

    private SeekBar seekBar;
    private Intent serviceIntent;
    private TextView currently_playing;
    private int duration;
    private boolean ServiceConnected;
    private int bookId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Search = findViewById(R.id.search_bar);

        // all buttom
        Button search_buttom = findViewById (R.id.search_button);
        Button pause_Button = findViewById (R.id.pause_button);
        Button stop_Button = findViewById (R.id.stop_button);

        currently_playing = findViewById(R.id.now_playing);
        seekBar = findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (bookId!=-1) {
                    if (fromUser && ServiceConnected) {
                        binder.play(bookId, progress);

                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        search_buttom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchString = Search.getText().toString();
                searchBooks(searchString);
            }
        });

        DisplayMetrics displayMetrics = new DisplayMetrics ();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        if (height > width) {
            viewpagerFragment = ViewPagerFragment.newInstance();
            fragmentTransaction.replace(R.id.book_display, viewpagerFragment);

        } else {
            //if landscape
            listFragment = BookListFragment.newInstance();
            detailFragment = new BookDetailsFragment();
            fragmentTransaction.replace(R.id.listview, listFragment);
            fragmentTransaction.replace(R.id.right_view, detailFragment);
            listFragment.addSelectListener(new BookListFragment.OnItemSelectedListener() {
                @Override
                public void onItemSelected(Book book) {
                    detailFragment.displayBook(book);
                }
            });

        }
        fragmentTransaction.commit();
        serviceIntent= new Intent(this, AudiobookService.class);
        bindService(serviceIntent, ServiceConnection, Context.BIND_AUTO_CREATE);

        stop_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binder.stop();
                seekBar.setProgress(0);
                bookId = -1;

                stopService(serviceIntent);
            }
        });
        pause_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binder.pause();

            }
        });

    }

    // display what is currently playing
    @Override
    public void play(Book book) {
        String title = book.title;

        bookId =book.id;
        startService(serviceIntent);
        duration= book.duration;

        //min is 0, max is duration
        seekBar.setMax(book.duration);


        // set whats playing on top with name
        currently_playing.setText("Now playing: " + title);
        binder.play(book.id);
    }


    Handler handler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {
            try{
                JSONArray  bookArray = new JSONArray((String)msg.obj);

                if(viewpagerFragment!=null){
                    for(int i = 0 ; i < bookArray.length(); i++){
                        try {
                            JSONObject jsonObject = bookArray.optJSONObject (i);
                            Book book = new Book ();
                            book.id = jsonObject.optInt ("book_id");
                            book.title = jsonObject.optString ("title");
                            book.author = jsonObject.optString ("author");
                            book.cover_url = jsonObject.optString ("cover_url");
                            book.duration = jsonObject.optInt ("duration");
                            books.add (book);
                        } catch (Exception e) {
                            e.printStackTrace ();
                        }
                    }
                    viewpagerFragment.setBooks(books);

                }else if (listFragment!=null){
                    listFragment.setBooks(bookArray);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
    });
    private void searchBooks(final  String searchString) {
        new Thread(){
            public void run(){
                try{
                    String urlStr = "https://kamorris.com/lab/abp/booksearch.php?search=" + searchString;
                    URL url = new URL(urlStr);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                    StringBuilder builder = new StringBuilder();
                    String tmpString;

                    while((tmpString = reader.readLine()) != null){
                        builder.append(tmpString);
                    }
                    Log.e("tag",builder.toString());
                    Message msg = Message.obtain();
                    msg.obj = builder.toString();
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    android.content.ServiceConnection ServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ServiceConnected = true;
            binder = (AudiobookService.MediaControlBinder) service;
            binder.setProgressHandler(progressHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ServiceConnected = false;
            binder = null;
        }
    };

    Handler progressHandler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {
            try{
            if (msg.obj != null) {
                AudiobookService.BookProgress bookProgress = (AudiobookService.BookProgress) msg.obj;
                Log.e ("tag", bookProgress.getProgress () + "");
                if (bookProgress.getProgress () < duration) {
                    seekBar.setProgress (bookProgress.getProgress ());
                } else if (bookProgress.getProgress () == duration) {
                    binder.stop ();
                    seekBar.setProgress (0);
                    bookId = -1;
                }
            }
            } catch (Exception e) {
                e.printStackTrace ();
            }
            return false;
        }
    });
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(ServiceConnection);
    }


}
