// Copyright (C) 2012 David Gavilan Ruiz
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package amos.io ;

import java.io.File ;
import java.io.FileInputStream ;
import java.io.FileNotFoundException ;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

import amos.img.*;

/**
 * @see http://www.exotica.org.uk/wiki/AMOS_file_formats
 * @see http://www.amigacoding.com/index.php/AMOS:Picture_Bank_format
 * @see http://www.amigacoding.com/index.php/AMOS:Extensions
 */
public class AMOSFileInputStream
{
    public static final String[] VALID_HEADERS = {
        // 16-byte header, Tested?, Saved from which AMOS?
        "AMOS Pro101V\0\0\0\0",//	Yes	AMOS Professional
        "AMOS Basic V134 ",//	Yes	AMOS Pro, but AMOS 1.3 compatible
        "AMOS Basic V1.3 ",//	Yes	AMOS The Creator v1.3
        "AMOS Basic V1.00",//	Yes	AMOS The Creator v1.0 - v1.2
        "AMOS Pro101v\0\0\0\0",//	No	AMOS Professional
        "AMOS Pro101v\0\0\0\3",//	No	AMOS Professional
        "AMOS Basic v134 ",//	No	AMOS Pro, but AMOS 1.3 compatible
        "AMOS Basic v1.3 ",//	No	AMOS The Creator v1.3
        "AMOS Basic v1.00"//	No	AMOS The Creator v1.0 - v1.2
    };
    FileInputStream m_stream ;
    boolean m_isSanityTested ;
    long    m_sourceSizeBytes ;
    long    m_readBytes ;
    int     m_numBanks ;
    int     m_currentBankSize;
    int     m_currentBankNumber;
    byte[]  m_tmp4B = {0,0,0,0};
    byte[]  m_tmp2B = {0,0};
    byte[]  m_tmp1B = {0};
    Map<Integer,String> m_tokenMap;
    Map<Integer,String> m_extensions;
    
    boolean m_isVerbose; 
    
    public boolean isSanityTested()
    {
        return m_isSanityTested ;
    }
    
    public AMOSFileInputStream(java.io.File file, boolean isVerbose) 
        throws java.io.FileNotFoundException, amos.io.UnsupportedFormat, java.io.IOException
    {
        m_isVerbose = isVerbose;
        m_stream = new FileInputStream(file);
        String headerString ;
        try {
            byte[] header = new byte[16];
            m_stream.read(header);
            headerString = new String(header);
            
        } catch (java.io.IOException exc) {
            throw( new amos.io.UnsupportedFormat("I/O error") );
        }
        
        if (!isValidHeader(headerString)) {
            throw( new amos.io.UnsupportedFormat(headerString) );
        }
        
        m_stream.read(m_tmp4B); // big endian unsigned
        // there's no unsigned int in Java, so store it in a long
        m_sourceSizeBytes = readUnsignedInt(m_tmp4B);
        m_readBytes = 0;
        m_numBanks = 0;
        m_currentBankSize = 0;
        m_currentBankNumber = 0;

        m_tokenMap = new HashMap<Integer,String>();
        m_extensions = new HashMap<Integer,String>();
        _initTokenMap();
        //System.out.println("source size: "+m_sourceSizeBytes);
    }
    
    /**
     * Checks if there are still tokens to read
     */
    public boolean isSourceCodeEnd()
    {
        return ( m_readBytes >= m_sourceSizeBytes );
    }
    
    /**
     * Just returns the number of the bank currently being processed
     */
    public int getCurrentBankNumber()
    {
        return m_currentBankNumber;
    }
    
    /**
     * Checks the number of memory banks, immediately after the source code.
     */
    public int readNumBanks() throws java.io.IOException, java.io.StreamCorruptedException
    {
        // Check for AmBs string
        m_stream.read(m_tmp4B);
        String text = new String(m_tmp4B);
        if (!text.equals("AmBs")) {
            throw( new java.io.StreamCorruptedException("File doesn't contain description of memory banks!") ); 
        }
        m_stream.read(m_tmp2B);
        m_numBanks = readUnsignedWord(m_tmp2B);
        return m_numBanks ;
    }
    
