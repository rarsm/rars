package rars.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Basic indenter for RISC-V assembly code.
 * Aligns labels and instructions using relative tab-based indentation.
 * Formatting rules:
 * - Instructions have n+1 tabs where n is the label's tab level
 * - If label has 0 tabs, instructions get 1 tab
 * - If label has 1 tab, instructions get 2 tabs, etc.
 * - Tab size is 8 spaces (multiple of 8)
 * - Comments align at column 40
 */
public class Indenter {
    // Tab configuration
    private static final int TAB_SIZE = 8;  // Tab size in spaces (multiple of 8)
    private static final int COMMENT_COLUMN = 40;  // Column where comments should align

    /**
     * Indents a list of assembly code lines according to RISC-V conventions.
     * @param lines List of raw assembly code lines
     * @return List of properly indented lines
     */
    public List<String> indent(List<String> lines) {
        List<String> indentedLines = new ArrayList<>();
        int currentLabelTabLevel = 0; // Track the tab level of the most recent label
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Handle empty lines and comments/directives
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("//")) {
                indentedLines.add(line);
                continue;
            }

            // Handle directives (align like instructions, except section directives)
            if (trimmedLine.startsWith(".")) {
                // Check if it's a section directive that should always be at the start
                boolean isSectionDirective = trimmedLine.startsWith(".data") || 
                                           trimmedLine.startsWith(".text") || 
                                           trimmedLine.startsWith(".bss") || 
                                           trimmedLine.startsWith(".rodata") ||
                                           trimmedLine.startsWith(".section");
                
                // Find the first # that's not inside quotes (same logic as instructions)
                String codePart;
                String commentPart = "";
                
                int commentIndex = -1;
                boolean inQuotes = false;
                char quoteChar = 0;
                
                for (int i = 0; i < line.length(); i++) {
                    char c = line.charAt(i);
                    
                    if (!inQuotes && (c == '"' || c == '\'')) {
                        inQuotes = true;
                        quoteChar = c;
                    } else if (inQuotes && c == quoteChar) {
                        // Check if it's escaped
                        if (i == 0 || line.charAt(i - 1) != '\\') {
                            inQuotes = false;
                        }
                    } else if (!inQuotes && c == '#') {
                        commentIndex = i;
                        break;
                    }
                }
                
                if (commentIndex != -1) {
                    codePart = line.substring(0, commentIndex).trim();
                    commentPart = line.substring(commentIndex);
                } else {
                    codePart = line.trim();
                }
                
                StringBuilder formattedLine = new StringBuilder();
                
                if (isSectionDirective) {
                    // Section directives always start at column 0
                    formattedLine.append(codePart);
                } else {
                    // Other directives use currentLabelTabLevel + 1 for directives under a label
                    formattedLine.append("\t".repeat(currentLabelTabLevel + 1));
                    formattedLine.append(codePart);
                }
                
                // Add comment if present (aligned consistently)
                if (!commentPart.isEmpty()) {
                    alignComment(formattedLine, commentPart);
                }
                
                indentedLines.add(formattedLine.toString());
                continue;
            }

            // Split line into components (handle # in string literals)
            String codePart;
            String commentPart = "";
            
            // Find the first # that's not inside quotes
            int commentIndex = -1;
            boolean inQuotes = false;
            char quoteChar = 0;
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                
                if (!inQuotes && (c == '"' || c == '\'')) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (inQuotes && c == quoteChar) {
                    // Check if it's escaped
                    if (i == 0 || line.charAt(i - 1) != '\\') {
                        inQuotes = false;
                    }
                } else if (!inQuotes && c == '#') {
                    commentIndex = i;
                    break;
                }
            }
            
            if (commentIndex != -1) {
                codePart = line.substring(0, commentIndex).trim();
                commentPart = line.substring(commentIndex); // Preserve original comment spacing
            } else {
                codePart = line.trim();
            }

            // If it's just a comment line, keep it as is
            if (codePart.isEmpty() && !commentPart.isEmpty()) {
                indentedLines.add(line);
                continue;
            }

            // Process code part
            String[] tokens = codePart.split("\\s+");
            boolean hasLabel = tokens.length > 0 && tokens[0].endsWith(":");
            String label = hasLabel ? tokens[0] : "";
            String instruction = hasLabel ? (tokens.length > 1 ? tokens[1] : "") : (tokens.length > 0 ? tokens[0] : "");
            String operands = hasLabel 
                ? (tokens.length > 2 ? String.join(" ", Arrays.copyOfRange(tokens, 2, tokens.length)) : "")
                : (tokens.length > 1 ? String.join(" ", Arrays.copyOfRange(tokens, 1, tokens.length)) : "");

            // Build indented line
            StringBuilder formattedLine = new StringBuilder();
            
            // Handle label
            if (!label.isEmpty()) {
                // Determine label indentation level from the original line
                int labelTabLevel = 0;
                for (int i = 0; i < line.length(); i++) {
                    if (line.charAt(i) == '\t') {
                        labelTabLevel++;
                    } else if (line.charAt(i) == ' ') {
                        // Count spaces as tabs (8 spaces = 1 tab)
                        int spaces = 0;
                        while (i < line.length() && line.charAt(i) == ' ') {
                            spaces++;
                            i++;
                        }
                        labelTabLevel += spaces / TAB_SIZE;
                        break;
                    } else {
                        break;
                    }
                }
                
                // Update the current label tab level for subsequent instructions
                currentLabelTabLevel = labelTabLevel;
                
                // Add label indentation
                formattedLine.append("\t".repeat(labelTabLevel));
                formattedLine.append(label);
                
                // If there's an instruction on the same line, it gets labelTabLevel + 1 tabs
                if (!instruction.isEmpty()) {
                    formattedLine.append("\n");
                    formattedLine.append("\t".repeat(labelTabLevel + 1));
                }
            } else {
                // No label, this is just an instruction
                // Use currentLabelTabLevel + 1 for instructions under a label
                if (!instruction.isEmpty()) {
                    formattedLine.append("\t".repeat(currentLabelTabLevel + 1));
                }
            }
            
            // Handle instruction and operands
            if (!instruction.isEmpty()) {
                formattedLine.append(instruction);
                if (!operands.isEmpty()) {
                    formattedLine.append(" ").append(operands);
                }
            }
            
            // Add comment (aligned consistently)
            if (!commentPart.isEmpty()) {
                alignComment(formattedLine, commentPart);
            }

            indentedLines.add(formattedLine.toString());
        }
        return indentedLines;
    }

    /**
     * Aligns a comment at the specified column position.
     * @param line The StringBuilder containing the current line
     * @param comment The comment text to append (including the # symbol)
     */
    private void alignComment(StringBuilder line, String comment) {
        int currentLength = line.length();
        
        if (currentLength >= COMMENT_COLUMN) {
            // If line is already at or past comment column, add just one space
            line.append(" ");
        } else {
            // Add spaces to reach the comment column
            int spacesToAdd = COMMENT_COLUMN - currentLength;
            line.append(" ".repeat(spacesToAdd));
        }
        
        line.append(comment);
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
