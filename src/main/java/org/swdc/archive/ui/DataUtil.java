package org.swdc.archive.ui;

import info.monitorenter.cpdetector.io.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

public class DataUtil {

    public static Charset getCharset(File file) {
        CodepageDetectorProxy detectorProxy = getCodePageDetector();
        try {
            return detectorProxy.detectCodepage(file.toURI().toURL());
        } catch (Exception e) {
            return Charset.defaultCharset();
        }
    }

    public static CodepageDetectorProxy getCodePageDetector() {
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
        detector.add(UnicodeDetector.getInstance());
        detector.add(JChardetFacade.getInstance());
        detector.add(ASCIIDetector.getInstance());
        return detector;
    }

}
