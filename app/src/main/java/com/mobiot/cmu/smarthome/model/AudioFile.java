package com.mobiot.cmu.smarthome.model;

import java.io.File;

/**
 * Created by mingchia on 4/27/17.
 */

public class AudioFile {
    private String fileName;
    private String absoluteFilePath;
    private String buildTime;
    private String audioLength;
    private File audioFile;
    public AudioFile(File file, String buildTime, String audioLength) {
        this.fileName = file.getName();
        this.absoluteFilePath = file.getAbsolutePath();
        this.buildTime = buildTime;
        this.audioLength = audioLength;
        audioFile = new File(this.absoluteFilePath);
    }
    public AudioFile(String fileName, String absoluteFilePath, String buildTime, String audioLength) {
        this.fileName = fileName;
        this.absoluteFilePath = absoluteFilePath;
        this.buildTime = buildTime;
        this.audioLength = audioLength;
        audioFile = new File(this.absoluteFilePath);
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }

    public String getBuildTime() {
        return buildTime;
    }

    public void setBuildTime(String buildTime) {
        this.buildTime = buildTime;
    }

    public String getAudioLength() {
        return audioLength;
    }

    public void setAudioLength(String audioLength) {
        this.audioLength = audioLength;
    }

    public File getAudioFile() {
        return audioFile;
    }
}

