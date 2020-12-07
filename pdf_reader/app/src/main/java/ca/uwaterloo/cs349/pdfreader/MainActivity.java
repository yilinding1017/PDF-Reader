package ca.uwaterloo.cs349.pdfreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not obvious from documentation, so read this carefully before making changes
// to the PDF display code.

public class MainActivity extends AppCompatActivity {

    final static int PAGENUM = 55;
    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "shannon1948.pdf";
    final int FILERESID = R.raw.shannon1948;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;
    // create an array to store all the annotations (pageImages)
    //ArrayList<PDFimage> pageImages = new ArrayList<PDFimage>();

    //ArrayList<ArrayList<Path>> annotationLst = new ArrayList<ArrayList<Path>>();

    // buttons to switch between pages
    private Button preButton;
    private Button nextButton;
    private TextView pageText;

    public static int mode = 0;

    static Paint pencil = new Paint();
    static Paint highlighter = new Paint();

    private Button undoButton;
    private Button redoButton;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        pageImage = new PDFimage(this);

        pencil.setAntiAlias(true);
        pencil.setStyle(Paint.Style.STROKE);
        pencil.setStrokeWidth(6);
        pencil.setColor(Color.BLACK);

        highlighter.setAntiAlias(true);
        highlighter.setStyle(Paint.Style.STROKE);
        highlighter.setStrokeWidth(28);
        highlighter.setARGB(70,255,255,0);

        pageImage.setBrush(pencil);

        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(0);
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }

        /*for(int i = 0; i < PAGENUM; ++i) {
            pageImages.add(new PDFimage(this));
        }*/

        /*for(int i = 0; i < PAGENUM; ++i) {
            annotationLst.add(new ArrayList<Path>());
        }*/

        // Get reference for buttons and page text
        preButton = findViewById(R.id.prebutton);
        nextButton = findViewById(R.id.nextbutton);
        pageText = findViewById(R.id.textView);

        undoButton = findViewById(R.id.undobutton);
        redoButton = findViewById(R.id.redobutton);

        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPage.getIndex() != 0) {
                    showPage(currentPage.getIndex()-1);
                    pageText.setText(currentPage.getIndex()+1 + "/" + PAGENUM);
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentPage.getIndex() != PAGENUM-1) {
                    showPage(currentPage.getIndex()+1);
                    pageText.setText(currentPage.getIndex()+1 + "/" + PAGENUM);
                }
            }
        });

        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // revert the field
                if(!pageImage.undoStack.isEmpty()) {
                    pageImage.undoStack.peek().isVisible = !(pageImage.undoStack.peek().isVisible);

                    pageImage.redoStack.push(pageImage.undoStack.pop());
                }
            }
        });

        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!pageImage.redoStack.isEmpty()) {
                    // revert the field
                    pageImage.redoStack.peek().isVisible = !(pageImage.redoStack.peek().isVisible);

                    pageImage.undoStack.push(pageImage.redoStack.pop());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.add(0,0,1,"Draw");
        menu.add(0,1,2,"Highlight");
        menu.add(0,2,3,"Erase");

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                // Draw
                mode = 0;

                pageImage.setBrush(pencil);
                break;
            case 1:
                // Highlight
                mode = 1;

                pageImage.setBrush(highlighter);
                break;
            case 2:
                // Erase
                mode = 2;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }
        // Save the previous annotation into array
        /*if(pageImage.paths != null) {
            //ArrayList<Path> currAnnotation = new ArrayList<Path>();
            for(Path spath : pageImage.paths) {
                annotationLst.get(currentPage.getIndex()).add(spath);
            }
        }*/

        // Close the current page before opening another one.
        if (null != currentPage) {
            // Save the previous pageImage into array
            //pageImages.set(currentPage.getIndex(),pageImage);

            // Set pageImage to current one (with the corresponding drawing)
            //pageImage = pageImages.get(index);

            currentPage.close();
        }
        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap, currentPage.getIndex());

        //pageImage.setPaths(annotationLst.get(index));

    }
}
