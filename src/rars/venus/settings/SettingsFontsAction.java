package rars.venus.settings;

import rars.Globals;
import rars.Settings;
import rars.venus.ExecutePane;
import rars.venus.GuiAction;
import rars.venus.MonoRightCellRenderer;
import rars.venus.util.AbstractFontSettingDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Action class for the Settings menu item for optionally loading a MIPS exception handler.
 */
public class SettingsFontsAction extends GuiAction {
    JDialog fontDialog;

    private static final int[] rowsFontSettingPositions = {
            Settings.TEXTSEGMENT_HIGHLIGHT_FONT,
            Settings.TEXTSEGMENT_DELAYSLOT_HIGHLIGHT_FONT,
            Settings.EXPLICIT_WRITE_HIGHLIGHT_FONT,
            Settings.EXPLICIT_READ_HIGHLIGHT_FONT,
            Settings.EVEN_ROW_FONT,
            Settings.ODD_ROW_FONT
    };

    private int numberOfRows = 3;
    private enum fontPositions {
        // must follow same order, linked which each row of this setting menu
        EDITOR_FONT(0), MESSAGE_PANE_FONT(1), ROWS_FONT(2);

        private final int pos;
        private fontPositions(int pos) {
            this.pos = pos;
        }
    }

    private static final int[] fontSettingPositions = {
        Settings.EDITOR_FONT,
        Settings.MESSAGE_PANE_FONT,
        rowsFontSettingPositions[0] // can be any in rowsFontSettingPositions because
        // we are always changing all the fonts together
    };

    JButton[] fontButtons;
    JCheckBox[] defaultCheckBoxes;
    JLabel[] samples;
    Color[] currentNondefaultBackground, currentNondefaultForeground;
    Color[] initialSettingsBackground, initialSettingsForeground;
    Font[] initialFont, currentFont, currentNondefaultFont;

    private static final int gridVGap = 2;
    private static final int gridHGap = 2;
    // Tool tips for font buttons
    private static final String SAMPLE_TOOL_TIP_TEXT = "Preview based on font settings";
    private static final String FONT_TOOL_TIP_TEXT = "Click, to select text font";
    private static final String DEFAULT_TOOL_TIP_TEXT = "Check, to select default font (disables font select buttons)";
    // Tool tips for the control buttons along the bottom
    public static final String CLOSE_TOOL_TIP_TEXT = "Apply current settings and close dialog";
    public static final String APPLY_TOOL_TIP_TEXT = "Apply current settings now and leave dialog open";
    public static final String RESET_TOOL_TIP_TEXT = "Reset to initial settings without applying";
    public static final String CANCEL_TOOL_TIP_TEXT = "Close dialog without applying current settings";

    private static final String fontButtonText = "font";

    /**
     * Create a new SettingsFontsAction.  Has all the GuiAction parameters.
     */
    public SettingsFontsAction(String name, Icon icon, String descrip,
                                      Integer mnemonic, KeyStroke accel) {
        super(name, icon, descrip, mnemonic, accel);
    }

    /**
     * When this action is triggered, launch a dialog to view and modify
     * font settings.
     */
    public void actionPerformed(ActionEvent e) {
        fontDialog = new JDialog(Globals.getGui(), "Font Settings", true);
        fontDialog.setContentPane(buildDialogPanel());
        fontDialog.setDefaultCloseOperation(
                JDialog.DO_NOTHING_ON_CLOSE);
        fontDialog.addWindowListener(
                new WindowAdapter() {
                    public void windowClosing(WindowEvent we) {
                        closeDialog();
                    }
                });
        fontDialog.pack();
        fontDialog.setLocationRelativeTo(Globals.getGui());
        fontDialog.setVisible(true);
    }

