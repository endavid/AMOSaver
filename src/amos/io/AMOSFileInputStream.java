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
    byte[]  m_tmp4B = {0,0,0,0};
    byte[]  m_tmp2B = {0,0};
    byte[]  m_tmp1B = {0};
    Map<Integer,String> m_tokenMap;
    Map<Integer,String> m_extensions;
    
    public boolean isSanityTested()
    {
        return m_isSanityTested ;
    }
    
    public AMOSFileInputStream(java.io.File file) 
        throws java.io.FileNotFoundException, amos.io.UnsupportedFormat, java.io.IOException
    {
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
        if (text.equals("AmBk")) {
            return AMOSBankType.MEMORYBANK;
        } else if (text.equals("AmSp")) {
            return AMOSBankType.SPRITEBANK;
        } else if (text.equals("AmIc")) {
            return AMOSBankType.ICONBANK;
        }
        throw( new java.io.StreamCorruptedException("Unknown memory bank!") ); 
    }
    
    /**
     * Reads Sprites or Icons
     */
    public List<BufferedImage> readImages() throws java.io.IOException, java.io.StreamCorruptedException
    {
        m_stream.read(m_tmp2B);
        int numImages = readUnsignedWord(m_tmp2B);
        List<BufferedImage> imgList = new ArrayList<BufferedImage>(numImages);
        System.out.println("Num images: "+numImages);
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
            System.out.println("img("+i+")="+(16*width)+"x"+height+"x"+depth+", ("+hotspotX+", "+hotspotY+")");
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
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
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
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
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
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
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
                    m_stream.read(m_tmp1B); // string size 
                    int strlength = readUnsignedByte(m_tmp1B);
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
                    m_stream.read(m_tmp2B); // length of the string 
                    int strlength = readUnsignedWord(m_tmp2B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + "\""+(new String(str))+"\"" ;
                    break;
                }
                case 0x002E: // String with single quotes
                {
                    m_stream.read(m_tmp2B); // length of the string 
                    int strlength = readUnsignedWord(m_tmp2B);
                    if ( (strlength%2)==1 ) strlength += 1; // round up to words
                    byte[] str = new byte[strlength];
                    m_stream.read(str);
                    readWords += 1 + (strlength>>1);
                    line = line + "\'"+(new String(str))+"\'" ;
                    break;
                }
                case 0x001E: // Binary integer value
                {
                    m_stream.read(m_tmp4B); // integer 
                    int value = readSignedInt(m_tmp4B);
                    readWords += 2;
                    line = line + "%"+Integer.toBinaryString(value) ;
                    break;
                }
                case 0x0036: // Hexadecimal integer value
                {
                    m_stream.read(m_tmp4B); // integer 
                    int value = readSignedInt(m_tmp4B);
                    readWords += 2;
                    line = line + "$"+Integer.toHexString(value) ;
                    break;
                }
                case 0x003E: // Decimal integer value
                {
                    m_stream.read(m_tmp4B); // integer 
                    int value = readSignedInt(m_tmp4B);
                    readWords += 2;
                    line = line +Integer.toString(value) ;
                    break;
                }
                case 0x0046: // Float value
                {
                    m_stream.read(m_tmp4B); // float 
                    float value = readFloat(m_tmp4B);
                    readWords += 2;
                    line = line + Float.toString(value) ;
                    break;
                }
                case 0x004E: // Extension command
                {
                    m_stream.read(m_tmp1B); // integer 
                    int extNumber = readUnsignedByte(m_tmp1B);
                    m_stream.read(m_tmp1B); // unused
                    m_stream.read(m_tmp2B); // signed 16-bit offset into extension's token table
                    int offset = readSignedWord(m_tmp2B);
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
                    line = line + "Else ";
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

    public static float readFloat(byte[] bigEndian)
    {
        int value = ((0x000000ff & (int)bigEndian[0]) << 24) |
            ((0x000000ff & (int)bigEndian[1]) << 16) |
            ((0x000000ff & (int)bigEndian[2]) <<  8) |
            ((0x000000ff & (int)bigEndian[3]) ) ;
        return Float.intBitsToFloat(value) ;
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
        m_tokenMap.put(0x0074,"("); 
        m_tokenMap.put(0x007c,")"); 
        m_tokenMap.put(0x0094," To ");  
        m_tokenMap.put(0x00f2,"Inkey$");  
        m_tokenMap.put(0x0246,"Next "); 
        m_tokenMap.put(0x025c,"Until "); 
        m_tokenMap.put(0x02c6," Then "); 
        m_tokenMap.put(0x02da,"End If"); 
        m_tokenMap.put(0x0390,"End Proc "); 
        m_tokenMap.put(0x03aa,"Global "); 
        m_tokenMap.put(0x0426,"Break Off "); 
        m_tokenMap.put(0x044e,"Dec "); 
        m_tokenMap.put(0x0476,"Print "); 
        m_tokenMap.put(0x064A,"REM "); // REM
        m_tokenMap.put(0x0652,"\'"); // REM2
        m_tokenMap.put(0x0686,"Rnd");
        m_tokenMap.put(0x0bb8,"Cls "); 
        m_tokenMap.put(0x0d1c,"Colour "); 
        m_tokenMap.put(0x0d34,"Flash Off "); 
        m_tokenMap.put(0x0dfe,"Fade "); 
        m_tokenMap.put(0x1232,"Fire"); 
        m_tokenMap.put(0x129e,"Wait "); 
        m_tokenMap.put(0x13e8,"Centre "); 
        m_tokenMap.put(0x1378,"Locate "); 
        m_tokenMap.put(0x1392,"Home"); 
        m_tokenMap.put(0x1446,"Curs Off "); 
        m_tokenMap.put(0x175A,"Dir$"); 
        m_tokenMap.put(0x185A,"Load ");
        m_tokenMap.put(0x1a26,"Spritebob Col");
        m_tokenMap.put(0x1a94,"Sprite ");
        m_tokenMap.put(0x1b9e,"Bob ");
        m_tokenMap.put(0x1bae,"Get Sprite Palette"); 
        m_tokenMap.put(0x1d28,"Make Mask "); 
        m_tokenMap.put(0x1de0,"Hide"); 
        m_tokenMap.put(0x1f94,"Channel "); 
        m_tokenMap.put(0x1fa2,"Amreg"); 
        m_tokenMap.put(0x1fbc,"Amal On "); 
        m_tokenMap.put(0x1fca,"Amal On "); 
        m_tokenMap.put(0x1ffc,"Amal Freeze "); 
        m_tokenMap.put(0x2012,"Amal "); 
        m_tokenMap.put(0xff4c," or "); 
        m_tokenMap.put(0xff58," and ");
        m_tokenMap.put(0xffa2,"=");
        m_tokenMap.put(0xffac,"<");
        m_tokenMap.put(0xffb6,">");
        m_tokenMap.put(0xffc0,"+");
        m_tokenMap.put(0xffca,"-");
        // extensions [ext_number,offset]
        m_extensions.put(0x010074, "Boom");        
    }
    
}