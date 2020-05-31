package org.swdc.archive.ui;

import info.monitorenter.cpdetector.io.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;

public class DataUtil {

    public static Charset getCharset(File file) {
        CodepageDetectorProxy detectorProxy = getCodePageDetector();
        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
            return detectorProxy.detectCodepage(bufferedInputStream, 512);
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }

    public static CodepageDetectorProxy getCodePageDetector() {
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
        detector.add(new ParsingDetector(false));
        detector.add(UnicodeDetector.getInstance());
        detector.add(JChardetFacade.getInstance());
        detector.add(ASCIIDetector.getInstance());
        return detector;
    }

    public static String getFileSize(Long size) {
        if (size == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (size < 1024) {
            fileSizeString = df.format((double)size) + "B";
        } else if (size < 1048576) {
            fileSizeString = df.format((double) size / 1024) + "K";
        } else if (size < 1073741824) {
            fileSizeString = df.format((double) size / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) size / 1073741824) + "G";
        }
        return fileSizeString;
    }

}