    // The dialog box that appears when menu item is selected.
    private JPanel buildDialogPanel() {
        JPanel contents = new JPanel(new BorderLayout(20, 20));
        contents.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel patches = new JPanel(new GridLayout(numberOfRows, 4, gridVGap, gridHGap));
        currentNondefaultBackground = new Color[numberOfRows];
        currentNondefaultForeground = new Color[numberOfRows];
        initialSettingsBackground = new Color[numberOfRows];
        initialSettingsForeground = new Color[numberOfRows];
        initialFont = new Font[numberOfRows];
        currentFont = new Font[numberOfRows];
        currentNondefaultFont = new Font[numberOfRows];

        fontButtons = new JButton[numberOfRows];
        defaultCheckBoxes = new JCheckBox[numberOfRows];
        samples = new JLabel[numberOfRows];
        for (int i = 0; i < numberOfRows; i++) {
            fontButtons[i] = new JButton(fontButtonText);
            defaultCheckBoxes[i] = new JCheckBox();
            samples[i] = new JLabel(" preview ");
            fontButtons[i].addActionListener(new FontChanger(i));
            defaultCheckBoxes[i].addItemListener(new DefaultChanger(i));
            samples[i].setToolTipText(SAMPLE_TOOL_TIP_TEXT);
            fontButtons[i].setToolTipText(FONT_TOOL_TIP_TEXT);
            defaultCheckBoxes[i].setToolTipText(DEFAULT_TOOL_TIP_TEXT);
        }

        initializeButtonFonts();

        for (int i = 0; i < numberOfRows; i++) {
            patches.add(fontButtons[i]);
            patches.add(defaultCheckBoxes[i]);
        }

        JPanel descriptions = new JPanel(new GridLayout(numberOfRows, 1, gridVGap, gridHGap));
        // Note the labels have to match buttons by position...
        descriptions.add(new JLabel("Editor", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Message Pane (Bottom Pane)", SwingConstants.RIGHT));
        descriptions.add(new JLabel("Even and Odd Rows (Memory)", SwingConstants.RIGHT));

        JPanel sample = new JPanel(new GridLayout(numberOfRows, 1, gridVGap, gridHGap));
        for (int i = 0; i < numberOfRows; i++) {
            sample.add(samples[i]);
        }

        JPanel instructions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // create deaf, dumb and blind checkbox, for illustration
        JCheckBox illustrate =
                new JCheckBox() {
                    protected void processMouseEvent(MouseEvent e) {
                    }

                    protected void processKeyEvent(KeyEvent e) {
                    }
                };
        illustrate.setSelected(true);
        instructions.add(illustrate);
        instructions.add(new JLabel("= use default font (disables font selection buttons)"));
        int spacer = 10;
        Box mainArea = Box.createHorizontalBox();
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(descriptions);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(sample);
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(Box.createHorizontalGlue());
        mainArea.add(Box.createHorizontalStrut(spacer));
        mainArea.add(patches);

        contents.add(mainArea, BorderLayout.EAST);
        contents.add(instructions, BorderLayout.NORTH);

        // Bottom row - the control buttons for Apply&Close, Apply, Cancel
        Box controlPanel = Box.createHorizontalBox();
        JButton okButton = new JButton("Apply and Close");
        okButton.setToolTipText(CLOSE_TOOL_TIP_TEXT);
        okButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setFontSettings();
                        closeDialog();
                    }
                });
        JButton applyButton = new JButton("Apply");
        applyButton.setToolTipText(APPLY_TOOL_TIP_TEXT);
        applyButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        setFontSettings();
                    }
                });
        JButton resetButton = new JButton("Reset");
        resetButton.setToolTipText(RESET_TOOL_TIP_TEXT);
        resetButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        resetButtonFonts();
                    }
                });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText(CANCEL_TOOL_TIP_TEXT);
        cancelButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        closeDialog();
                    }
                });
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(okButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(applyButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(cancelButton);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(resetButton);
        controlPanel.add(Box.createHorizontalGlue());

        JPanel allControls = new JPanel(new GridLayout(2, 1));
        allControls.add(controlPanel);
        contents.add(allControls, BorderLayout.SOUTH);
        return contents;
    }

    // Called once, upon dialog setup.
    private void initializeButtonFonts() {
        Settings settings = Globals.getSettings();
        LineBorder lineBorder = new LineBorder(Color.BLACK);
        Font fontSetting;
        boolean usingDefaults;

        for (int i = 0; i < numberOfRows; i++) {
            fontSetting = settings.getFontByPosition(fontSettingPositions[i]);
            fontButtons[i].setFont(MonoRightCellRenderer.MONOSPACED_PLAIN_12POINT); //fontSetting);
            fontButtons[i].setMargin(new Insets(4, 4, 4, 4));
            initialFont[i] = currentFont[i] = fontSetting;
            currentNondefaultFont[i] = fontSetting;
            samples[i].setOpaque(true); // otherwise, background color will not be rendered
            samples[i].setBorder(lineBorder);
            samples[i].setFont(fontSetting);
            usingDefaults = fontSetting.equals(settings.getDefaultFontByPosition(fontSettingPositions[i]));
            defaultCheckBoxes[i].setSelected(usingDefaults);
            fontButtons[i].setEnabled(!usingDefaults);
        }
    }


    // Set the font settings according to current button fonts.  Occurs when "Apply" selected.
    private void setFontSettings() {
        Settings settings = Globals.getSettings();
        // changes the font for all rows in memory, register and execute pane
        for (int i = 0; i < rowsFontSettingPositions.length; i++)
            settings.setFontByPosition(rowsFontSettingPositions[i], samples[fontPositions.ROWS_FONT.pos].getFont());
        // changes font for the Editor and Message Pane if new font set
        settings.setFontByPosition(fontSettingPositions[fontPositions.EDITOR_FONT.pos], 
            samples[fontPositions.EDITOR_FONT.pos].getFont());
        settings.setFontByPosition(fontSettingPositions[fontPositions.MESSAGE_PANE_FONT.pos], 
            samples[fontPositions.MESSAGE_PANE_FONT.pos].getFont());

        ExecutePane executePane = Globals.getGui().getMainPane().getExecutePane();
        executePane.getRegistersWindow().refresh();
        executePane.getControlAndStatusWindow().refresh();
        executePane.getFloatingPointWindow().refresh();
        // If a successful assembly has occurred, the various panes will be populated with tables
        // and we want to apply the new settings.  If it has NOT occurred, there are no tables
        // in the Data and Text segment windows so we don't want to disturb them.
        // In the latter case, the component count for the Text segment window is 0 (but is 1
        // for Data segment window).
        if (executePane.getTextSegmentWindow().getContentPane().getComponentCount() > 0) {
            executePane.getDataSegmentWindow().updateValues();
            executePane.getTextSegmentWindow().highlightStepAtPC();
        }
    }

    // Called when Reset selected.
    private void resetButtonFonts() {
        Settings settings = Globals.getSettings();
        Font fontSetting;
        for (int i = 0; i < numberOfRows; i++) {
            fontSetting = initialFont[i];
            samples[i].setFont(fontSetting);
            boolean usingDefaults = fontSetting.equals(settings.getDefaultFontByPosition(fontSettingPositions[i]));
            defaultCheckBoxes[i].setSelected(usingDefaults);
            fontButtons[i].setEnabled(!usingDefaults);
        }
    }


    // We're finished with this modal dialog.
    private void closeDialog() {
        fontDialog.setVisible(false);
        fontDialog.dispose();
    }


    /////////////////////////////////////////////////////////////////
    //
    //  Class that handles click on the font select button
    //
    private class FontChanger implements ActionListener {
        private int position;

        public FontChanger(int pos) {
            position = pos;
        }

        public void actionPerformed(ActionEvent e) {
            FontSettingDialog fontDialog = new FontSettingDialog(null, "Select Text Font", samples[position].getFont());
            Font newFont = fontDialog.showDialog();
            if (newFont != null) {
                samples[position].setFont(newFont);
            }
        }
    }


    /////////////////////////////////////////////////////////////////
    //
    // Class that handles action (check, uncheck) on the Default checkbox.
    //
    private class DefaultChanger implements ItemListener {
        private int position;

        public DefaultChanger(int pos) {
            position = pos;
        }

        public void itemStateChanged(ItemEvent e) {
            Font newFont = null;
            if (e.getStateChange() == ItemEvent.SELECTED) {
                fontButtons[position].setEnabled(false);
                newFont = Globals.getSettings().getDefaultFontByPosition(rowsFontSettingPositions[position]);
                currentNondefaultFont[position] = samples[position].getFont();
            } else {
                fontButtons[position].setEnabled(true);
                newFont = currentNondefaultFont[position];
            }
            samples[position].setFont(newFont);
        }
    }

    ///////////////////////////////////////////////////////////////////
    //
    // Modal dialog to set a font.
    //
    private class FontSettingDialog extends AbstractFontSettingDialog {
        private boolean resultOK;

        public FontSettingDialog(Frame owner, String title, Font currentFont) {
            super(owner, title, true, currentFont);
        }

        private Font showDialog() {
            resultOK = true;
            // Because dialog is modal, this blocks until user terminates the dialog.
            this.setVisible(true);
            return resultOK ? getFont() : null;
        }

        protected void closeDialog() {
            this.setVisible(false);
        }

        private void performOK() {
            resultOK = true;
        }

        private void performCancel() {
            resultOK = false;
        }

        // Control buttons for the dialog.
        protected Component buildControlPanel() {
            Box controlPanel = Box.createHorizontalBox();
            JButton okButton = new JButton("OK");
            okButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            performOK();
                            closeDialog();
                        }
                    });
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            performCancel();
                            closeDialog();
                        }
                    });
            JButton resetButton = new JButton("Reset");
            resetButton.addActionListener(
                    new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            reset();
                        }
                    });
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(okButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(cancelButton);
            controlPanel.add(Box.createHorizontalGlue());
            controlPanel.add(resetButton);
            controlPanel.add(Box.createHorizontalGlue());
            return controlPanel;
        }

        // required by Abstract super class but not used here.
        protected void apply(Font font) {

        }

    }
}