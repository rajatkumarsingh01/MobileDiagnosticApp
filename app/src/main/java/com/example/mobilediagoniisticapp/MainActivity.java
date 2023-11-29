package com.example.mobilediagoniisticapp;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.TextAlignment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class MainActivity extends AppCompatActivity {
    private CameraManager cameraManager;
    private String frontCameraId;
    private String rearCameraId;
    private Button microphoneCheckButton;
    private Button bluetoothCheckButton;
    private Button sendToFirebase;
    private Button generatePDFButton;



    private FirebaseFirestore db;
    Boolean bluetoothCheck = false;
    Boolean cameraCheck = false;
    Boolean microphoneCheck = false;

    Boolean gyroscope_accelerometerCheck = false;

    Boolean gpsCheck = false;
    Calendar calendar = Calendar.getInstance();
    Date currentTime = calendar.getTime();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private LocationManager locationManager;


    // Define the desired date and time format
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int STORAGE_CODE = 1000;

    // Format the current date and time
    String currentDateTime = dateFormat.format(currentTime);

    // Define a constant for the camera permission request code

    private static final int GPS_PERMISSION_REQUEST_CODE = 103;

    private static final int GYROSCOPE_PERMISSION_REQUEST_CODE = 104;

    private static final int ACCELEROMETER_PERMISSION_REQUEST_CODE = 105;


    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int MICROPHONE_PERMISSION_REQUEST_CODE = 101;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        }

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId;
                } else if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    rearCameraId = cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        Button gpsCheckButton=findViewById(R.id.gpsCheckButton);

        gpsCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasGpsPermission()) {
                    // GPS permission is granted; check if GPS is working
                    boolean isGpsWorking = isGpsWorking();

                    if (isGpsWorking) {
                        Toast.makeText(MainActivity.this, "GPS is working.", Toast.LENGTH_SHORT).show();
                        gpsCheck = true;
                    } else {
                        Toast.makeText(MainActivity.this, "GPS is not working.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Request GPS permission
                    requestGpsPermission();
                }
            }
        });

        Button gyroscope_accelerometerCheckButton=findViewById(R.id.gyroscopeCheckButton);
        gyroscope_accelerometerCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasAccelerometerPermission() && hasGyroscopePermission()) {
                    // Both sensors are granted; check if they are working
                    boolean isAccelerometerWorking = isAccelerometerWorking();
                    boolean isGyroscopeWorking = isGyroscopeWorking();

                    if (isAccelerometerWorking && isGyroscopeWorking) {
                        Toast.makeText(MainActivity.this, "Accelerometer and Gyroscope are working.", Toast.LENGTH_SHORT).show();
                     Boolean gyroscope_accelerometerCheck = true;
                    } else {
                        Toast.makeText(MainActivity.this, "At least one sensor is not working.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Request permissions for both sensors
                    requestAccelerometerPermission();
                    requestGyroscopePermission();
                }
            }
        });





        Button cameraCheckButton = findViewById(R.id.cameraCheckButton);
        cameraCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request camera permissions if needed
                if (hasCameraPermission()) {
                    boolean isFrontCameraWorking = isCameraWorking(frontCameraId);
                    boolean isRearCameraWorking = isCameraWorking(rearCameraId);

                    if (isFrontCameraWorking && isRearCameraWorking) {
                        Toast.makeText(MainActivity.this, "Both cameras are working.", Toast.LENGTH_SHORT).show();
                        cameraCheck = true;
                    } else {
                        Toast.makeText(MainActivity.this, "At least one camera is not working.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Permission not granted; request permissions
                    requestCameraPermission();
                }
            }
        });

        microphoneCheckButton = findViewById(R.id.microphoneCheckButton);
        microphoneCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request microphone permissions if needed
                if (hasMicrophonePermission()) {
                    boolean isMicrophoneWorking = isMicrophoneWorking();
                    if (isMicrophoneWorking) {
                        Toast.makeText(MainActivity.this, "Microphone is working.", Toast.LENGTH_SHORT).show();
                        microphoneCheck = true;
                    } else {
                        Toast.makeText(MainActivity.this, "Microphone is not working.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Permission not granted; request permissions
                    requestMicrophonePermission();
                }
            }
        });

        bluetoothCheckButton = findViewById(R.id.bluetoothCheckButton);
        bluetoothCheckButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request Bluetooth permissions if needed
                if (hasBluetoothPermission()) {
                    boolean isBluetoothWorking = isBluetoothWorking();
                    if (isBluetoothWorking) {
                        Toast.makeText(MainActivity.this, "Bluetooth is working.", Toast.LENGTH_SHORT).show();
                        bluetoothCheck = true;
                    } else {
                        Toast.makeText(MainActivity.this, "Bluetooth is not working.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Permission not granted; request permissions
                    requestBluetoothPermission();
                }
            }
        });
        sendToFirebase = findViewById(R.id.sendToFirebaseButton);
        sendToFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request Bluetooth permissions if needed
                db = FirebaseFirestore.getInstance();

                // Create a new data object
                Map<String, Object> data = new HashMap<>();
                data.put("Camera", cameraCheck);
                data.put("Bluetooth", bluetoothCheck);
                data.put("Microphone", microphoneCheck);
                data.put("Gyroscope_Acceleromter",gyroscope_accelerometerCheck);
                data.put("Gps_Location",gpsCheck);

                // Specify the collection and document
                CollectionReference usersCollection = db.collection("users");
                DocumentReference userDocument = usersCollection.document(currentDateTime);

                // Add the data to the Firestore document
                userDocument.set(data)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, "Data added successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, "Error : "+ e, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        generatePDFButton = findViewById(R.id.generatePDFButton);
        generatePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newFunc();
            }
        });

    }

    private void newFunc() {
            try {
                String pdfPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();

                // Create a file with a name
                File file = new File(pdfPath,"phoneStatus.pdf");

                // Output the file
                FileOutputStream outputStream = new FileOutputStream(file);

                // Create a PDF writer to create an output stream
                PdfWriter writer = new PdfWriter(outputStream);

                // Create a PDF document in writing mode
                PdfDocument pdfDocument = new PdfDocument(writer);

                // Create a document from the PDF document
                Document document = new Document(pdfDocument);

                // Set the default size of the page
//                pdfDocument.setDefaultPageSize();

                // Set the margins of the page
                document.setMargins(10f, 10f, 10f, 10f);

                // Set the alignment of content to center
                document.setTextAlignment(TextAlignment.CENTER);

                String cameraCheckString = cameraCheck.toString();
                String bluetoothCheckString = bluetoothCheck.toString();
                String microphoneCheckString = microphoneCheck.toString();
                String gyroscopeCheckString=gyroscope_accelerometerCheck.toString();
                String gpsCheckString=gpsCheck.toString();



                // Adding content to the document
                document.add(new Paragraph("Camera :      " + cameraCheckString));
                document.add(new Paragraph("Bluetooth  :      " + bluetoothCheckString));
                document.add(new Paragraph("Microphone :      " + microphoneCheckString));
                document.add(new Paragraph("Gyroscope_Accelerometer:" +gyroscopeCheckString));
                document.add(new Paragraph("Gps_Location:    " +gpsCheckString));

                // Close the document
                document.close();
                Toast.makeText(this, "File is Generated", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private boolean hasAccelerometerPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAccelerometerPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, ACCELEROMETER_PERMISSION_REQUEST_CODE);
    }

    private boolean isAccelerometerWorking() {
        return accelerometer != null; // Check if accelerometer is available on the device
    }

    private boolean hasGyroscopePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGyroscopePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, GYROSCOPE_PERMISSION_REQUEST_CODE);
    }

    private boolean isGyroscopeWorking() {
        return gyroscope != null; // Check if gyroscope is available on the device
    }

    private boolean hasGpsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGpsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, GPS_PERMISSION_REQUEST_CODE);
    }

    private boolean isGpsWorking() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }



    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private boolean hasMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_REQUEST_CODE);
    }

    private boolean hasBluetoothPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, BLUETOOTH_PERMISSION_REQUEST_CODE);
    }

    private boolean isBluetoothWorking() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }




    // Handle the permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ACCELEROMETER_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Accelerometer permission granted; you can now proceed to check the accelerometer
                boolean isAccelerometerWorking = isAccelerometerWorking();
                if (isAccelerometerWorking) {
                    Toast.makeText(MainActivity.this, "Accelerometer is working.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Accelerometer is not working.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Accelerometer permission denied; you can handle this case as needed
                Toast.makeText(MainActivity.this, "Accelerometer permission denied.", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == GYROSCOPE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Gyroscope permission granted; you can now proceed to check the gyroscope
                boolean isGyroscopeWorking = isGyroscopeWorking();
                if (isGyroscopeWorking) {
                    Toast.makeText(MainActivity.this, "Gyroscope is working.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Gyroscope is not working.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Gyroscope permission denied; you can handle this case as needed
                Toast.makeText(MainActivity.this, "Gyroscope permission denied.", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == GPS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // GPS permission granted; you can now proceed to check GPS
                boolean isGpsWorking = isGpsWorking();
                if (isGpsWorking) {
                    Toast.makeText(MainActivity.this, "GPS is working.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "GPS is not working.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // GPS permission denied; you can handle this case as needed
                Toast.makeText(MainActivity.this, "GPS permission denied.", Toast.LENGTH_SHORT).show();
            }
        }



        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted; you can now proceed to check the camera
                boolean isFrontCameraWorking = isCameraWorking(frontCameraId);
                boolean isRearCameraWorking = isCameraWorking(rearCameraId);

                if (isFrontCameraWorking && isRearCameraWorking) {
                    Toast.makeText(MainActivity.this, "Both cameras are working.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "At least one camera is not working.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Camera permission denied; you can handle this case as needed
                Toast.makeText(MainActivity.this, "Camera permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == MICROPHONE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Microphone permission granted; you can now proceed to check the microphone
                boolean isMicrophoneWorking = isMicrophoneWorking();
                if (isMicrophoneWorking) {
                    Toast.makeText(MainActivity.this, "Microphone is working.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Microphone is not working.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Microphone permission denied; you can handle this case as needed
                Toast.makeText(MainActivity.this, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            // Handle Bluetooth permission result
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission granted; you can now proceed to check Bluetooth
                boolean isBluetoothWorking = isBluetoothWorking();
                if (isBluetoothWorking) {
                    Toast.makeText(MainActivity.this, "Bluetooth is working.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Bluetooth is not working.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Bluetooth permission denied; you can handle this case as needed
                Toast.makeText(MainActivity.this, "Bluetooth permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isCameraWorking(String cameraId) {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                return true;
            }


            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    camera.close(); // Release the camera resource
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            }, null);
            return true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }

    }
    private boolean isMicrophoneWorking() {
        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            return true;
        }
        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        try {
            audioRecord.startRecording();
            audioRecord.stop();
            audioRecord.release();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    }