    /**
     * Checks the type of memory bank to be read
     */
    public AMOSBankType readBankType() throws java.io.IOException, java.io.StreamCorruptedException
    {
        m_stream.read(m_tmp4B);
        String text = new String(m_tmp4B);
        AMOSBankType bankType = AMOSBankType.GetAMOSBankTypeById(text);
        if (bankType==AMOSBankType.UNKNOWN) {
            throw( new java.io.StreamCorruptedException("Unknown memory bank!") ); 
        }
        return bankType;
    }
    
    /**
     * Reads Sprites or Icons
     */
    public List<BufferedImage> readImages() throws java.io.IOException, java.io.StreamCorruptedException
    {
        m_stream.read(m_tmp2B);
        int numImages = readUnsignedWord(m_tmp2B);
        List<BufferedImage> imgList = new ArrayList<BufferedImage>(numImages);
        if (m_isVerbose) {
            System.err.println("... reading "+numImages+" images");
        }
        for (int i=0;i<numImages;++i) {
            int width = 0, height = 0, depth = 0;
            int hotspotX = 0, hotspotY = 0;
            int dataSize = 0;
            byte[] imageData = null ;
            
            m_stream.read(m_tmp2B);
            width = readUnsignedWord(m_tmp2B);
            m_stream.read(m_tmp2B);
            height = readUnsignedWord(m_tmp2B);
            m_stream.read(m_tmp2B);
            depth = readUnsignedWord(m_tmp2B);
            if (depth<1 || depth>6) {
                throw( new java.io.StreamCorruptedException("Incompatible image depth("+depth+")!") );
            }
            m_stream.read(m_tmp2B);
            hotspotX = readUnsignedWord(m_tmp2B);
            m_stream.read(m_tmp2B);
            hotspotY = readUnsignedWord(m_tmp2B);
            dataSize = 2 * width * height * depth ;
            if (m_isVerbose) {
                System.err.println("img("+i+")="+(16*width)+"x"+height+"x"+depth+", ("+hotspotX+", "+hotspotY+")");
            }
            if ( dataSize > 0 ) {
                imageData = new byte[dataSize];
                m_stream.read(imageData);
                // now convert planar data... 
            }
            // construct image 
            // -- width is in 16-bit words
            PlanarImage img = new PlanarImage(width*16, height, depth, imageData);
            // add to list
            imgList.add(img.GetAsBufferedImage());
        }
        // after all the images comes the color palette
        byte[] paletteData = new byte[64];
        m_stream.read(paletteData);
        // decode color palette
        IndexColorModel palette = PlanarImage.decodeColorPalette(paletteData);
        // replace color palettes
        for (int i=0;i<imgList.size();++i) {
            BufferedImage image = imgList.get(i);
            imgList.set(i, new BufferedImage(palette,image.getRaster(), false, null));
        }
        
        return imgList ;
    }
    
    /**
     * Gets the information about this memory bank.
     * For unsupported formats, it returns MEMORYBANKS as generic type
     */
    public AMOSBankType readMemoryBankType() throws java.io.IOException
    {
        int bankNumber = 0;
        boolean isChipMemory = false;
        int bankSize = 0;
        int flags = 0;
        String bankName = "";
        byte[] tmp8B = new byte[8];
        // read header
        m_stream.read(m_tmp2B);
        bankNumber = readUnsignedWord(m_tmp2B);
        m_stream.read(m_tmp2B);
        isChipMemory = (readUnsignedWord(m_tmp2B)==0);
        m_stream.read(m_tmp4B);
        flags = 0x0f & (m_tmp4B[0] >> 4);
        m_tmp4B[0] = (byte)( 0x0f & m_tmp4B[0] );
        bankSize = (int)readUnsignedInt(m_tmp4B) - 8;
        m_stream.read(tmp8B);
        bankName = new String(tmp8B);
        if (m_isVerbose) {
            System.err.println(" Bank "+bankNumber+": "+bankName+(isChipMemory?" (chip) ":" ")+bankSize+" bytes");
        }
        AMOSBankType bankType = AMOSBankType.GetAMOSBankTypeById(bankName);
        if (bankType==AMOSBankType.UNKNOWN) {
            System.err.println("Unknown memory bank: "+bankName);
        }
        m_currentBankSize = bankSize ;
        m_currentBankNumber = bankNumber;
        return bankType;
    }
    
    /**
     * For unknown bank types, just reads the data as a byte array.
     */
    public byte[] readMemoryBankRaw() throws java.io.IOException
    {
        if (m_currentBankSize > 0) {
            byte[] data = new byte[m_currentBankSize];
            m_stream.read(data);
            m_currentBankSize = 0;
            return data;
        }
        return null;
    }
    
