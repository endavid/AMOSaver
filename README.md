AMOSaver
========

A tool to decode AMOS program files.

Features
---------

### Parse source code ###

* It parses an AMOS file and outputs the source code in plain text.
* Currently, not every token is decoded.
 * Basically, I reverse-engineered a couple of my old programs, so I may have missed some tokens.
 * There are too many extensions to add support for every of them, so again, I just decoded some of the tokens I used in my old programs. 
 * List of AMOS extensions: http://www.amigacoding.com/index.php/AMOS:Extensions
 * From that list, I just decode a couple of commands from (1)Music, (2)Compactor, (19)MusiCRAFT 

### Decode sprites and icons ###

* The program also decodes the Sprite and Icon memory banks and converts them to PNG images.

### Decode other memory banks ###

* So far, the program just gives brief information about the other memory banks, but it does not decode them.

Build
------
Use <code>ant</code> to build the project.

* Go to the project folder.
* Type <code>ant</code>, and it will create a <code>.jar</code> file inside <code>dist</code> folder.
* Alternatively, if you have a Mac, open the XCode project file and build using XCode.

Usage
------

* Run it from the command line using Java VM.

<pre><code>java -jar AMOSFileDecoder [options] SOURCEFILE.AMOS
</code></pre>

* Options:
 * <code>-v | --verbose:</code> outputs more information
 * <code>--sourceonly:</code> decode only the source code
 * <code>--imagefolder PATH:</code> output images to PATH
 * <code>--datafolder PATH:</code> output memory banks to PATH

* Examples:
 * This example will decode the input file and output as a plain AMOS file (I call this file "pamos").
<pre><code>java -jar AMOSFileDecoder --sourceonly MYPROGRAM.AMOS &gt;myprogram.pamos
</code></pre>
 * This example will create image files inside the specified folder, output the source code to the pamos file, and output information to the console such as the size of each sprite or the memory banks that the AMOS file contains.
<pre><code>java -jar AMOSFileDecoder -v --imagefolder ~/Pictures/ MYPROGRAM.AMOS &gt;myprogram.pamos
**[OUTPUT Example]**
_Decoding 6 banks..._
 Bank 9: Pac.Pic. 27826 bytes
 Bank 0: Samples  (chip) 30088 bytes
 Bank 7: Pac.Pic. 5246 bytes
 Bank 3: Music    (chip) 45124 bytes
 Bank 5: Samples  (chip) 60900 bytes
_... reading 8 images_
img(0)=16x10x5, (0, 0)
img(1)=16x10x5, (0, 0)
...
</code></pre>

Games in AMOS
--------------
* You can find the AMOS source file of some of the old games I made here: http://endavid.com/lists/works.html
* I also uploaded the decoded source code into this repository: https://github.com/endavid/HawkFB_AMOS_retrogames 
About the name: AMOSaver
------------------------
* AMOSaver is a lame name that comes from 3 lame ideas:
 * _AMOS Saver_: it saves AMOS files, and it rescues my old programs into something readable
 * _AMOS, a version_: a version of AMOS
 * _vAMOS a ver_: in Spanish, "let's see"... I was not sure what this project would turn into, so for now, let's just see...

To do
-----
* These are some of the things I plan to add:
 * Parse more source tokens
 * Decode Pac.Pic. banks
 * Decode Samples
 * Decode Music
 * Provide a syntax highlighter in PHP
* These are things I have **NO PLAN** of implementing:
 * An interpreter to run the program
 * Extensions of AMOS language

