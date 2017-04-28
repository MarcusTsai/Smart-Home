package com.mobiot.cmu.smarthome.facetracker;

import android.widget.TextView;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import java.util.concurrent.atomic.AtomicInteger;

public class FaceTrackerFactory implements MultiProcessor.Factory<Face> {
    private GraphicOverlay mGraphicOverlay;
    //public int FaceCount = 0;
    //public ArrayList<Integer> idList;
    static AtomicInteger faceCount = new AtomicInteger(0);

    private TextView faceCountDisplay = null;
    //public static int OffsetThread = Thread.activeCount();

    public FaceTrackerFactory(GraphicOverlay graphicOverlay) {
        mGraphicOverlay = graphicOverlay;
        //idList = new ArrayList<>();
    }

    @Override
    public Tracker<Face> create(Face face) {

        FaceGraphic graphic = new FaceGraphic(mGraphicOverlay);
        //System.out.println("Face: " + face);
        //System.out.println("Face Count1:" + idList.size() );
        //System.out.println("Face Count2:" + faceCount);

        //System.out.println("Face Offset:" + OffsetThread);
        //System.out.println("Face Count3:" + (ManagementFactory.getThreadMXBean().getThreadCount() - OffsetThread));
        //handler.sendEmptyMessage(0);
        return new GraphicTracker<>(mGraphicOverlay, graphic, faceCount);
    }

}