    /**
     * Reads a Packed Picture (Pac.Pic.)
     * @see http://www.exotica.org.uk/wiki/AMOS_Pac.Pic._format
     */
    public BufferedImage readPacPic() throws java.io.IOException
    {
        int width, height, numColors, numBitplanes;
        // Screen header
        // --------------------------------------------
        m_stream.read(m_tmp4B); // fixed ID
        width = _read2BAsUInt(); // width in pixels
        height = _read2BAsUInt(); // height in pixels
        m_stream.read(m_tmp2B); // hardware top-left X
        m_stream.read(m_tmp2B); // hardware top-left Y
        m_stream.read(m_tmp2B); // hardware screen width
        m_stream.read(m_tmp2B); // hardware screen height
        m_stream.read(m_tmp2B); // unknown
        m_stream.read(m_tmp2B); // unknown
        // Value of the Amiga BPLCON0 register, which details the hardware screen mode such as HAM, hires or interlaced
        m_stream.read(m_tmp2B);
        // Number of colours on screen. 
        numColors = _read2BAsUInt(); // 2, 4, 8, 16, 32, 64 (EHB) or 4096 (HAM)
        numBitplanes = _read2BAsUInt(); // 1..6
        // 32 2-byte palette entries in the Amiga COLORxx register format.
        byte[] paletteData = new byte[64];
        m_stream.read(paletteData);
        // Picture header
        // --------------------------------------------
        _read4BAsInt(); // fixed ID
        _read2BAsUInt(); // X coordinate offset in bytes of the picture within the screen itself.
        _read2BAsUInt(); // Y coordinate offset in lines (vertical pixels) of the picture within the screen itself.
        _read2BAsUInt(); // picture width in bytes.
        _read2BAsUInt(); // picture height in "line lumps"
        _read2BAsUInt(); // number of lines in a "line lump"
        _read2BAsUInt(); // number of bitplanes in the picture
        _read4BAsUInt(); // offset to the RLEDATA stream, relative to the picture header ID's offset.
        _read4BAsUInt(); // offset to the POINTS stream, relative to the picture header ID's offset.
        
        // @todo Decompress picture data
        // ---------------------------------------------
        m_currentBankSize -= 114; // headers
        byte[] data = readMemoryBankRaw();
        
        // Create PlanarImage
        // ---------------------------------------------
        IndexColorModel palette = PlanarImage.decodeColorPalette(paletteData);
        PlanarImage img = new PlanarImage(1,1,1,data,palette);

        return img.GetAsBufferedImage();
    }
    
