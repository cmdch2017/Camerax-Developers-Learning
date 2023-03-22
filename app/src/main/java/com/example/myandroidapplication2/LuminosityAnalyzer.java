package com.example.myandroidapplication2;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class LuminosityAnalyzer implements ImageAnalysis.Analyzer {

    private LumaListener listener;

    public LuminosityAnalyzer(LumaListener listener) {
        this.listener = listener;
    }

    private byte[] toByteArray(ByteBuffer buffer) {
        buffer.rewind(); // Rewind the buffer to zero
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data); // Copy the buffer into a byte array
        return data; // Return the byte array
    }

    @Override
    public void analyze(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = toByteArray(buffer);
        List<Integer> pixels = new ArrayList<>();
        for (byte b : data) {
            pixels.add(b & 0xFF);
        }
        double luma = pixels.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        listener.onLumaAvailable(luma);

        image.close();
    }
}
