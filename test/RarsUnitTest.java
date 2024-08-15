import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rars.AssemblyException;
import rars.ErrorMessage;
import rars.SimulationException;
import rars.api.Program;
import rars.simulator.ProgramArgumentList;
import rars.simulator.Simulator;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class RarsUnitTest extends RarsTest {
    @ParameterizedTest
    @MethodSource({"agnosticTestFiles", "rv32TestFiles"})
    public void testRunRV32(String path) throws IOException {
        Program p = setupProgram(false);
        testFile(path, p);
    }

    @ParameterizedTest
    @MethodSource({"agnosticTestFiles", "rv64TestFiles"})
    public void testRunRV64(String path) throws IOException {
        Program p = setupProgram(true);
        testFile(path, p);
    }

    static Stream<String> agnosticTestFiles() {
        File[] tests = new File("./test").listFiles();
        assumeTrue(tests != null && Arrays.stream(tests).anyMatch(t -> t.isFile() && t.getName().endsWith(".s")));
        return Arrays.stream(tests).filter(t -> t.isFile() && t.getName().endsWith(".s")).map(File::getPath);
    }
    static Stream<String> rv32TestFiles() {
        File[] tests = new File("./test/riscv-tests-32").listFiles();
        assumeTrue(tests != null && Arrays.stream(tests).anyMatch(t -> t.isFile() && t.getName().endsWith(".s")));
        return Arrays.stream(tests).filter(t -> t.isFile() && t.getName().endsWith(".s")).map(File::getPath);
    }
    static Stream<String> rv64TestFiles() {
        File[] tests = new File("./test/riscv-tests-64").listFiles();
        assumeTrue(tests != null && Arrays.stream(tests).anyMatch(t -> t.isFile() && t.getName().endsWith(".s")));
        return Arrays.stream(tests).filter(t -> t.isFile() && t.getName().endsWith(".s")).map(File::getPath);
    }

    public void testFile(String path, Program p) throws IOException {
        int[] errorlines = null;
        String stdin = "", stdout = "", stderr ="", errorMessage = "", exitReason = "";
        ArrayList<String> programArgumentList = null;
        ArrayList<String> fileList = new ArrayList<>();
        fileList.add(path);
        int exitCode = 0;
        p.getOptions().selfModifyingCode = false;
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = br.readLine();
            while(line != null){
                if (line.startsWith("#error on lines:")) {
                    String[] linenumbers = line.replaceFirst("#error on lines:", "").split(",");
                    errorlines = new int[linenumbers.length];
                    for(int i = 0; i < linenumbers.length; i++){
                        errorlines[i] = Integer.parseInt(linenumbers[i].trim());
                    }
                } else if (line.startsWith("#lib:")) {
                    String lib = line.replaceFirst("#lib:", "");
                    fileList.add(Paths.get(path).getParent().resolve(lib).toString());
                } else if (line.startsWith("#stdin:")) {
                    stdin = line.replaceFirst("#stdin:", "").replaceAll("\\\\n","\n");
                } else if (line.startsWith("#stdout:")) {
                    stdout = line.replaceFirst("#stdout:", "").replaceAll("\\\\n","\n").trim();
                } else if (line.startsWith("#stderr:")) {
                    stderr = line.replaceFirst("#stderr:", "").replaceAll("\\\\n","\n").trim();
                } else if (line.startsWith("#exit:")) {
                    exitReason = line.replaceFirst("#exit:", "");
                    try {
                        exitCode = Integer.parseInt(exitReason);
                    } catch (NumberFormatException nfe) {
                        exitCode = -1;
                    }
                } else if (line.startsWith("#args:")) {
                    String args = line.replaceFirst("#args:", "");
                    programArgumentList = new ProgramArgumentList(args).getProgramArgumentList();
                } else if (line.startsWith("#error:")) {
                    errorMessage = line.replaceFirst("#error:", "");
                } else if (line.startsWith("#selfmod:")) {
                    String selfmod = line.replaceFirst("#selfmod:", "");
                    if (selfmod.equals("true")) {
                        p.getOptions().selfModifyingCode = true;
                    }
                }
                line = br.readLine();
            }

        try {
            p.assemble(fileList, path);
            assertNull(errorlines,"Expected assembly error, but successfully assembled " + path);

            p.setup(programArgumentList, stdin);
            Simulator.Reason r = p.simulate();
            if(r != Simulator.Reason.NORMAL_TERMINATION){
                assertEquals(r.toString().toLowerCase(),exitReason,"Ended abnormally " + r + " while executing " + path);
            }else{
                assertEquals(p.getExitCode(), exitCode, "Final exit code was wrong for " + path);
                assertEquals(p.getSTDOUT().trim(),stdout, "STDOUT was wrong for " + path);
                assertEquals(p.getSTDERR().trim(), stderr, "STDERR was wrong for " + path);
            }
        } catch (AssemblyException ae){
            assertNotNull(errorlines, "Failed to assemble " + path);
            assertEquals(ae.errors().errorCount(), errorlines.length, "Mismatched number of assembly errors in" + path);

            Iterator<ErrorMessage> errors = ae.errors().getErrorMessages().iterator();
            for(int number : errorlines){
                ErrorMessage error = errors.next();
                while(error.isWarning()) error = errors.next();
                assertEquals(error.getLine(), number, "Expected error on line " + number + ". Found error on line " + error.getLine()+" in " + path);
            }
        } catch (SimulationException se){
            assertEquals(se.error().getMessage(), errorMessage, "Crashed while executing " + path + "; " + se.error().generateReport());
        }
    }
    @Override
    @Test
    public void checkBinary() {
        super.checkBinary();
    }

    @Override
    @Test
    public void checkPseudo() {
        super.checkPseudo();
    }

}