    /**
     * Reads tokens from a line of AMOS BASIC code
     */
    public String readLine() throws java.io.IOException, java.io.StreamCorruptedException
    {
        String line = "";
        m_stream.read(m_tmp1B); // unsigned byte
        int lineLength = readUnsignedByte(m_tmp1B) ; // in words (2 bytes)
        m_readBytes += lineLength * 2 ; // in bytes
        
        m_stream.read(m_tmp1B);
        int indentLevel = readUnsignedByte(m_tmp1B) ;
        for (int i=1; i<indentLevel; ++i) line = line + " ";
        
        int tokenID = 0 ;
        //System.out.println("line l: "+lineLength+" indent: "+indentLevel);

        
        // big parsing loop
        int readWords = 1 ;
        while (readWords<lineLength) {
            m_stream.read(m_tmp2B);
            ++readWords ;
            tokenID = readUnsignedWord(m_tmp2B);
            switch(tokenID) {
                case 0: // NULL token
                    break ;
                // Specially printed tokens
                // -------------------------------------------
                case 0x0006: // Variable reference
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    int strlength = _read1BAsUInt(); // string size
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) ;
                    if ( (flag&0x01)!=0 ) {
                        line = line + "#" ; // float reference
                    } else if ( (flag&0x02)!=0 ) {
                        line = line + "$" ; // string reference
                    }
                    //m_tmp2B[0] = str[strlength-2];
                    //m_tmp2B[1] = str[strlength-1];
                    //tokenID = readUnsignedWord(m_tmp2B);
                    break;
                }
                case 0x000C: // Label
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    int strlength = _read1BAsUInt(); // string size
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) +":" ;
                    break;
                }
                case 0x0012: // Procedure call reference
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    int strlength = _read1BAsUInt(); // string size
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) ;
                    if ( (flag&0x01)!=0 ) {
                        line = line + "#" ; // float reference
                    } else if ( (flag&0x02)!=0 ) {
                        line = line + "$" ; // string reference
                    }
                    break;
                }
                case 0x0018: // Label reference
                {
                    m_stream.read(m_tmp2B); // unknown purpose 
                    int strlength = _read1BAsUInt(); // string size
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    m_stream.read(m_tmp1B); // flag
                    int flag = readUnsignedByte(m_tmp1B);
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 2 + (strlength>>1);
                    line = line + (new String(str)) ;
                    if ( (flag&0x01)!=0 ) {
                        line = line + "#" ; // float reference
                    } else if ( (flag&0x02)!=0 ) {
                        line = line + "$" ; // string reference
                    }
                    break;
                }
                case 0x0026: // String with double quotes
                {
                    // length of the string 
                    int strlength = _read2BAsUInt();
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + "\""+(new String(str))+"\"" ;
                    break;
                }
                case 0x002E: // String with single quotes
                {
                    // length of the string 
                    int strlength = _read2BAsUInt();
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + "\'"+(new String(str))+"\'" ;
                    break;
                }
                case 0x001E: // Binary integer value
                {
                    int value = _read4BAsInt();
                    readWords += 2;
                    line = line + "%"+Integer.toBinaryString(value) ;
                    break;
                }
                case 0x0036: // Hexadecimal integer value
                {
                    int value = _read4BAsInt();
                    readWords += 2;
                    line = line + "$"+Integer.toHexString(value) ;
                    break;
                }
                case 0x003E: // Decimal integer value
                {
                    int value = _read4BAsInt();
                    readWords += 2;
                    line = line +Integer.toString(value) ;
                    break;
                }
                case 0x0046: // Float value
                {
                    float value = _read4BAsFloat();
                    readWords += 2;
                    line = line + Float.toString(value) ;
                    break;
                }
                case 0x004E: // Extension command
                {
                    int extNumber = _read1BAsUInt();
                    m_stream.read(m_tmp1B); // unused
                    // signed 16-bit offset into extension's token table
                    int offset = _read2BAsInt();
                    readWords += 2;
                    String tokenStr = m_extensions.get((extNumber<<16)|offset);
                    if ( tokenStr != null ) {
                        line = line + tokenStr ;
                    } else {
                        line = line +"[ext"+extNumber+"(0x"+Integer.toHexString(offset)+")] " ;
                    }
                    break;
                }                    
                // Specially sized tokens
                // -------------------------------------------
                case 0x023C: // FOR
                {
                    line = line + "For ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0250: // REPEAT
                {
                    line = line + "Repeat";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0268: // WHILE
                {
                    line = line + "While ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x027E: // DO
                {
                    line = line + "Do ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x02BE: // IF
                {
                    line = line + "If ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x02D0: // ELSE
                {
                    line = line + " Else ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0404: // DATA
                {
                    line = line + "Data ";
                    m_stream.read(m_tmp2B); // unknown purpose
                    readWords += 1;
                    break;
                }
                case 0x0290: // EXIT IF
                {
                    line = line + "Exit If ";
                    m_stream.read(m_tmp4B); // unknown purpose
                    readWords += 2;
                    break;
                }
                case 0x029E: // EXIT
                {
                    line = line + "Exit ";
                    m_stream.read(m_tmp4B); // unknown purpose
                    readWords += 2;
                    break;
                }
                case 0x0316: // ON
                {
                    line = line + "On ";
                    m_stream.read(m_tmp4B); // unknown purpose
                    readWords += 2;
                    break;
                }                    
                case 0x0376: // PROCEDURE
                {
                    line = line + "Procedure ";
                    m_stream.read(m_tmp4B); // number of bytes to corresponding End Proc line
                    m_stream.read(m_tmp2B); // part of seed for encryption
                    m_stream.read(m_tmp1B); // flags
                    m_stream.read(m_tmp1B); // part of seed for encryption
                    readWords += 4;
                    break;
                }                    
                case 0x064A: // REM
                case 0x0652: // REM type 2
                {
                    line = line + m_tokenMap.get(tokenID);
                    m_stream.read(m_tmp1B); // unused 
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + (new String(str)) ;
                    break;
                }
                default:
                {
                    String tokenStr = m_tokenMap.get(tokenID);
                    if ( tokenStr != null ) {
                        line = line + tokenStr ;
                    } else {
                        line = line + ("[0x" + Integer.toHexString(tokenID)+"]");                    
                    }
                }
            }
        }
        
        // last token is supposed to be null
        if ( tokenID != 0 ) {
            throw( new java.io.StreamCorruptedException("Line didn't end with a NULL token, but 0x"+Integer.toHexString(tokenID)+" ("+line+")") );
        }

        // stop at the first line just for testing
        //m_readBytes = m_sourceSizeBytes ;

        return line;
    } // end readLine()
    
    
    public boolean isValidHeader(String header)
    {
        m_isSanityTested = true ;
        for (int i=0; i<4; ++i) {
            if (header.equals(VALID_HEADERS[i])) return true ;
        }
        m_isSanityTested = false ;
        for (int i=4; i<VALID_HEADERS.length; ++i) {
            if (header.equals(VALID_HEADERS[i])) return true ;
        }
        return false ;
    }
    
    // functions to read values from current stream
    // -------------------------------------------------------
    private float _read4BAsFloat() throws java.io.IOException {
        m_stream.read(m_tmp4B);
        return readFloat(m_tmp4B);
    }
    private int _read4BAsInt() throws java.io.IOException {
        m_stream.read(m_tmp4B);
        return readSignedInt(m_tmp4B);
    }
    private long _read4BAsUInt() throws java.io.IOException {
        m_stream.read(m_tmp4B);
        return readUnsignedInt(m_tmp4B);
    }
    private int _read2BAsInt() throws java.io.IOException {
        m_stream.read(m_tmp2B);
        return readSignedWord(m_tmp2B);
    }
    private int _read2BAsUInt() throws java.io.IOException {
        m_stream.read(m_tmp2B);
        return readUnsignedWord(m_tmp2B);
    }
    private int _read1BAsUInt() throws java.io.IOException {
        m_stream.read(m_tmp1B);
        return readUnsignedByte(m_tmp1B);
    }
    
    /**
     * bits 31-8: mantissa (24 bits)
     * bit 7: sign bit. Positive if 0, negative if 1
     * bits 6-0: exponent
     * FP result =[(-1)^SIGN] * [2^(EXP - 0x40)] * [MANTISSA / 0x1000000]
     */
    public static float readFloat(byte[] bigEndian)
    {
        int mantissa =
            ((0x000000ff & (int)bigEndian[0]) << 16) |
            ((0x000000ff & (int)bigEndian[1]) << 8) |
            ((0x000000ff & (int)bigEndian[2])) ;
        int exponent = (int)(0x7f & bigEndian[3]) - 0x040 ;
        int sign = 0x01 & (int)(bigEndian[3]>>7) ;
        float f = ((float)mantissa/(float)0x01000000) 
            * (float)Math.pow(2.0, (double)exponent);
        //System.err.println("mantissa: "+mantissa+", exp: "+exponent);
        return (sign==1)?-f:f;
    }
    public static int readSignedWord(byte[] bigEndianSigned)
    {
        int value = ((0x00ff & (int)bigEndianSigned[0]) << 8) |
            ((0x00ff & (int)bigEndianSigned[1]) );
        return value ;
    }    
    public static int readSignedInt(byte[] bigEndianSigned)
    {
        int value = ((0x000000ff & (int)bigEndianSigned[0]) << 24) |
                    ((0x000000ff & (int)bigEndianSigned[1]) << 16) |
                    ((0x000000ff & (int)bigEndianSigned[2]) <<  8) |
                    ((0x000000ff & (int)bigEndianSigned[3]) ) ;
        return value ;
    }
    public static long readUnsignedInt(byte[] bigEndianUnsigned)
    {
        long value = 0xFFFFFFFFL & (long)(
                ((0x000000ff & (int)bigEndianUnsigned[0]) << 24) |
                ((0x000000ff & (int)bigEndianUnsigned[1]) << 16) |
                ((0x000000ff & (int)bigEndianUnsigned[2]) <<  8) |
                ((0x000000ff & (int)bigEndianUnsigned[3]) ) );
        return value ;
    }
    public static int readUnsignedWord(byte[] bigEndianUnsigned)
    {
        int value = 0xFFFF & (int)(
                ((0x00ff & (int)bigEndianUnsigned[0]) << 8) |
                ((0x00ff & (int)bigEndianUnsigned[1]) ) );
        return value ;
    }
    public static int readUnsignedByte(byte[] bigEndianUnsigned)
    {
        int value = 0xff & (int)bigEndianUnsigned[0] ;
        return value ;
    }
    
    private void _initTokenMap()
    {
        m_tokenMap.put(0x0054," : ");
        m_tokenMap.put(0x005C,",");
        m_tokenMap.put(0x0064,";"); 
        m_tokenMap.put(0x0074,"("); 
        m_tokenMap.put(0x007c,")"); 
        m_tokenMap.put(0x0094," To ");  
        m_tokenMap.put(0x00f2,"Inkey$");  
        m_tokenMap.put(0x012c,"Double Buffer"); 
        m_tokenMap.put(0x0246,"Next "); 
        m_tokenMap.put(0x025c,"Until "); 
        m_tokenMap.put(0x02c6," Then "); 
        m_tokenMap.put(0x02a8,"Goto "); 
        m_tokenMap.put(0x02b2,"Gosub "); 
        m_tokenMap.put(0x02da,"End If"); 
        m_tokenMap.put(0x0356," Step "); 
        m_tokenMap.put(0x0360,"Return"); 
        m_tokenMap.put(0x0390,"End Proc "); 
        m_tokenMap.put(0x03aa,"Global "); 
        m_tokenMap.put(0x03b6,"End "); 
        m_tokenMap.put(0x040e,"Read "); 
        m_tokenMap.put(0x0418,"Restore "); 
        m_tokenMap.put(0x0426,"Break Off "); 
        m_tokenMap.put(0x0444,"Inc "); 
        m_tokenMap.put(0x044e,"Dec "); 
        m_tokenMap.put(0x0458,"Add "); 
        m_tokenMap.put(0x0476,"Print ");
        m_tokenMap.put(0x04d0,"Input ");
        m_tokenMap.put(0x057c,"Upper$");
        m_tokenMap.put(0x0598,"Str$");
        m_tokenMap.put(0x0640,"Dim ");
        m_tokenMap.put(0x064A,"REM "); // REM
        m_tokenMap.put(0x0652,"\'"); // REM2
        m_tokenMap.put(0x0686,"Rnd");
        m_tokenMap.put(0x06d6,"Pi#");
        m_tokenMap.put(0x0702,"Sin");
        m_tokenMap.put(0x070c,"Cos");
        m_tokenMap.put(0x09ea,"Screen Open ");
        m_tokenMap.put(0x0a04,"Screen Close "); 
        m_tokenMap.put(0x0a18,"Screen Display "); 
        m_tokenMap.put(0x0b16,"View"); 
        m_tokenMap.put(0x0bb8,"Cls "); 
        m_tokenMap.put(0x0c6e,"Screen ");
        m_tokenMap.put(0x0c84,"Hires");
        m_tokenMap.put(0x0c90,"Lowres");
        m_tokenMap.put(0x0cca,"Wait Vbl");
        m_tokenMap.put(0x0d1c,"Colour "); 
        m_tokenMap.put(0x0d34,"Flash Off"); 
        m_tokenMap.put(0x0d52,"Shift Off");
        m_tokenMap.put(0x0d62,"Shift Up ");
        m_tokenMap.put(0x0d90,"Set Rainbow ");
        m_tokenMap.put(0x0dd4,"Rainbow Del "); 
        m_tokenMap.put(0x0ddc,"Rainbow "); 
        m_tokenMap.put(0x0df0,"Rain"); 
        m_tokenMap.put(0x0dfe,"Fade ");
        m_tokenMap.put(0x0ec8,"Bar "); 
        m_tokenMap.put(0x0ed8,"Box "); 
        m_tokenMap.put(0x1044,"Ink "); 
        m_tokenMap.put(0x11f8,"Joy"); 
        m_tokenMap.put(0x1aa8,"Bob Off"); 
        m_tokenMap.put(0x1202,"Jup"); 
        m_tokenMap.put(0x120c,"Jdown"); 
        m_tokenMap.put(0x1218,"Jleft"); 
        m_tokenMap.put(0x1224,"Jright"); 
        m_tokenMap.put(0x1232,"Fire"); 
        m_tokenMap.put(0x1290,"Wait Key"); 
        m_tokenMap.put(0x129e,"Wait "); 
        m_tokenMap.put(0x12ce,"Timer"); 
        m_tokenMap.put(0x12f4,"Wind Open "); 
        m_tokenMap.put(0x131a,"Wind Close"); 
        m_tokenMap.put(0x1351,"Window "); 
        m_tokenMap.put(0x135e,"Window "); 
        m_tokenMap.put(0x1378,"Locate "); 
        m_tokenMap.put(0x1392,"Home"); 
        m_tokenMap.put(0x13d2,"Pen "); 
        m_tokenMap.put(0x13dc,"Paper "); 
        m_tokenMap.put(0x13e8,"Centre "); 
        m_tokenMap.put(0x1446,"Curs Off"); 
        m_tokenMap.put(0x14b2,"Shade On"); 
        m_tokenMap.put(0x1528,"Cdown"); 
        m_tokenMap.put(0x1540,"Cright"); 
        m_tokenMap.put(0x175A,"Dir$"); 
        m_tokenMap.put(0x184e,"Load ");
        m_tokenMap.put(0x185A,"Load ");
        m_tokenMap.put(0x19b0,"Sprite Off ");
        m_tokenMap.put(0x1a26,"Spritebob Col");
        m_tokenMap.put(0x1a94,"Sprite ");
        m_tokenMap.put(0x1ab6,"Bob Off ");
        m_tokenMap.put(0x1b14,"Bobsprite Col");
        m_tokenMap.put(0x1b36,"Bob Col");
        m_tokenMap.put(0x1b46,"Bob Col");
        m_tokenMap.put(0x1b52,"Col");
        m_tokenMap.put(0x1b9e,"Bob ");
        m_tokenMap.put(0x1bae,"Get Sprite Palette"); 
        m_tokenMap.put(0x1bd0,"Get Sprite "); 
        m_tokenMap.put(0x1cfe,"Paste Bob "); 
        m_tokenMap.put(0x1d12,"Paste Icon "); 
        m_tokenMap.put(0x1d28,"Make Mask "); 
        m_tokenMap.put(0x1de0,"Hide"); 
        m_tokenMap.put(0x1f94,"Channel "); 
        m_tokenMap.put(0x1fa2,"Amreg"); 
        m_tokenMap.put(0x1fbc,"Amal On "); 
        m_tokenMap.put(0x1fca,"Amal On "); 
        m_tokenMap.put(0x1fd2,"Amal Off "); 
        m_tokenMap.put(0x1fe2,"Amal Off "); 
        m_tokenMap.put(0x1fea,"Amal Freeze "); 
        m_tokenMap.put(0x1ffc,"Amal Freeze "); 
        m_tokenMap.put(0x2012,"Amal "); 
        m_tokenMap.put(0x2bae,"Get Bob Palette"); 
        m_tokenMap.put(0xff4c," or "); 
        m_tokenMap.put(0xff58," and ");
        m_tokenMap.put(0xff66,"<>");
        m_tokenMap.put(0xff7a,"<=");
        m_tokenMap.put(0xff8e,">=");
        m_tokenMap.put(0xff98,"=>");
        m_tokenMap.put(0xffa2,"=");
        m_tokenMap.put(0xffac,"<");
        m_tokenMap.put(0xffb6,">");
        m_tokenMap.put(0xffc0,"+");
        m_tokenMap.put(0xffca,"-");
        m_tokenMap.put(0xffe2,"*");
        m_tokenMap.put(0xffec,"/");
        m_tokenMap.put(0xfff6,"^");        
        // extensions [ext_number,offset]
        _initMusicTokenMap();
        _initCraftTokenMap();
        m_extensions.put(0x020056, "Unpack ");
    }
    
    /**
     * Extension 1: Music
     */
    private void _initMusicTokenMap()
    {
        m_extensions.put(0x01002c, "Music Off");
        m_extensions.put(0x010058, "Music ");
        m_extensions.put(0x010074, "Boom");
        m_extensions.put(0x0100f8, "Sam Play ");
        m_extensions.put(0x010144, "Play ");    
        m_extensions.put(0x010196, "Mvolume ");
   }
    /**
     * Extensions 18, 19: Craft, MUSICraft
     */
    private void _initCraftTokenMap()
    {
        m_extensions.put(0x130028, "St Play ");   
        m_extensions.put(0x130030, "St Stop"); 
    }
    
}