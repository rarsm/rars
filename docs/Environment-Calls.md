RARS currently supports system calls that MARS originally supported and system calls compatible with Linux tooling (gcc, [riscv-pk](https://github.com/riscv/riscv-pk), etc.). Only a handful are compatible with other education simulators, but it is configurable, so the call numbers can be changed with a simple config file.

They can be called by loading the call number into `a7`, any other arguments into `a0`-`a6` and calling `ecall`. Currently, when in 64 bit mode, only the lower 32 bits will be considered and outputs will be sign extended from 32 to 64 bits. The following exits the program with the code 42.

```assembly
li a0, 42
li a7, 93
ecall 
```

Note: all registers besides the output are guaranteed not to change.

All supported system calls are shown below.

| Name | Call Number (a7) | Description | Inputs | Outputs |
|------|------------------|-------------|--------|---------|
|PrintInt|1|Prints an integer|a0 = integer to print|N/A|
|PrintFloat|2|Prints a floating point number|fa0 = float to print|N/A|
|PrintDouble|3|Prints a double precision floating point number|fa0 = double to print|N/A|
|PrintString|4|Prints a null-terminated string to the console|a0 = the address of the string|N/A|
|ReadInt|5|Reads an int from input console|N/A|a0 = the int|
|ReadFloat|6|Reads a float from input console|N/A|fa0 = the float|
|ReadDouble|7|Reads a double from input console|N/A|fa0 = the double|
|ReadString|8|Reads a string from the console|a0 = address of input buffer<br>a1 = maximum number of characters to read|N/A|
|Sbrk|9|Allocate heap memory|a0 = amount of memory in bytes|a0 = address to the allocated block|
|Exit|10|Exits the program with code 0|N/A|N/A|
|PrintChar|11|Prints an ascii character|a0 = character to print (only lowest byte is considered)|N/A|
|ReadChar|12|Reads a character from input console|N/A|a0 = the character|
|GetCWD|17|Writes the path of the current working directory into a buffer|a0 = the buffer to write into <br>a1 = the length of the buffer|a0 = -1 if the path is longer than the buffer|
|Time|30|Get the current time (milliseconds since 1 January 1970)|N/A|a0 = low order 32 bits<br>a1=high order 32 bits|
|MidiOut|31|Outputs simulated MIDI tone to sound card (does not wait for sound to end).|See MIDI note below|N/A|
|Sleep|32|Set the current thread to sleep for a time (not precise)|a0 = time to sleep in milliseconds|N/A|
|MidiOutSync|33|Outputs simulated MIDI tone to sound card, then waits until the sound finishes playing.|See MIDI note below|N/A|
|PrintIntHex|34|Prints an integer (in hexdecimal format left-padded with zeroes)|a0 = integer to print|N/A|
|PrintIntBinary|35|Prints an integer (in binary format left-padded with zeroes) |a0 = integer to print|N/A|
|PrintIntUnsigned|36|Prints an integer (unsigned)|a0 = integer to print|N/A|
|RandSeed|40|Set seed for the underlying Java pseudorandom number generator|a0 = index of pseudorandom number generator<br>a1 = the seed|N/A|
|RandInt|41|Get a random integer|a0 = index of pseudorandom number generator|a0 = random integer|
|RandIntRange|42|Get a random bounded integer|a0 = index of pseudorandom number generator<br>a1 = upper bound for random number|a0 = uniformly selectect from [0,bound]|
|RandFloat|43|Get a random float|a0 = index of pseudorandom number generator|fa0 = uniformly randomly selected from from [0,1]|
|RandDouble|44|Get a random double from the range 0.0-1.0|a0 = index of pseudorandom number generator|fa0 = the next pseudorandom|
|ConfirmDialog|50|Service to display a message to user|a0 = address of null-terminated string that is the message to user|a0 = Yes (0), No (1), or Cancel(2)|
|InputDialogInt|51|N/A|N/A|N/A|
|InputDialogFloat|52|N/A|N/A|N/A|
|InputDialogDouble|53|N/A|N/A|N/A|
|InputDialogString|54|Service to display a message to a user and request a string input|a0 = address of null-terminated string that is the message to user<br>a1 = address of input buffer<br>a2 = maximum number of characters to read (including the terminating null)|a1 contains status value.<br> 0: OK status. Buffer contains the input string.<br>-2: Cancel was chosen. No change to buffer.<br>-3: OK was chosen but no data had been input into field. No change to buffer.<br>-4: length of the input string exceeded the specified maximum. Buffer contains the maximum allowable input string terminated with null.|
|MessageDialog|55|Service to display a message to user|a0 = address of null-terminated string that is the message to user <br>a1 = the type of the message to the user, which is one of:<br>0: error message <br>1: information message <br>2: warning message <br>3: question message <br>other: plain message|N/A|
|MessageDialogInt|56|Service to display a message followed by a int to user|a0 = address of null-terminated string that is the message to user <br>a1 = the int to display|N/A|
|Close|57|Close a file|a0 = the file descriptor to close|N/A|
|MessageDialogDouble|58|Service to display message followed by a double|a0 = address of null-terminated string that is the message to user <br> fa0 = the double|N/A|
|MessageDialogString|59|Service to display a message followed by a string to user|a0 = address of null-terminated string that is the message to user <br>a1 = address of the second string to display|N/A|
|MessageDialogFloat|60|Service to display a message followed by a float to user|a0 = address of null-terminated string that is the message to user <br>fa1 = the float to display|N/A|
|LSeek|62|Seek to a position in a file|a0 = the file descriptor <br> a1 = the offset for the base <br>a2 is the begining of the file (0), the current position (1), or the end of the file (2)}|a0 = the selected position from the beginning of the file or -1 is an error occurred|
|Read|63|Read from a file descriptor into a buffer|a0 = the file descriptor <br>a1 = address of the buffer <br>a2 = maximum length to read|a0 = the length read or -1 if error|
|Write|64|Write to a filedescriptor from a buffer|a0 = the file descriptor<br>a1 = the buffer address<br>a2 = the length to write|a0 = the number of characters written|
|Exit2|93|Exits the program with a code|a0 = the number to exit with|N/A|
|Open|1024|Opens a file from a path <br>Only supported flags (a1) are read-only (0), write-only (1) and write-append (9). write-only flag creates file if it does not exist, so it is technically write-create.  write-append will start writing at end of existing file.|a0 = Null terminated string for the path <br>a1 = flags|a0 = the file descriptor or -1 if an error occurred|

### Using MIDI output
These system services comes from MARS, and provide a means of producing sound.  MIDI output is
simulated by your system sound card, and the simulation is provided by the <tt>javax.sound.midi</tt>
package.

This service requires four parameters as follows:

  - **pitch (a0)**
    - Accepts a positive byte value (0-127) that denotes a pitch as it would
be represented in MIDI
    - Each number is one semitone / half-step in the chromatic scale.
    - 0 represents a very low C and 127 represents a very high G (a standard
88 key piano begins at 9-A and ends at 108-C).
    - If the parameter value is outside this range, it applies a default value 60 which is the same as middle C on a piano.
    - From middle C, all other pitches in the octave are as follows:<table width="450" border="0" align="center" cellpadding="2"><tr><td><li>61 = C# or Db</li><li>62 = D</li><li>63 = D# or Eb</li><li>64 = E or Fb</li></td><td><li>65 = E# or F</li><li>66 = F# or Gb</li><li>67 = G</li><li>68 = G# or Ab</li></td><td><li>69 = A</li><li>70 = A# or Bb</li><li>71 = B or Cb</li><li>72 = B# or C</li></td></tr></table>
    - To produce these pitches in other octaves, add or subtract multiples of 12.
  - **duration in milliseconds (a1)**
    - Accepts a positive integer value that is the length of the tone in milliseconds.
    - If the parameter value is negative, it applies a default value of one second (1000 milliseconds).
  - **instrument (a2)**
    - Accepts a positive byte value (0-127) that denotes the General MIDI
&quot;patch&quot; used to play the tone.
    - If the parameter is outside this range, it applies a default value 0 which is an <em>Acoustic Grand Piano</em>.
    - General MIDI standardizes the number associated with each possible instrument (often referred to as <em>program change</em> numbers), however it does not determine how the tone will sound. This is determined by the synthesizer that is producing the sound. Thus a<em> Tuba</em> (patch 58) on one computer
may sound different than that same patch on another computer.
    - The 128 available patches are divided into instrument families of 8:<table width="450" border="0" align="center" cellpadding="2"><tr><td width="60">0-7</td><td width="160">Piano</td><td width="60">64-71</td><td>Reed</td></tr><tr><td width="60">8-15</td><td width="160">Chromatic Percussion</td><td width="60">72-79</td><td>Pipe</td></tr><tr><td width="60">16-23</td><td width="160">Organ</td><td width="60">80-87</td><td>Synth Lead</td></tr><tr><td width="60">24-31</td><td width="160">Guitar</td><td width="60">88-95</td><td>Synth Pad</td></tr><tr><td width="60">32-39</td><td width="160">Bass</td><td width="60">96-103</td><td>Synth Effects</td></tr><tr><td width="60">40-47</td><td width="160">Strings</td><td width="60">104-111</td><td>Ethnic</td></tr><tr><td width="60">48-55</td><td width="160">Ensemble</td><td width="60">112-119</td><td>Percussion</td></tr><tr><td width="60">56-63</td><td width="160">Brass</td><td width="60">120-127</td><td>Sound Effects</td></tr></table>
    - Note that outside of Java, General MIDI usually refers to patches 1-128. When referring to a list of General MIDI patches, 1 must be subtracted to play the correct patch. For a full list of General MIDI instruments, see <a href="http://www.midi.org/">www.midi.org/about-midi/gm/gm1sound.shtml</a>. The General MIDI channel 10 percussion key map is not relevant to the toneGenerator method because it always defaults to MIDI channel 1.
  - **volume (a3)**
    - Accepts a positive byte value (0-127) where 127 is the loudest and 0
is silent. This value denotes MIDI velocity which refers to the initial
attack of the tone.
    - If the parameter value is outside this range, it applies a default value 100.
    - MIDI velocity measures how hard a <em>note on</em> (or <em>note off</em>) message is played, perhaps on a MIDI controller like a keyboard. Most MIDI synthesizers will translate this into volume on a logarithmic scale in which the difference in amplitude decreases as the velocity value increases.
    - Note that velocity value on more sophisticated synthesizers can also
affect the timbre of the tone (as most instruments sound different when
they are played louder or softer).

MIDI Output was developed and documented by Otterbein student Tony Brock in July 2007.


