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

    // constructor
    public PDFimage(Context context) {
        super(context);

        for(int i = 0; i < MainActivity.PAGENUM; ++i) {
            annotLst.add(new ArrayList<Annotation>());
        }
        annotaions = annotLst.get(0);
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(LOGNAME, "Action down");
                path = new Path();
                path.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(LOGNAME, "Action move");
                path.lineTo(event.getX(), event.getY());
                // Erase Mode
                if (MainActivity.mode == 2) {
                    Region eraseRegion = new Region();
                    RectF eraseBound = new RectF();
                    path.computeBounds(eraseBound,true);

                    if (eraseBound.top - eraseBound.bottom == 0){
                        eraseBound.top -= 0.1;
                        eraseRegion.set(new Rect((int)eraseBound.left,(int)eraseBound.top,(int)eraseBound.right,(int)eraseBound.bottom));
                    } else if (eraseBound.right - eraseBound.left == 0){
                        eraseBound.left -= 0.1;
                        eraseRegion.set(new Rect((int)eraseBound.left,(int)eraseBound.top,(int)eraseBound.right,(int)eraseBound.bottom));
                    } else{
                        eraseRegion.setPath(path, new Region((int)eraseBound.left, (int)eraseBound.top,(int)eraseBound.right, (int)eraseBound.bottom));
                    }

                    //eraseRegion.setPath(path, new Region(0,0,this.getWidth(),this.getHeight()));

                    for (Annotation epath : annotaions) {
                        if(!epath.isVisible) continue;
                        Region pathRegion = new Region();
                        RectF pathBound = new RectF();
                        epath.path.computeBounds(pathBound, true);

                        if (pathBound.top - pathBound.bottom == 0){
                            pathBound.top -= 0.1;
                            pathRegion.set(new Rect((int)pathBound.left,(int)pathBound.top,(int)pathBound.right,(int)pathBound.bottom));
                        } else if (pathBound.left - pathBound.right == 0){
                            pathBound.left -= 0.1;
                            pathRegion.set(new Rect((int)pathBound.left,(int)pathBound.top,(int)pathBound.right,(int)pathBound.bottom));
                        } else{
                            pathRegion.setPath(epath.path, new Region((int) pathBound.left, (int) pathBound.top,(int) pathBound.right, (int) pathBound.bottom));
                        }

                        //pathRegion.setPath(epath.path, new Region(0,0,this.getWidth(),this.getHeight()));

                        if(pathRegion.op(eraseRegion,Region.Op.INTERSECT)) {
                            epath.isVisible = false;
                            // draw saved in undo stack
                            undoStack.push(epath);
                        }
                    }
                }

                break;
            case MotionEvent.ACTION_UP:
                Log.d(LOGNAME, "Action up");
                if(MainActivity.mode != 2) {
                    annotaions.add(new Annotation(path,MainActivity.mode,true));
                    undoStack.push(annotaions.get(annotaions.size()-1));
                }
                path = null;
                break;
        }
        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap, int index) {
        this.bitmap = bitmap;

        this.annotaions = annotLst.get(index);
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
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }
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
