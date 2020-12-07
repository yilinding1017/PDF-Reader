package ca.uwaterloo.cs349.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.Stack;


class Annotation {
    Path path;
    int toolmode;
    boolean isVisible;
    Annotation(Path path, int mode, boolean visible){
        this.path = path;
        this.toolmode = mode;
        this.isVisible = visible;
    }
};

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    // drawing path
    Path path = null;
    //ArrayList<Path> paths = new ArrayList();
    ArrayList<Annotation> annotaions = new ArrayList();

    // image to display
    Bitmap bitmap;
    Paint paint = new Paint(Color.BLUE);

    ArrayList<ArrayList<Annotation>> annotLst = new ArrayList();

    Stack<Annotation> undoStack = new Stack<>();
    Stack<Annotation> redoStack = new Stack<>();

    float startX1 = 0;
    float startY1 = 0;
    float startX2 = 0;
    float startY2 = 0;
    Matrix pdfMatrix;
    Matrix defaultMatrix;

    float translateX=0;
    float translateY=0;
    float scale=1;
    float middleX=0;
    float middleY=0;


    // constructor
    public PDFimage(Context context) {
        super(context);

        for(int i = 0; i < MainActivity.PAGENUM; ++i) {
            annotLst.add(new ArrayList<Annotation>());
        }
        annotaions = annotLst.get(0);

        /*pdfMatrix = this.getImageMatrix();
        defaultMatrix = this.getImageMatrix();*/
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pointer = event.getPointerCount();
        if(pointer == 1) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //isMoved = false;
                    Log.d(LOGNAME, "Action down");
                    path = new Path();
                    path.moveTo(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_MOVE:
                    //isMoved = true;
                    Log.d(LOGNAME, "Action move");
                    path.lineTo(event.getX(), event.getY());
                    // Erase Mode
                    if (MainActivity.mode == 2) {
                        // Referenced idea from
                        // https://stackoverflow.com/questions/9843578/collision-detection-with-bitmaps-on-surfaceviews-canvas-in-android/9918830#9918830
                        Region eraseRegion = new Region();
                        RectF eraseBound = new RectF();
                        path.computeBounds(eraseBound, true);

                        if (eraseBound.top == eraseBound.bottom) {
                            eraseBound.bottom += 0.2;
                            eraseRegion.set(new Rect((int) eraseBound.left, (int) eraseBound.top, (int) eraseBound.right, (int) eraseBound.bottom));
                        } else if (eraseBound.left  == eraseBound.right) {
                            eraseBound.right += 0.2;
                            eraseRegion.set(new Rect((int) eraseBound.left, (int) eraseBound.top, (int) eraseBound.right, (int) eraseBound.bottom));
                        } else {
                            eraseRegion.setPath(path, new Region((int) eraseBound.left, (int) eraseBound.top, (int) eraseBound.right, (int) eraseBound.bottom));
                        }

                        //eraseRegion.setPath(path, new Region(0,0,this.getWidth(),this.getHeight()));

                        for (Annotation epath : annotaions) {
                            if (!epath.isVisible) continue;
                            Region pathRegion = new Region();
                            RectF pathBound = new RectF();
                            epath.path.computeBounds(pathBound, true);

                            if (pathBound.top  == pathBound.bottom) {
                                pathBound.bottom += 0.2;
                                pathRegion.set(new Rect((int) pathBound.left, (int) pathBound.top, (int) pathBound.right, (int) pathBound.bottom));
                            } else if (pathBound.left  == pathBound.right) {
                                pathBound.right += 0.2;
                                pathRegion.set(new Rect((int) pathBound.left, (int) pathBound.top, (int) pathBound.right, (int) pathBound.bottom));
                            } else {
                                pathRegion.setPath(epath.path, new Region((int) pathBound.left, (int) pathBound.top, (int) pathBound.right, (int) pathBound.bottom));
                            }

                            //pathRegion.setPath(epath.path, new Region(0,0,this.getWidth(),this.getHeight()));

                            if (pathRegion.op(eraseRegion, Region.Op.INTERSECT)) {
                                epath.isVisible = false;
                                // draw saved in undo stack
                                undoStack.push(epath);
                            }
                        }
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(LOGNAME, "Action up");
                    if (MainActivity.mode != 2) {
                        annotaions.add(new Annotation(path, MainActivity.mode, true));
                        undoStack.push(annotaions.get(annotaions.size() - 1));
                    }
                    path = null;
                    //isMoved = false;
                    break;
            }
        } else {
            // Referenced the ideas from
            // https://developer.android.com/training/gestures/scale?fbclid=IwAR03NPGuy-lI28BVKUvPbKnUmrATfNHV02tgPDTVpX_CQg8zTyBxnfAtH1w
            this.setScaleType(ScaleType.MATRIX);
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    pdfMatrix = this.getImageMatrix();
                    startX1 = event.getX(0);
                    startY1 = event.getY(0);
                    startX2 = event.getX(1);
                    startY2 = event.getY(1);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    float endX1 = event.getX(0);
                    float endY1 = event.getY(0);
                    float endX2 = event.getX(1);
                    float endY2 = event.getY(1);

                    double startDis = Math.sqrt(Math.pow(startX1 - startX2, 2) + Math.pow(startY1 - startY2, 2));
                    double endDis = Math.sqrt(Math.pow(endX1 - endX2, 2) + Math.pow(endY1 - endY2, 2));

                    if(Math.abs(startDis-endDis) < 2) {
                        // Pan mode
                        translateX = endX1-startX1;
                        translateY = endY1-startY1;
                        pdfMatrix.postTranslate(translateX,translateY);
                    } else {
                        // Zoom mode
                        scale = (float) (endDis/startDis);
                        middleX = (endX1+endX2)/2;
                        middleY = (endY1+endY2)/2;
                        pdfMatrix.postScale(scale,scale,middleX,middleY);
                    }

                    startX1 = event.getX(0);
                    startY1 = event.getY(0);
                    startX2 = event.getX(1);
                    startY2 = event.getY(1);

                    break;
            }
        }
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap, int index) {
        this.bitmap = bitmap;

        this.annotaions = annotLst.get(index);

        //pdfMatrix = defaultMatrix;
    }

    /*public void setPaths(ArrayList<Path> inputpaths) {
        if(inputpaths == null) this.paths = new ArrayList<Path>();
        else {
            for(Path spath : inputpaths) {
                this.paths.add(spath);
            }
        }
    }*/

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Scale/translate canvas
        //canvas.setScaleType(ScaleType.MATRIX);
        //Matrix canvasMatrix = canvas.getImageMatrix();
        //canvas.Translate(translateX,translateY);
        //canvas.Scale(scale,scale,middleX,middleY);

        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
        this.setImageMatrix(pdfMatrix);
        // draw lines over it
        for (Annotation annot : annotaions) {
            if(annot.isVisible) {
                if(annot.toolmode == 0) {
                    canvas.drawPath(annot.path, MainActivity.pencil);
                } else if(annot.toolmode == 1) {
                    canvas.drawPath(annot.path, MainActivity.highlighter);
                }
            }
        }
        if (path != null && MainActivity.mode != 2) {
            canvas.drawPath(path, paint);
        }
        super.onDraw(canvas);
    }
}
