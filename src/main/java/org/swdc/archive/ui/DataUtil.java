package org.swdc.archive.ui;

import info.monitorenter.cpdetector.io.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

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

}
