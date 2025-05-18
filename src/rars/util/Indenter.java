package rars.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Basic indenter for RISC-V assembly code.
 * Aligns labels, instructions/directives, and comments in fixed columns.
 * Formatting rules:
 * - Labels start at column 0
 * - Instructions start at column 20
 * - Comments start at column 40
 */
public class Indenter {
    // Column positions for alignment
    private static final int LABEL_COLUMN = 0;         // Column for labels
    private static final int INSTRUCTION_COLUMN = 20;  // Column for instructions
    private static final int COMMENT_COLUMN = 40;      // Column for comments

    /**
     * Indents a list of assembly code lines according to RISC-V conventions.
     * @param lines List of raw assembly code lines
     * @return List of properly indented lines
     */
    public List<String> indent(List<String> lines) {
        List<String> indentedLines = new ArrayList<>();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Handle empty lines and comments/directives
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                indentedLines.add(line);
                continue;
            }

            // Handle directives
            if (trimmedLine.startsWith(".")) {
                StringBuilder formattedLine = new StringBuilder();
                formattedLine.append(trimmedLine);
                indentedLines.add(formattedLine.toString());
                continue;
            }

            // Split line into components
            String[] parts = line.split("#", 2);
            String codePart = parts[0].trim();
            String commentPart = parts.length > 1 ? "#" + parts[1].trim() : "";

            // Process code part
            String[] tokens = codePart.split("\\s+");
            boolean hasLabel = tokens.length > 0 && tokens[0].endsWith(":");
            String label = hasLabel ? tokens[0] : "";
            String instruction = hasLabel ? (tokens.length > 1 ? tokens[1] : "") : (tokens.length > 0 ? tokens[0] : "");
            String operands = hasLabel 
                ? String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length))
                : String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length));

            // Build indented line
            StringBuilder formattedLine = new StringBuilder();
            
            // Handle label
            if (!label.isEmpty()) {
                formattedLine.append(label);
                int spacesNeeded = INSTRUCTION_COLUMN - label.length() - 1;
                if (spacesNeeded > 0) {
                    formattedLine.append(" ".repeat(spacesNeeded));
                }
            } else {
                // Only add spaces if there's an instruction to align
                if (!instruction.isEmpty()) {
                    formattedLine.append(" ".repeat(INSTRUCTION_COLUMN - 1));
                }
            }
            
            // Handle instruction and operands
            if (!instruction.isEmpty()) {
                formattedLine.append(instruction);
                if (!operands.isEmpty()) {
                    formattedLine.append(" ").append(operands);
                }
            }
            
            // Add comment
            if (!commentPart.isEmpty()) {
                int currentLength = formattedLine.length();
                if (currentLength < COMMENT_COLUMN) {
                    formattedLine.append(" ".repeat(COMMENT_COLUMN - currentLength));
                } else {
                    formattedLine.append(" ");
                }
                formattedLine.append(commentPart);
            }

            indentedLines.add(formattedLine.toString());
        }
        return indentedLines;
    }

    /**
     * Convenience method to indent a complete assembly program string.
     * @param assemblyCode The raw assembly code as a single string with newlines
     * @return Formatted assembly code with consistent indentation
     */
    public static String indentAssembly(String assemblyCode) {
        return String.join("\n", new Indenter().indent(Arrays.asList(assemblyCode.split("\n"))));
    }
}
