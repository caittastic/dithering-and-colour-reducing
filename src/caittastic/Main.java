package caittastic;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashSet;

import static java.lang.Math.*;

public class Main{


  public static void main(String[] args){
    Long startTime = System.nanoTime();
    Long progress = 0L;

    String inputFileName = "input\\" + "ranma.png";
    String outputFileName = "output\\" + System.nanoTime() + "-"+inputFileName.substring(6);
    String paletteFileName = "pico pallet.png";

    try{
      //get palette as bufferedImage
      BufferedImage paletteImg = ImageIO.read(new File("input\\" + paletteFileName));

      //get all the colours from the palette into an array
      HashSet<Color> palette = new HashSet<Color>();
      for(int x = 0; x < paletteImg.getWidth(); x++){
        for(int y = 0; y < paletteImg.getHeight(); y++){
          Color colour = new Color(paletteImg.getRGB(x, y));
          palette.add(colour);
        }
      }

      //get input file as bufferedImage
      BufferedImage originalImage = ImageIO.read(new File(inputFileName));
      BufferedImage processedImage = ImageIO.read(new File(inputFileName));
      Long totalProgress = (long)processedImage.getWidth() * processedImage.getHeight();

      double totalError = 0;
      //iterate over every pixel in the processedImage
      for(int y = 0; y < processedImage.getWidth(); y++){
        for(int x = 0; x < processedImage.getHeight(); x++){
          double smallestError = 10000;
          Color closestColour = null;

          //get the colour of the current pixel
          int baseRGB = processedImage.getRGB(x, y);
          int baseR = (baseRGB & 0xff0000) >> 16;
          int baseG = (baseRGB & 0xff00) >> 8;
          int baseB = baseRGB & 0xff;

          //itterate over the palette
          for(Color paletteColour: palette){
            //get the r, g, b differences between the current pixels colour and a colour in the palette
            int rDif = abs(paletteColour.getRed() - baseR);
            int gDif = abs(paletteColour.getGreen() - baseG);
            int bDif = abs(paletteColour.getBlue() - baseB);

            //find the error between the two colours
            double error = Math.sqrt(rDif * rDif + gDif * gDif + bDif * bDif);
            //store the colour with the smallest error
            if(smallestError >= error){
              smallestError = error;
              closestColour = paletteColour;
            }
          }
          //set current pixel to closest colour
          processedImage.setRGB(x, y, closestColour.getRGB());

          int rDif = baseR - closestColour.getRed();
          int gDif = baseG - closestColour.getGreen();
          int bDif = baseB - closestColour.getBlue();

          //following floyd-steinberg, distribute 7/16 error to east, 5/16 to south, 3/16 to south-west, 1/16 to south-east
          distributeError(x + 1, y, processedImage, (double)7 / 16, rDif, gDif, bDif);
          distributeError(x, y + 1, processedImage, (double)5 / 16, rDif, gDif, bDif);
          distributeError(x - 1, y + 1, processedImage, (double)3 / 16, rDif, gDif, bDif);
          distributeError(x, y + 1, processedImage, (double)1 / 16, rDif, gDif, bDif);

          //find the error between the current pixel in the base image and processed image
          totalError += getColourDifference(originalImage.getRGB(x, y), processedImage.getRGB(x, y));

          progress += 1;
          System.out.print(new DecimalFormat("0.00").format(((double)progress / (double)totalProgress) * 100) + "%");
          System.out.printf("\r");
        }
      }

      //output the finished processedImage
      File outputFile = new File(outputFileName);
      ImageIO.write(processedImage, "png", outputFile);

      //check if the processedImage generated doesnt have colours not in the pallet
      if(!isValid(processedImage, palette))
        System.out.println("INVALID IMAGE GENERATED!!!!!!!!!!!!!!!!!!!!!");
      System.out.println("total error: " + totalError);
    } catch(IOException ignored){}

    //show the output image in a window
    ImageIcon image = new ImageIcon(outputFileName);
    int frameSize = 750;
    int halfFrameSize = frameSize / 2;
    JFrame f = new JFrame(outputFileName.substring(7));
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    f.setBounds((screenSize.width / 2) - halfFrameSize, (screenSize.height / 2) - halfFrameSize, frameSize, frameSize);
    int minWH = min(frameSize / image.getIconWidth(), frameSize / image.getIconHeight());
    image.setImage(image.getImage().getScaledInstance(image.getIconWidth() * minWH, image.getIconHeight() * minWH, Image.SCALE_DEFAULT));
    f.add(new JLabel(image));
    f.pack();
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setVisible(true);

    System.out.println();
    System.out.println("completed in: " + new DecimalFormat("0.000").format((double)(System.nanoTime() - startTime) / 1000000000) + "s");
  }

  private static double getColourDifference(int oRGB, int pRGB){
    int oR = (oRGB & 0xff0000) >> 16;
    int oG = (oRGB & 0xff00) >> 8;
    int oB = oRGB & 0xff;

    int pR = (pRGB & 0xff0000) >> 16;
    int pG = (pRGB & 0xff00) >> 8;
    int pB = pRGB & 0xff;

    double rDif = abs(oR - pR);
    double gDIf = abs(oG - pG);
    double bDif = abs(oB - pB);

    return sqrt(rDif * rDif + gDIf * gDIf + bDif * bDif);
  }

  private static void distributeError(int x, int y, BufferedImage image, double distributionFraction, int rDif, int gDif, int bDif){
    if(x >= 0 && x < image.getWidth() && y < image.getHeight()){
      Color oldColour = new Color(image.getRGB(x, y));
      image.setRGB(x, y, new Color(
              mid(0, (int)(oldColour.getRed() + rDif * distributionFraction), 255),
              mid(0, (int)(oldColour.getGreen() + gDif * distributionFraction), 255),
              mid(0, (int)(oldColour.getBlue() + bDif * distributionFraction), 255)
      ).getRGB());
    }
  }

  private static boolean isValid(BufferedImage image, HashSet<Color> palette){
    for(int x = 0; x < image.getWidth(); x++){
      for(int y = 0; y < image.getHeight(); y++){
        Color checkColour = new Color(image.getRGB(x, y));
        if(!palette.contains(checkColour))
          return false;
      }
    }
    return true;
  }

  private static int mid(int a, int b, int c){
    return max(a, (min(b, c)));
  }


}
