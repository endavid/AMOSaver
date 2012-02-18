package amos.img;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.image.IndexColorModel;

public class PlanarImage {

    BufferedImage m_img;
    int m_width;
    int m_height;
    int m_numBitplanes;
    
    public PlanarImage(int width, int height, int depth, byte[] planarData) {
        m_img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, createGrayGradientPalette());
        m_width=width;
        m_height=height;
        m_numBitplanes = depth;
        PlanarToChunky(planarData);
    }
    
    public BufferedImage GetAsBufferedImage() {
        return m_img ;
    }
    
    private void PlanarToChunky(byte[] planarData) {
        WritableRaster raster = m_img.getRaster();
        int numBytesWidth = m_width >> 3 ; // 8 pixels per byte
        int index = 0;
        for (int bit=0;bit<m_numBitplanes;++bit) {
            for (int j=0;j<m_height;++j){
                for (int i=0;i<numBytesWidth;++i) {
                    int b = 0xff & (int)planarData[index++];
                    for (int p = 0; p<8; ++p) {
                        int bitPixel = (b>>p) & 0x01;
                        bitPixel = bitPixel << bit ;
                        int x = 8 * i + (7-p) ;
                        if (bit > 0) bitPixel |= raster.getSample(x,j,0);
                        raster.setSample(x,j,0, bitPixel);
                    }
                }
            }
        }
    }
    /**
     * Create a simple color model with a gray gradient
     */
    private static IndexColorModel createGrayGradientPalette() {        
        byte[] r = new byte[32];
        byte[] g = new byte[32];
        byte[] b = new byte[32];
        
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) (8*i);
            g[i] = (byte) (8*i);
            b[i] = (byte) (8*i);
        }
        return new IndexColorModel(5, 32, r, g, b);
    }
    
    /**
     * Decodes 64 bytes of data into a 32-color paletter
     */
    public static IndexColorModel decodeColorPalette(byte[] paletteData) {
        byte[] r = new byte[32];
        byte[] g = new byte[32];
        byte[] b = new byte[32];
        
        for (int i = 0; i < r.length; i++) {
            byte or = paletteData[2*i];
            byte gb = paletteData[2*i+1];
            r[i] = (byte) ( (0x0f & or)<<4 );
            g[i] = (byte) ( (0x0f & (gb>>4))<<4 );
            b[i] = (byte) ( (0x0f & gb)<<4 );
        }
        return new IndexColorModel(5, 32, r, g, b);
    }
    
}