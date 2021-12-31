package irl.robohon.facedetector;

import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import trikita.log.Log;

public class MainActivity extends AppCompatActivity
{
  Handler handler = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    Processor processor = new Processor();

    HandlerThread handlerThread = new HandlerThread("Processor");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());

    Log.i("starting server");
    handler.post(new Runnable(){ @Override public void run(){
      processor.run();
    }});
  }

}
