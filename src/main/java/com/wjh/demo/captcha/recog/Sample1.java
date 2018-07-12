package com.wjh.demo.captcha.recog;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * 验证码地址 http://vote.sun0769.com/include/captcha.asp
 * @see https://blog.csdn.net/lb245557472/article/details/72681836
 * @author Clement
 *
 */
public class Sample1 {

    private static final String CAPTCHA_URL = "http://vote.sun0769.com/include/captcha.asp";
    private static final String BASE_IMAGE_PATH = "D:\\Workspaces\\eclipse-workspace\\captchaRecog\\img\\";

    /**
     * 下载验证码
     * @throws IOException 
     */
    private void downloadCaptcha() throws IOException {
        for(int i = 0; i < 10; i++) {
            BufferedImage bi = ImageIO.read(new URL(CAPTCHA_URL));
            File file = new File(BASE_IMAGE_PATH + i + ".png");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            ImageIO.write(bi, "png", file);
        }
    }

    private int getAverageGrayThreshold(BufferedImage bi) {
        int total = 0;
        int w = bi.getWidth();
        int h = bi.getHeight();
        for(int x = 0; x < w; x++) {
            for(int y = 0; y < h; y++) {
                int rgb = bi.getRGB(x, y);
                Color color = new Color(rgb);
                int grayVal = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
                total += grayVal;
            }
        }
        int average = total / (w * h);
        return average;
    }

    /**
     * 二值化
     * @throws IOException 
     */
    private void binaryzation() throws IOException {
        for (int index = 0; index < 10; index++) {
            File file = new File(BASE_IMAGE_PATH + index + ".png");
            BufferedImage bi = ImageIO.read(file);
            int w = bi.getWidth();
            int h = bi.getHeight();
            int grayThreshold = getAverageGrayThreshold(bi);
            for(int i = 0; i < w; i++) {
                for(int j = 0; j < h; j++) {
                    Color color = new Color(bi.getRGB(i, j));
                    int r = color.getRed();
                    int g = color.getGreen();
                    int b = color.getBlue();
                    int grayVal = (r + g + b) / 3;
                    if (grayVal > grayThreshold) {
                        bi.setRGB(i, j, Color.WHITE.getRGB());
                    } else {
                        bi.setRGB(i, j, Color.BLACK.getRGB());
                    }
                }
            }
            File binaryFile = new File(BASE_IMAGE_PATH + "\\binary\\" + index + ".png");
            if (!binaryFile.exists()) {
                binaryFile.getParentFile().mkdirs();
            }
            binaryFile.createNewFile();
            ImageIO.write(bi, "png", binaryFile);
        }
    }

    /**
     * 图像腐蚀算法
     */
    private void corrosion(BufferedImage bi, int[][] img, int x, int y) {
        int w = bi.getWidth();
        int h = bi.getHeight();

        boolean flag = false;
        // 上下都为白色，清除该点
        if (y > 0 && y+1 < h && isWhite(img[x][y-1]) && isWhite(img[x][y+1])) {
            flag = true;
        }
        // 左右都为白色，清除该点
        if (x > 0 && x+1 < w && isWhite(img[x-1][y]) && isWhite(img[x+1][y])) {
            flag = true;
        }
        if (x > 0 && y > 0 && x + 1 < w && y + 1 < h) {
            // 左上和右下都为白色，清除该点
//            if (isWhite(img[x-1][y-1]) && isWhite(img[x+1][y+1])) {
//                flag = true;
//            }
            // 左下和右上都为白色，清除该点
//            if (isWhite(img[x+1][y-1]) && isWhite(img[x-1][y+1])) {
//                flag = true;
//            } 
        }

        int aroundPoints = 0;
        int[][] points = {{x, y-1}, {x+1, y-1}, {x+1, y}, {x+1, y+1}, {x, y+1}, {x-1, y+1}, {x-1, y}, {x-1, y-1}};
        for(int i = 0; i < points.length; i++) {
            int[] point = points[i];
            int px = point[0];
            int py = point[1];
            if (px >= 0 && px < w && py >= 0 && py < h && isBlack(img[px][py])) {
                aroundPoints++;
            }
        }

        if (flag || aroundPoints <= 2) {
            bi.setRGB(x, y, -1);
        }
    }
    
    /**
     * 用图像腐蚀去除干扰线和噪点
     * @throws IOException 
     */
    private void removeDisturbLine() throws IOException {
        for (int index = 0; index < 10; index++) {
            File file = new File(BASE_IMAGE_PATH + "\\binary\\" + index + ".png");
            BufferedImage bi = ImageIO.read(file);
            int w = bi.getWidth();
            int h = bi.getHeight();
            int[][] img = new int[w][h];
            for(int i = 0; i < w; i++) {
                for(int j = 0; j < h; j++) {
                    img[i][j] = bi.getRGB(i, j);
                }
            }
            for(int x = 0; x < w; x++) {
                for(int y = 0; y < h; y++) {
                    corrosion(bi, img, x, y);
                }
            }
            File corrosionFile = new File(BASE_IMAGE_PATH + "\\corrosion\\" + index + ".png");
            if (!corrosionFile.exists()) {
                corrosionFile.getParentFile().mkdirs();
            }
            corrosionFile.createNewFile();
            ImageIO.write(bi, "png", corrosionFile);
        }
    }

    private void splitImage() throws IOException {
        for (int i = 0; i < 10; i++) {
            File file = new File(BASE_IMAGE_PATH + i + ".png");
            BufferedImage bi = ImageIO.read(file);
            int h = bi.getHeight();
            int numWidth = 13;
            for(int j = 0; j < 6; j++) {
                int startX = 26 + j * numWidth;
                BufferedImage img = bi.getSubimage(startX, 0, numWidth, h);
                File splitFile = new File(BASE_IMAGE_PATH + "\\split\\" + i + "\\" + j + ".png");
                if (!splitFile.exists()) {
                    splitFile.getParentFile().mkdirs();
                }
                splitFile.createNewFile();
                ImageIO.write(img, "png", splitFile);
            }
        }
    }

    public boolean isWhite(int colorInt) {  
        Color color = new Color(colorInt);
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return r > 230 && g > 230 && b > 230;  
    }

    public boolean isBlack(int colorInt) {
        return !isWhite(colorInt);
    }

    public static void main(String[] args) throws IOException {
        Sample1 sam = new Sample1();
//        sam.downloadCaptcha();
//        sam.binaryzation();
        sam.removeDisturbLine();
//        sam.splitImage();
    }
}