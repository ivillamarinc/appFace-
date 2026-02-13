package com.example.appface;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.*;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.barcode.*;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 100;
    private static final int REQUEST_CAMERA = 101;
    private static final int CAMERA_PERMISSION_CODE = 200;

    ImageView imageView;
    TextView txtResultado;
    Button btnGaleria, btnCamara, btnOCR, btnBarcode;

    Bitmap selectedBitmap;
    Uri selectedImageUri;

    FaceDetector faceDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main),
                (v, insets) -> {
                    v.setPadding(
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                    );
                    return insets;
                });

        // Inicializar vistas
        imageView = findViewById(R.id.imageView);
        txtResultado = findViewById(R.id.txtResultado);
        btnGaleria = findViewById(R.id.btnGaleria);
        btnCamara = findViewById(R.id.btnCamara);
        btnOCR = findViewById(R.id.btnOCR);
        btnBarcode = findViewById(R.id.btnBarcode);

        // Configuración detector de rostros
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .build();

        faceDetector = FaceDetection.getClient(options);

        // Botones
        btnGaleria.setOnClickListener(v -> abrirGaleria());
        btnCamara.setOnClickListener(v -> verificarPermisoCamara());

        btnOCR.setOnClickListener(v -> {
            if (selectedBitmap != null) detectarOCR();
            else Toast.makeText(this, "Seleccione una imagen", Toast.LENGTH_SHORT).show();
        });

        btnBarcode.setOnClickListener(v -> {
            if (selectedBitmap != null) detectarCodigoBarras();
            else Toast.makeText(this, "Seleccione una imagen", Toast.LENGTH_SHORT).show();
        });
    }

    // ================= GALERÍA =================
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    // ================= CÁMARA =================
    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            abrirCamara();
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        }
    }

    // ================= RESULTADO IMAGEN =================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        try {
            if (requestCode == REQUEST_GALLERY) {

                selectedImageUri = data.getData();

                selectedBitmap = MediaStore.Images.Media
                        .getBitmap(getContentResolver(), selectedImageUri);

                imageView.setImageBitmap(selectedBitmap);
            }
            else if (requestCode == REQUEST_CAMERA) {
                selectedBitmap = (Bitmap) data.getExtras().get("data");
            }

            imageView.setImageBitmap(selectedBitmap);

            // Detectar rostros automáticamente
            detectarRostros(selectedBitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= ROSTROS =================
    private void detectarRostros(Bitmap bitmap) {

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(this::dibujarRostros)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error detección de rostros", Toast.LENGTH_SHORT).show());
    }

    private void dibujarRostros(List<Face> faces) {

        Bitmap mutable = selectedBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutable);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        for (Face face : faces) {
            canvas.drawRect(face.getBoundingBox(), paint);
        }

        imageView.setImageBitmap(mutable);
        txtResultado.setText("Rostros detectados: " + faces.size());
    }

    // ================= OCR =================
    private void detectarOCR() {

        InputImage image = InputImage.fromBitmap(selectedBitmap, 0);

        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    String result = text.getText();

                    if (result.isEmpty()) {
                        txtResultado.setText("No se detectó texto");
                    } else {
                        txtResultado.setText(result);
                    }
                })
                .addOnFailureListener(e ->
                        txtResultado.setText("Error al reconocer texto")
                );
    }

    // ================= BARCODE =================
    private void detectarCodigoBarras() {

        InputImage image;

        try {
            if (selectedImageUri != null) {
                image = InputImage.fromFilePath(this, selectedImageUri);
            } else {
                image = InputImage.fromBitmap(selectedBitmap, 0);
            }
        } catch (IOException e) {
            txtResultado.setText("Error al procesar imagen");
            return;
        }

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {

                    if (barcodes.isEmpty()) {
                        txtResultado.setText("No se detectó código de barras");
                        return;
                    }

                    StringBuilder resultado = new StringBuilder();

                    for (Barcode barcode : barcodes) {

                        String valor = barcode.getRawValue();

                        resultado.append("Código: ")
                                .append(valor)
                                .append("\n");
                    }

                    txtResultado.setText(resultado.toString());
                })
                .addOnFailureListener(e ->
                        txtResultado.setText("Error al leer código de barras")
                );
    }

}
