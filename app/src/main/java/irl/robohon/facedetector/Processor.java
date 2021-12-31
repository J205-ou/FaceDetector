package irl.robohon.facedetector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import trikita.log.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Phaser;

import static com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE;
import static com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST;

public class Processor
{
  private ServerSocket mServer;
  private Socket mSocket;
  private int port = 8080;

  private FaceDetector detector = null;
  private final Phaser phaser = new Phaser(1);
  JSONObject json_result = null;

  public Processor()
  {
    // MLkit face detector setup
    // Real-time contour detection
    FaceDetectorOptions opts_detector =
      new FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
      .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
      .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
      .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
      // .setMinFaceSize(0.5f)
      .setMinFaceSize(0.2f)
      .build();
    detector = FaceDetection.getClient(opts_detector);
  }

  void run()
  {
    try {
      Log.i("creating serverSocket");
      mServer = new ServerSocket(port);

      while(true){
        mSocket = mServer.accept();
        Log.i("server: connection accepted: " + mSocket.getRemoteSocketAddress());
        DataInputStream in = new DataInputStream(mSocket.getInputStream());
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
        int n_read;
        Bitmap bitmap;

        while (true){
          bitmap = null;
          try {
            // receive header (4-byte big endian)
            byte[] header = new byte[4];
            in.readFully(header);
            int len = java.nio.ByteBuffer.wrap(header).getInt();
            byte[] data = new byte[len];
            in.readFully(data);
            bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
          }
          catch (Exception e){
            // socket may have been closed
            break;
          }
          if (bitmap == null) break;

          phaser.register();
          faceDetection(bitmap);
          phaser.arriveAndAwaitAdvance();
          out.write(json_result.toString());
          out.flush();
        }
        Log.i("connection closed");
        mSocket.close();
      }

    }
    catch (Exception e){
      e.printStackTrace();
    }
  }

  // face detection by MLkit
  private void faceDetection(Bitmap bitmap)
  {
    InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
    Task<List<Face>> result = detector.process(inputImage)
      .addOnSuccessListener(
			    new OnSuccessListener<List<Face>>() {
			      @Override
			      public void onSuccess(List<Face> faces) {
				try {
				  JSONObject json_base = new JSONObject();
				  json_base.put("status", "OK");

				  JSONArray json_arr = new JSONArray();
				  for (Face face : faces) {
				    JSONObject json = new JSONObject();
				    Rect bb = face.getBoundingBox();
				    float rotY = face.getHeadEulerAngleY(); // Head is rotated to the right rotY degrees
				    float rotZ = face.getHeadEulerAngleZ(); // Head is tilted sideways rotZ degrees

				    JSONArray json_bb = new JSONArray();
				    json_bb.put(bb.left);
				    json_bb.put(bb.top);
				    json_bb.put(bb.right);
				    json_bb.put(bb.bottom);

				    json.put("region", json_bb);
				    json.put("rotY", rotY);
				    json.put("rotZ", rotZ);
				    json_arr.put(json);
				  }
				  json_base.put("faces", json_arr);
                                  json_result = json_base;
				}
				catch (Exception e) {
                                  Log.e("faceDetection onSuccessListener failed: " + e.getMessage());
				}
                                phaser.arriveAndDeregister();
			      }
			    })
      .addOnFailureListener(
			    new OnFailureListener() {
			      @Override
			      public void onFailure(@NonNull Exception e) {
				// Task failed with an exception
                                Log.e("faceDetection onFailureListener: " + e.getMessage());
                                phaser.arriveAndDeregister();
			      }
			    });
  }

}
