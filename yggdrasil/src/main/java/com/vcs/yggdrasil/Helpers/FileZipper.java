package com.vcs.yggdrasil.Helpers;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipOutputStream;
import com.vcs.yggdrasil.Helpers.Logger;

public class FileZipper{


    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                } else {
                    File dir = new File(filePath);
                    if(dir.mkdirs()){

                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(Paths.get(filePath)))) {
            byte[] bytesIn = new byte[1024];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    public static void zip(String[] files, String zipFilePath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
            zipOut.setLevel(Deflater.BEST_COMPRESSION);
            for (String filePath : files) {
                File fileToZip = new File(filePath);
                try (FileInputStream fis = new FileInputStream(fileToZip)) {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                }
            }
        }
    }


    public static void zipDir(String sourceDirPath, String zipFilePath) {
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
            zipOut.setLevel(Deflater.BEST_COMPRESSION);
            File sourceDir = new File(sourceDirPath);
            zipFile(sourceDir, sourceDir.getName(), zipOut);
        }
        catch (IOException e){
            Logger.log(Logger.LogLevel.ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }

//    public static void main(String[] args) {
//        String sourceDirPath = "C:/Users/Pratyush/Desktop/New folder (3)/.ygg";
//        String zipFilePath = "Assets";
//
////        zipDir(sourceDirPath, zipFilePath);
////        System.out.println("Directory zipped successfully.")
//
//        try {
//
//            unzip("Assets/.ygg.zip", "Assets/.ygg" );
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }



}
