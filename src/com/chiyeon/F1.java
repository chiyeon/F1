package com.chiyeon;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class F1 implements Runnable, NativeKeyListener {

    // for timer
    private static long nsPerSecond = 1000000000L;
    private boolean timerOn = false;
    private int minutes = 0, hours = 0;

    // query whether player is leaving or not
    private boolean isLeaving = false;

    // window settings
    public static String title = "F1";
    public static int width = 320;
    public static int height = 74;
    private int expansions = 0;         // expanded when hours expands past the current window limit

    // window elements;
    private JFrame frame;
    private JPanel panel;
    private JLabel clock;
    private JLabel quitMessage;

    // buttons displayed when mouse hovers
    private JPanel buttonsPanel;
    private JButton quitButton;
    private JButton settingsButton;

    // settings window
    private JFrame settingsFrame;
    private JPanel settingsPanel;
    private boolean searchingForKey = false;
    private int keyToChange;
    private JDialog newKeyMessageDialog;
    private JLabel keyLabels[];

    // main thread
    private Thread thread = new Thread(this);

    // values from config
    private static final int COLOR_BACKGROUND = 0, COLOR_TIMER_OFF = 1, COLOR_TIMER_ON = 2, COLOR_TIMER_PAUSED = 3;
    private String Colors[] = {
        "#0f0f0f",
        "#f9f7cf",
        "#adce74",
        "#98acf8"
    };
    private static final int KEY_GO = 0, KEY_RESET = 1, KEY_QUIT = 2;
    private String Keys[] = {
        "F1",
        "F2",
        "Escape"
    };
    private boolean windowAlwaysOnTop = true;

    private MouseAdapter windowMouseHandler = new MouseAdapter() {
        private Point mouseOffset;

        @Override
        public void mouseEntered(MouseEvent e) {
            panel.add(buttonsPanel, 0);
            panel.revalidate();
            panel.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(!panel.contains(e.getPoint())) {
                panel.remove(buttonsPanel);
                panel.revalidate();
                panel.repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mouseOffset != null) {
                Point position = e.getLocationOnScreen();
                int newX = position.x - mouseOffset.x;
                int newY = position.y - mouseOffset.y;
                SwingUtilities.getWindowAncestor(e.getComponent()).setLocation(newX, newY);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            Point position = e.getComponent().getLocationOnScreen();
            mouseOffset = new Point(e.getLocationOnScreen());
            mouseOffset.x -= position.x;
            mouseOffset.y -= position.y;
        }
    };

    public static void main(String[] args) {
        try {
            // register for global input recognition
            GlobalScreen.registerNativeHook();

            // this disables constant input logging in the console
            // Get the logger for "org.jnativehook" and set the level to warning.
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.WARNING);
            // Don't forget to disable the parent handlers.
            logger.setUseParentHandlers(false);
        } catch (NativeHookException e) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(e.getMessage());

            System.exit(1);
        }

        // adds global listener
        GlobalScreen.addNativeKeyListener(new F1());
    }

    public F1() {
        LoadConfig();
        // enable anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        InitializeMainFrame();
    }

    private void InitializeMainFrame() {
        // set up main panel, with mouse handler to allow relocation
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.addMouseListener(windowMouseHandler);
        panel.addMouseMotionListener(windowMouseHandler);

        // clock to display the current time
        clock = new JLabel("0.00");
        clock.setFont(clock.getFont().deriveFont(64.0f));
        clock.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(clock);

        // quit message shown when confirming user wants to exit
        quitMessage = new JLabel("Press ESC again to quit");
        quitMessage.setForeground(Color.decode("#f8f1f1"));
        quitMessage.setAlignmentX(Component.RIGHT_ALIGNMENT);

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        buttonsPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        settingsButton = new JButton("S");
        settingsButton.setBackground(Color.decode("#a6a9b6"));
        settingsButton.setBorderPainted(false);
        settingsButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                InitializeSettingsFrame();
            }
        });
        buttonsPanel.add(settingsButton);

        quitButton = new JButton("X");
        quitButton.setBackground(Color.decode("#ff4646"));
        quitButton.setBorderPainted(false);
        quitButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                System.exit(0);
            }
        });
        buttonsPanel.add(quitButton);

        UpdateClockColors();

        // main window
        frame = new JFrame(title);
        frame.add(panel);
        frame.setUndecorated(true);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setAlwaysOnTop(windowAlwaysOnTop);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void UpdateClockColors() {
        panel.setBackground(Color.decode(Colors[COLOR_BACKGROUND]));
        clock.setForeground(Color.decode(Colors[COLOR_TIMER_OFF]));
    }

    private void ShowNewKeyQuery() {
        // set up the "Press new Key" message for key bind, shown when user hits the edit button in options on a keybind
        newKeyMessageDialog = new JDialog();
        JOptionPane newKeyMessage = new JOptionPane("Hit Desired Key", JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        newKeyMessageDialog = new JDialog();
        newKeyMessageDialog.setTitle("New Key");
        newKeyMessageDialog.setModal(true);
        newKeyMessageDialog.setContentPane(newKeyMessage);
        newKeyMessageDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        newKeyMessageDialog.setAlwaysOnTop(windowAlwaysOnTop);
        newKeyMessageDialog.pack();
        newKeyMessageDialog.setLocationRelativeTo(null);
        newKeyMessageDialog.setVisible(true);
    }

    private void InitializeSettingsFrame() {

        // in order to refresh if we quit without saving
        // load backups when closing w/o saving in case player has
        // no save file
        String keysBackup[] = Keys.clone();
        String colorsBackup[] = Colors.clone();

        // the main panel of the settings window
        settingsPanel = new JPanel();
        //settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setLayout(new GridBagLayout());
        settingsPanel.setBorder(new EmptyBorder(0, 12, 12, 12));
        settingsPanel.addMouseListener(windowMouseHandler);
        settingsPanel.addMouseMotionListener(windowMouseHandler);


        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        // title for controls segment
        JLabel controlsLabel = new JLabel("Controls");
        controlsLabel.setFont(controlsLabel.getFont().deriveFont(21f));
        controlsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsPanel.add(controlsLabel, gbc);

        // the three labels that show the current key value (ie F1)
        keyLabels = new JLabel[3];
        keyLabels[KEY_GO] = new JLabel(Keys[KEY_GO]);
        keyLabels[KEY_RESET] = new JLabel(Keys[KEY_RESET]);
        keyLabels[KEY_QUIT] = new JLabel(Keys[KEY_QUIT]);

        // the three panels, left to right, that hold first the
        //      Key Label (ie Go: )
        //      Key Value (ie F1)
        //      Edit Button ([Edit])
        JPanel keyPanels[] = new JPanel[3];

        // initialize each panel, set the layout, and add the Key value label (that has been init already)
        for(int i = 0; i < 3; i++) {
            keyPanels[i] = new JPanel();
            keyPanels[i].setLayout(new GridLayout(1, 0));
            keyPanels[i].add(keyLabels[i]);
            gbc.gridy++;
            settingsPanel.add(keyPanels[i], gbc);
        }

        // set each identifier label for the key, place in FRONT of the key value
        keyPanels[KEY_GO].add(new JLabel("Go: "), 0);
        keyPanels[KEY_RESET].add(new JLabel("Reset: "), 0);
        keyPanels[KEY_QUIT].add(new JLabel("Quit: "), 0);

        // setup up each button for each panel
        JButton keyButtons[] = new JButton[3];
        for(int i = 0; i < 3; i++) {
            final int index = i;
            keyButtons[i] = new JButton("Edit");
            keyButtons[i].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    searchingForKey = true;
                    keyToChange = index;
                    ShowNewKeyQuery();
                }
            });
            keyPanels[i].add(keyButtons[i]);
        }

        // title for colors segment
        JLabel colorsLabel = new JLabel("Colors");
        colorsLabel.setFont(colorsLabel.getFont().deriveFont(21f));
        colorsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gbc.gridy++;
        settingsPanel.add(colorsLabel, gbc);

        int numColors = Colors.length;

        JPanel colorPanel[] = new JPanel[numColors];
        for(int i = 0; i < numColors; i++) {
            final int index = i;
            colorPanel[i] = new JPanel();
            JTextField textField = new JTextField(Colors[i]);
            colorPanel[i].add(textField);

            JButton colorPreview = new JButton();
            colorPreview.setOpaque(true);
            colorPreview.setBackground(Color.decode(Colors[i]));
            colorPreview.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    JColorChooser chooser = new JColorChooser();
                    AbstractColorChooserPanel[] chooserPanels = new AbstractColorChooserPanel[] { chooser.getChooserPanels()[1] };
                    chooser.setChooserPanels(chooserPanels);
                    chooser.getSelectionModel().addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            Color newColor = chooser.getColor();
                            Colors[index] = "#" + Integer.toHexString(newColor.getRGB()).substring(2);
                            ((JTextField)colorPanel[index].getComponent(1)).setText(Colors[index]);
                        }
                    });

                    JDialog dialog = new JDialog();
                    dialog.add(chooser);
                    dialog.setTitle("Choose new color");
                    dialog.setAlwaysOnTop(windowAlwaysOnTop);
                    dialog.setLocationRelativeTo(settingsFrame);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
            colorPanel[i].add(colorPreview);

            colorPanel[i].setLayout(new GridLayout(1, 0));
            gbc.gridy++;
            settingsPanel.add(colorPanel[i], gbc);
        }

        // add labels for all the colors
        colorPanel[COLOR_BACKGROUND].add(new JLabel("Background: "), 0);
        colorPanel[COLOR_TIMER_OFF].add(new JLabel("Timer Off: "), 0);
        colorPanel[COLOR_TIMER_ON].add(new JLabel("Timer On: "), 0);
        colorPanel[COLOR_TIMER_PAUSED].add(new JLabel("Timer Paused: "), 0);

        for(int i = 0; i < 4; i++) {
            final int index = i;
            JTextField textField = ((JTextField)colorPanel[index].getComponent(1));
            JButton preview = ((JButton)colorPanel[index].getComponent(2));
            textField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    update(e);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    update(e);
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    update(e);
                }

                private void update(DocumentEvent e) {
                    try {
                        preview.setBackground(Color.decode(textField.getText()));
                    } catch(Exception ex) {

                    }
                }
            });
        }

        // title for settings segment
        JLabel settingsLabel = new JLabel("Settings");
        settingsLabel.setFont(settingsLabel.getFont().deriveFont(21f));
        settingsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        gbc.gridy++;
        settingsPanel.add(settingsLabel, gbc);

        // panel to hold option for always on top window
        JPanel alwaysOnTopPanel = new JPanel();
        alwaysOnTopPanel.setLayout(new GridLayout(0, 1));
        gbc.gridy++;
        settingsPanel.add(alwaysOnTopPanel, gbc);

        // checkbox for always on top option
        JCheckBox alwaysOnTopCheckBox = new JCheckBox("Always on Top: ");
        alwaysOnTopCheckBox.setHorizontalTextPosition(SwingConstants.LEFT);
        alwaysOnTopCheckBox.setSelected(windowAlwaysOnTop);
        alwaysOnTopPanel.add(alwaysOnTopCheckBox);

        JButton saveButton = new JButton("Save and Close");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // update colors
                for(int i = 0; i < numColors; i++) {
                    Colors[i] = ((JTextField)colorPanel[i].getComponent(1)).getText();
                }
                windowAlwaysOnTop = alwaysOnTopCheckBox.isSelected();
                frame.setAlwaysOnTop(windowAlwaysOnTop);
                UpdateClockColors();
                panel.validate();
                panel.repaint();
                SaveConfig();
                settingsFrame.dispose();
            }
        });
        gbc.gridy++;
        settingsPanel.add(saveButton, gbc);

        // the main frame
        settingsFrame = new JFrame("Options");

        settingsFrame.addWindowListener(new WindowAdapter() {
            // reset the keys, so that it does NOT save any changes
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                super.windowClosing(windowEvent);
                Keys = keysBackup;
                Colors = colorsBackup;
            }
        });

        settingsFrame.add(settingsPanel);
        settingsFrame.setSize(360, 640);
        settingsFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        settingsFrame.setAlwaysOnTop(windowAlwaysOnTop);
        settingsFrame.pack();
        settingsFrame.setLocationRelativeTo(null);
        settingsFrame.setVisible(true);
    }

    public Color getInvertedColor(Color color) {
        return new Color(255 - color.getRed(), 255 - color.getBlue(), 255 - color.getGreen());
    }

    private void LoadConfig() {
        try {
            FileReader fstream = new FileReader("config.properties");
            BufferedReader in = new BufferedReader(fstream);
            Object lines[] = in.lines().toArray();
            for(int i = 0; i < Colors.length; i++) {
                Colors[i] = lines[i].toString().substring(10);
            }
            for(int i = Colors.length; i < lines.length - 1; i++) {
                Keys[i-Colors.length] = lines[i].toString().substring(7);
            }

            windowAlwaysOnTop = Boolean.parseBoolean(((String)lines[Colors.length + Keys.length - 1]).substring(11));

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void SaveConfig() {
        try {
            FileWriter fstream = new FileWriter("config.properties");
            BufferedWriter out = new BufferedWriter(fstream);
            String contents = "";
            for(int i = 0; i < Colors.length; i++) {
                contents += "colors-" + i + ": " + Colors[i] + "\n";
            }
            for(int i = 0; i < Keys.length; i++) {
                contents += "key-" + i + ": " + Keys[i] + "\n";
            }
            contents += "always-on: " + windowAlwaysOnTop;
            out.write(contents);
            out.close();
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void StartTime() {
        clock.setForeground(Color.decode(Colors[COLOR_TIMER_ON]));
        thread.start();
        timerOn = true;
    }

    public void StopTime() {
        thread.interrupt();
        clock.setForeground(Color.decode(Colors[COLOR_TIMER_PAUSED]));
        timerOn = false;
    }

    public void ResetTime() {
        StopTime();
        thread = new Thread(this);
        clock.setForeground(Color.decode(Colors[COLOR_TIMER_OFF]));
        clock.setText("0.00");
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        long current = start;

        while(timerOn) {
            String clockText = "";
            current = System.currentTimeMillis();

            if((current-start)/1000.0 >= 60 + (hours * 60 * 60) + minutes*60) {
                minutes++;
            }

            if(minutes >= 60) {
                hours++;
                minutes = 0;

                String hoursString = String.valueOf(hours);
                if(hoursString.length() > expansions) {
                    expansions++;
                    frame.setSize(width + 72 * expansions, height);
                }
            }

            if(hours > 0) {
                float seconds = (current-start)/1000.0f - (hours * 60 * 60) - minutes * 60.0f;
                String secondsString = String.valueOf(Math.floor(seconds * 100.0)/100.0);
                clockText += String.valueOf(hours) + ":";
                clockText += String.format("%02d:", minutes);
                clockText += String.format("%02d", (int)Math.floor((double)seconds));
                clockText += String.format("%s", (secondsString.substring(secondsString.indexOf(".")) + "0").substring(0,3));
            } else {
                if (minutes > 0) {
                    float seconds = (current-start)/1000.0f - minutes * 60.0f;
                    String secondsString = String.valueOf(Math.floor(seconds * 100.0)/100.0);
                    clockText += String.valueOf(minutes) + ":";
                    clockText += String.format("%02d", (int)Math.floor((double)seconds));
                    clockText += String.format("%s", (secondsString.substring(secondsString.indexOf(".")) + "0").substring(0,3));
                }
                else
                    clockText += String.format("%.2f", ((current-start)/1000.0)-minutes*60);
            }

            clock.setText(clockText);
            if(System.getProperty("os.name").equals("Linux")) {
                Toolkit.getDefaultToolkit().sync();
            }
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if(NativeKeyEvent.getKeyText(e.getKeyCode()).equals(Keys[KEY_GO])) {
            if(timerOn) {
                StopTime();
            } else {
                if(clock.getText() == "0.00") {
                    StartTime();
                } else {
                    ResetTime();
                }
            }
        } else if(NativeKeyEvent.getKeyText(e.getKeyCode()).equals(Keys[KEY_RESET])) {
            ResetTime();
        } else if(NativeKeyEvent.getKeyText(e.getKeyCode()).equals(Keys[KEY_QUIT])) {
            if(frame.isFocused()) {
                if(isLeaving) {
                    System.exit(0);
                } else {
                    isLeaving = true;
                    panel.add(quitMessage, 0);
                    panel.revalidate();
                    panel.repaint();

                    Timer resetTimer = new Timer(3000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            isLeaving = false;
                            panel.remove(quitMessage);
                            panel.revalidate();
                            panel.repaint();
                        }
                    });
                    resetTimer.setRepeats(false); // Only execute once
                    resetTimer.start(); // Go go go!
                }
            }
        }

        if(searchingForKey) {
            String newKey = NativeKeyEvent.getKeyText(e.getKeyCode());
            Keys[keyToChange] = newKey;
            searchingForKey = false;
            keyLabels[keyToChange].setText(newKey);
            settingsPanel.revalidate();
            settingsPanel.repaint();
            newKeyMessageDialog.dispose();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {

    }
}
