package com.wjh.demo.captcha.recog;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

/**
 * 验证码地址 http://vote.sun0769.com/include/captcha.asp
 * @see https://blog.csdn.net/lb245557472/article/details/72681836
 * @author Clement
 *
 */
public class Sample1 {

    private static final String CAPTCHA_URL = "http://vote.sun0769.com/include/captcha.asp";
    private static final String BASE_IMAGE_PATH = System.getProperty("user.dir") + "\\img\\";

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
            File file = new File(BASE_IMAGE_PATH + "\\corrosion\\" + i + ".png");
            BufferedImage bi = ImageIO.read(file);
            int h = bi.getHeight();
            int numWidth = 13;
            for(int j = 0; j < 6; j++) {
                int startX = 26 + j * numWidth;
                BufferedImage img = bi.getSubimage(startX, 0, numWidth, h);
                img = getMaxConnectedRegion(img);
                File splitFile = new File(BASE_IMAGE_PATH + "\\split\\" + i + "\\" + j + ".png");
                if (!splitFile.exists()) {
                    splitFile.getParentFile().mkdirs();
                }
                splitFile.createNewFile();
                ImageIO.write(img, "png", splitFile);
            }
        }
    }

    private boolean isWhite(int colorInt) {  
        Color color = new Color(colorInt);
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return r > 230 && g > 230 && b > 230;  
    }

    private boolean isBlack(int colorInt) {
        return !isWhite(colorInt);
    }

    private int[][] toArrayData(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][] imgData = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                imgData[x][y] = img.getRGB(x, y);
            }
        }
        return imgData;
    }

    /**
     * 获取一副图片中最大的连通区域，并裁剪出该区域
     */
    private BufferedImage getMaxConnectedRegion(BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        Set<int[]> visted = new HashSet<>(); //保存已经访问过的黑点
        Set<int[]> maxRegion = new HashSet<>();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (!contains(visted, new int[] {x, y}) && isBlack(bi.getRGB(x, y))) { //找到连通区域的起点
                    Set<int[]> region = new HashSet<>();
                    getConnectedRegion(bi, x, y, region);
                    visted.addAll(region);
                    if (region.size() > maxRegion.size()) {
                        maxRegion = region;
                    }
                }
            }
        }
        int minX = w, minY = h, maxX = 0, maxY = 0;
        for (int[] region : maxRegion) {
            int x = region[0], y = region[1];
            if (minX > x) {
                minX = x;
            }
            if (minY > y) {
                minY = y;
            }
            if (maxX < x) {
                maxX = x;
            }
            if (maxY < y) {
                maxY = y;
            }
        }
        return bi.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * 找到从点(x, y)出发的连通区域, 当该点周围没有黑点时返回
     * @param bi
     * @param x
     * @param y
     * @param region 保存连通区域中已经访问过的点
     */
    private void getConnectedRegion(BufferedImage bi, int x, int y, Set<int[]> region) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[][] aroundPoints = {{x, y-1}, {x+1, y-1}, {x+1, y}, {x+1, y+1}, {x, y+1}, {x-1, y+1}, {x-1, y}, {x-1, y-1}};
        for(int i = 0; i < aroundPoints.length; i++) {
            int[] point = aroundPoints[i];
            int x1 = point[0];
            int y1 = point[1];
            if (x1 > 0 && x1 < w && y1 > 0 && y1 < h && !contains(region, point) && isBlack(bi.getRGB(x1, y1))) {
                region.add(point);
                getConnectedRegion(bi, x1, y1, region);
            }
        }
    }

    private boolean contains(Set<int[]> set, int[] point) {
        set = set.stream().filter(p -> p[0] == point[0] && p[1] == point[1]).collect(Collectors.toSet());
        return !set.isEmpty();
    }

    /**
     * 识别验证码
     * @throws IOException 
     */
    public void recognise() throws IOException {
        File splitFolder = new File(BASE_IMAGE_PATH + "\\split");
        File[] files = splitFolder.listFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File[] childs = file.listFiles();
            StringBuffer captcha = new StringBuffer();
            for (int j = 0; j < childs.length; j++) {
                BufferedImage bi = ImageIO.read(childs[j]);
                captcha.append(recogniseBaseSample(bi));
            }

            System.out.println("识别出验证码： " + captcha.toString());
            System.out.println("============================");
        }
    }

    /**
     * 根据模板进行识别
     * 
     * @param bi
     * @return
     * @throws IOException
     */
    private String recogniseBaseSample(BufferedImage bi) throws IOException {
        File file = new File(BASE_IMAGE_PATH + "\\sample\\");
        File[] samples = file.listFiles();
        Map<Double, File> similarMap = new HashMap<>();
        for (int i = 0; i < samples.length; i++) {
            BufferedImage sample = ImageIO.read(samples[i]);
            Double degree = getSimilarityDegree(bi, sample);
            similarMap.put(degree, samples[i]);
        }
        Optional<Double> max = similarMap.keySet().stream().max(Double::compare);
        File similarImg = similarMap.get(max.get());
        String name = similarImg.getName();
        // System.out.println(name + " ---相似度---> " + max.get());
        return name.substring(0, name.lastIndexOf("."));
    }

    /**
     * 获取两个图片的相似度
     * 
     * @param bi
     * @param sample
     * @return
     */
    private double getSimilarityDegree(BufferedImage bi, BufferedImage sample) {
        int w = bi.getWidth(), h = bi.getHeight();
        int w0 = sample.getWidth(), h0 = sample.getHeight();
        int samePoint = getSamePointCount(bi, 0, 0, sample);
        double degreeSample = samePoint / (w0 * h0 * 1d);
        double degreeSource = samePoint / (w * h * 1d);
        return degreeSample * degreeSource;
    }

    private int getSamePointCount(BufferedImage img1, int startX, int startY, BufferedImage img2) {
        int w1 = img1.getWidth(), h1 = img1.getHeight();
        int w2 = img2.getWidth(), h2 = img2.getHeight();
        // 裁剪为相同大小的图片
        int minW = w1 < w2 ? w1 : w2;
        int minH = h1 < h2 ? h1 : h2;
        BufferedImage img11, img22;
        if (w1 <= w2 && h1 >= h2) {
            img11 = img1.getSubimage(0, startY, minW, minH);
            img22 = img2.getSubimage(startX, 0, minW, minH);
        } else if (w1 > w2 && h1 < h2) {
            img11 = img1.getSubimage(startX, 0, minW, minH);
            img22 = img2.getSubimage(0, startY, minW, minH);
        } else if (w1 > w2) {
            img11 = img1.getSubimage(startX, startY, minW, minH);
            img22 = img2.getSubimage(0, 0, minW, minH);
        } else {
            img11 = img1.getSubimage(0, 0, minW, minH);
            img22 = img2.getSubimage(startX, startY, minW, minH);
        }
        // 求相同大小图片中相同点的个数
        int maxSamePoint = 0;
        for (int x = 0; x < minW; x++) {
            for (int y = 0; y < minH; y++) {
                if (isBlack(img11.getRGB(x, y)) && isBlack(img22.getRGB(x, y))) {
                    maxSamePoint++;
                }
            }
        }
        int maxW = w1 > w2 ? w1 : w2;
        int maxH = h1 > h2 ? h1 : h2;
        if (startX + minW < maxW || startY + minH < maxH) {
            if (startX + minW < maxW) {
                startX++;
            } else if (startY + minH < maxH) {
                startY++;
                startX = 0;
            }
            int samePoint = getSamePointCount(img1, startX, startY, img2);
            if (samePoint > maxSamePoint) {
                maxSamePoint = samePoint;
            }
        }
        return maxSamePoint;
    }

    public static void main(String[] args) throws IOException {
        Sample1 sam = new Sample1();
        sam.downloadCaptcha();
        sam.binaryzation();
        sam.removeDisturbLine();
        sam.splitImage();
        sam.recognise();
    }
}
