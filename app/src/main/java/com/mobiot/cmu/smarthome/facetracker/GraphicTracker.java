package com.mobiot.cmu.smarthome.facetracker;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.mobiot.cmu.smarthome.activities.MultiTrackerActivity.handler;
import static com.mobiot.cmu.smarthome.activities.MultiTrackerActivity.idList;


/**
 * Generic activity_tracker which is used for tracking either a face or a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
class GraphicTracker<T> extends Tracker<T> {
    private GraphicOverlay mOverlay;
    private TrackedGraphic<T> mGraphic;
    private List<Integer> midList;
    private int id = -1;
    private AtomicInteger mfaceCount;

    GraphicTracker(GraphicOverlay overlay, TrackedGraphic<T> graphic, AtomicInteger faceCount) {
        mOverlay = overlay;
        mGraphic = graphic;
        midList = idList;
        mfaceCount = faceCount;
    }

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    @Override
    public void onNewItem(int id, T item) {
        mGraphic.setId(id);
        midList.add(id);
        this.id = id;
        mfaceCount.getAndIncrement();
        handler.sendEmptyMessage(0);
        //faceCount++;
        //System.out.println("Add Face: " + faceCount);
    }

    /**
     * Update the position/characteristics of the item within the overlay.
     */
    @Override
    public void onUpdate(Detector.Detections<T> detectionResults, T item) {
        mOverlay.add(mGraphic);
        mGraphic.updateItem(item);
        //midList.add(id);
        //mfaceCount.getAndIncrement();
        //System.out.println("item: " + item);
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily, for example if the face was momentarily blocked from
     * view.
     */
    @Override
    public void onMissing(Detector.Detections<T> detectionResults) {
        mOverlay.remove(mGraphic);
        //midList.remove(new Integer(id));
        //mfaceCount.getAndDecrement();
    }

    /**
     * Called when the item is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    @Override
    public void onDone() {
        mOverlay.remove(mGraphic);
        //if (midList.contains(new Integer(id))) {
        midList.remove(new Integer(id));
        mfaceCount.getAndDecrement();
        handler.sendEmptyMessage(0);
        //}
        //faceCount--;
        //System.out.println("Delete Face: " + faceCount);
    }
}
