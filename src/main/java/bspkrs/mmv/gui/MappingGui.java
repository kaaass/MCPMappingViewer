/*
 * Copyright (C) 2015 bspkrs
 * Portions Copyright (C) 2014 Alex "immibis" Campbell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bspkrs.mmv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import bspkrs.mmv.McpMappingLoader;
import bspkrs.mmv.McpMappingLoader.CantLoadMCPMappingException;
import bspkrs.mmv.VersionFetcher;
import bspkrs.mmv.version.AppVersionChecker;
import immibis.bon.IProgressListener;

public class MappingGui extends JFrame
{
    public static final String                  VERSION_NUMBER        = "1.0.1";
    private static final long                   serialVersionUID      = 1L;
    private final Preferences                   prefs                 = Preferences.userNodeForPackage(MappingGui.class);
    private JFrame                              frmMcpMappingViewer;
    private JButton                             btnRefreshTables;
    private JComboBox<String>                   cmbMappingVersion;
    private JCheckBox                           chkForceRefresh;
    private JCheckBox                           chkUseMirror;
    private JPanel                              pnlProgress;
    private JProgressBar                        progressBar;
    private JPanel                              pnlFilter;
    private JComboBox<String>                   cmbFilter;
    private JButton                             btnSearch;
    private JButton                             btnGetBotCommands;
    private JCheckBox                           chkClearOnCopy;
    private final static String                 PREFS_KEY_FILTER      = "filter";
    private final static String                 PREFS_KEY_CLASS_SORT  = "classSort";
    private final static String                 PREFS_KEY_METHOD_SORT = "methodSort";
    private final static String                 PREFS_KEY_PARAM_SORT  = "paramSort";
    private final static String                 PREFS_KEY_FIELD_SORT  = "fieldSort";
    private final List<RowSorter.SortKey>       classSort             = new ArrayList<RowSorter.SortKey>();
    private final List<RowSorter.SortKey>       methodSort            = new ArrayList<RowSorter.SortKey>();
    private final List<RowSorter.SortKey>       paramSort             = new ArrayList<RowSorter.SortKey>();
    private final List<RowSorter.SortKey>       fieldSort             = new ArrayList<RowSorter.SortKey>();
    private JTable                              tblClasses;
    private JTable                              tblMethods;
    private JTable                              tblFields;
    private JTable                              tblParams;
    private Thread                              curTask               = null;
    private final Map<String, McpMappingLoader> mcpInstances          = new HashMap<>();
    private final VersionFetcher                versionFetcher        = new VersionFetcher();
    private McpMappingLoader                    currentLoader;
    private AppVersionChecker                   versionChecker;
    private final String                        versionURL            = "http://bspk.rs/Minecraft/MMV/MMV.version";
    private final String                        mcfTopic              = "http://www.minecraftforum.net/topic/2115030-";

    // @formatter:off
    public static DefaultTableModel classesDefaultModel = new DefaultTableModel(new Object[][] { {}, }, new String[] { "包名", "SRG名", "混淆名" })
    {
        private static final long serialVersionUID = 1L;
        boolean[]                 columnEditables  = new boolean[] { false, false, false };
        @SuppressWarnings("rawtypes")
        Class[]                   columnTypes      = new Class[] { String.class, String.class, String.class };

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Class getColumnClass(int columnIndex) { return columnTypes[columnIndex]; }

        @Override
        public boolean isCellEditable(int row, int column) { return columnEditables[column]; }
    };

    public static DefaultTableModel methodsDefaultModel = new DefaultTableModel( new Object[][] { {}, }, new String[] { "MCP名", "SRG名", "混淆名", "SRG描述符", "注释" })
    {
        private static final long serialVersionUID = 1L;
        boolean[]                 columnEditables  = new boolean[] { false, false, false, false, false };
        @SuppressWarnings("rawtypes")
        Class[]                   columnTypes      = new Class[] { String.class, String.class, String.class, String.class, String.class };

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Class getColumnClass(int columnIndex) { return columnTypes[columnIndex]; }

        @Override
        public boolean isCellEditable(int row, int column) { return columnEditables[column]; }
    };

    public static DefaultTableModel paramsDefaultModel = new DefaultTableModel( new Object[][] { {}, }, new String[] { "MCP名", "SRG名", "类型" })
    {
        private static final long serialVersionUID = 1L;
        boolean[]                 columnEditables  = new boolean[] { false, false, false };
        @SuppressWarnings("rawtypes")
        Class[]                   columnTypes      = new Class[] { String.class, String.class, String.class };

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Class getColumnClass(int columnIndex) { return columnTypes[columnIndex]; }

        @Override
        public boolean isCellEditable(int row, int column) { return columnEditables[column]; }
    };

    public static DefaultTableModel fieldsDefaultModel = new DefaultTableModel( new Object[][] { {}, }, new String[] { "MCP名", "SRG名", "混淆名", "注释" } )
    {
        private static final long serialVersionUID = 1L;
        boolean[]                 columnEditables  = new boolean[] { false, false, false, false };
        @SuppressWarnings("rawtypes")
        Class[]                   columnTypes      = new Class[] { String.class, String.class, String.class, String.class };

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Class getColumnClass(int columnIndex) { return columnTypes[columnIndex]; }

        @Override
        public boolean isCellEditable(int row, int column) { return columnEditables[column]; }
    };
    private JSplitPane splitMethods;
    private JButton btnGetVersions;
    // @formatter:on

    private void savePrefs()
    {
        for (int i = 0; i < Math.min(cmbFilter.getItemCount(), 20); i++)
            prefs.put(PREFS_KEY_FILTER + i, cmbFilter.getItemAt(i));

        if (tblClasses.getRowSorter().getSortKeys().size() > 0)
        {
            int i = tblClasses.getRowSorter().getSortKeys().get(0).getColumn() + 1;
            SortOrder order = tblClasses.getRowSorter().getSortKeys().get(0).getSortOrder();
            prefs.putInt(PREFS_KEY_CLASS_SORT, order == SortOrder.DESCENDING ? i * -1 : i);
        }
        else
            prefs.putInt(PREFS_KEY_CLASS_SORT, 1);

        if (tblMethods.getRowSorter().getSortKeys().size() > 0)
        {
            int i = tblMethods.getRowSorter().getSortKeys().get(0).getColumn() + 1;
            SortOrder order = tblMethods.getRowSorter().getSortKeys().get(0).getSortOrder();
            prefs.putInt(PREFS_KEY_METHOD_SORT, order == SortOrder.DESCENDING ? i * -1 : i);
        }
        else
            prefs.putInt(PREFS_KEY_METHOD_SORT, 1);

        if (tblParams.getRowSorter().getSortKeys().size() > 0)
        {
            int i = tblParams.getRowSorter().getSortKeys().get(0).getColumn() + 1;
            SortOrder order = tblParams.getRowSorter().getSortKeys().get(0).getSortOrder();
            prefs.putInt(PREFS_KEY_PARAM_SORT, order == SortOrder.DESCENDING ? i * -1 : i);
        }
        else
            prefs.putInt(PREFS_KEY_PARAM_SORT, 1);

        if (tblFields.getRowSorter().getSortKeys().size() > 0)
        {
            int i = tblFields.getRowSorter().getSortKeys().get(0).getColumn() + 1;
            SortOrder order = tblFields.getRowSorter().getSortKeys().get(0).getSortOrder();
            prefs.putInt(PREFS_KEY_FIELD_SORT, order == SortOrder.DESCENDING ? i * -1 : i);
        }
        else
            prefs.putInt(PREFS_KEY_FIELD_SORT, 1);
    }

    private void loadPrefs(boolean sortOnly)
    {
        if (!sortOnly)
        {
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cmbFilter.getModel();
            for (int i = 0; i < 20; i++)
            {
                String item = prefs.get(PREFS_KEY_FILTER + i, " ");
                if (!item.equals(" "))
                {
                    if (model.getIndexOf(item) == -1)
                        cmbFilter.addItem(item);
                }
            }

            cmbFilter.setSelectedIndex(-1);

            if (cmbMappingVersion.getItemCount() > 0)
            {
                btnRefreshTables.setEnabled(true);
                cmbMappingVersion.setSelectedIndex(0);
            }
            else
                btnRefreshTables.setEnabled(false);
        }

        classSort.clear();
        methodSort.clear();
        paramSort.clear();
        fieldSort.clear();

        int i = prefs.getInt(PREFS_KEY_CLASS_SORT, 1);
        classSort.add(new RowSorter.SortKey(Math.abs(i) - 1, i > 0 ? SortOrder.ASCENDING : SortOrder.DESCENDING));
        tblClasses.getRowSorter().setSortKeys(classSort);

        i = prefs.getInt(PREFS_KEY_METHOD_SORT, 1);
        methodSort.add(new RowSorter.SortKey(Math.abs(i) - 1, i > 0 ? SortOrder.ASCENDING : SortOrder.DESCENDING));
        tblMethods.getRowSorter().setSortKeys(methodSort);

        i = prefs.getInt(PREFS_KEY_PARAM_SORT, 1);
        paramSort.add(new RowSorter.SortKey(Math.abs(i) - 1, i > 0 ? SortOrder.ASCENDING : SortOrder.DESCENDING));
        tblParams.getRowSorter().setSortKeys(paramSort);

        i = prefs.getInt(PREFS_KEY_FIELD_SORT, 1);
        fieldSort.add(new RowSorter.SortKey(Math.abs(i) - 1, i > 0 ? SortOrder.ASCENDING : SortOrder.DESCENDING));
        tblFields.getRowSorter().setSortKeys(fieldSort);
    }

    private void checkForUpdates()
    {
        versionChecker = new AppVersionChecker("MCP Mapping 查看器", VERSION_NUMBER, versionURL, mcfTopic,
                new String[] { "{appName} 的版本 {oldVer} 已经过时了! 访问 {updateURL} 以下载最新版本（{newVer}）。" },
                new String[] {
                        "{appName} 的版本 {oldVer} 已经过时了! <br/><br/>访问 <a href=\"{updateURL}\">{updateURL}</a> 以下载最新版本（{newVer}）。" },
                5000);
        if (!versionChecker.isCurrentVersion())
        {
            showHTMLDialog(MappingGui.this, versionChecker.getDialogMessage()[0], "有新的版本", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
        try
        {
            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Throwable e)
        {}
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    MappingGui window = new MappingGui();
                    window.frmMcpMappingViewer.setVisible(true);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private static String getPrintableStackTrace(Throwable e, Set<StackTraceElement> stopAt)
    {
        String s = e.toString();
        int numPrinted = 0;
        for (StackTraceElement ste : e.getStackTrace())
        {
            boolean stopHere = false;
            if (stopAt.contains(ste) && numPrinted > 0)
                stopHere = true;
            else
            {
                s += "\n    at " + ste.toString();
                numPrinted++;
                if (ste.getClassName().startsWith("javax.swing."))
                    stopHere = true;
            }

            if (stopHere)
            {
                int numHidden = e.getStackTrace().length - numPrinted;
                s += "\n    ... " + numHidden + " more";
                break;
            }
        }
        return s;
    }

    private static String getStackTraceMessage(String prefix, Throwable e)
    {
        String s = prefix;

        s += "\n" + getPrintableStackTrace(e, Collections.<StackTraceElement> emptySet());
        while (e.getCause() != null)
        {
            Set<StackTraceElement> stopAt = new HashSet<StackTraceElement>(Arrays.asList(e.getStackTrace()));
            e = e.getCause();
            s += "\nCaused by: " + getPrintableStackTrace(e, stopAt);
        }
        return s;
    }

    /**
     * Create the application.
     */
    public MappingGui()
    {
        initialize();
        checkForUpdates();
    }

    public void setCsvFileEdited(boolean bol)
    {
        btnGetBotCommands.setEnabled(bol);
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize()
    {
        frmMcpMappingViewer = new JFrame();
        frmMcpMappingViewer.setIconImage(new ImageIcon(MappingGui.class.getResource("/bspkrs/mmv/gui/icon/bspkrs32.png")).getImage());
        frmMcpMappingViewer.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent arg0)
            {
                savePrefs();
            }
        });
        frmMcpMappingViewer.setTitle("MCP Mapping 查看器");
        frmMcpMappingViewer.setBounds(100, 100, 950, 621);
        frmMcpMappingViewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frmMcpMappingViewer.getContentPane().setLayout(new BorderLayout(0, 0));

        JSplitPane splitMain = new JSplitPane();
        splitMain.setBorder(null);
        splitMain.setDividerSize(3);
        splitMain.setResizeWeight(0.5);
        splitMain.setContinuousLayout(true);
        splitMain.setMinimumSize(new Dimension(179, 80));
        splitMain.setPreferredSize(new Dimension(179, 80));
        splitMain.setOrientation(JSplitPane.VERTICAL_SPLIT);

        JScrollPane scrlpnClasses = new JScrollPane();
        scrlpnClasses.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        splitMain.setLeftComponent(scrlpnClasses);

        tblClasses = new JTable();
        tblClasses.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scrlpnClasses.setViewportView(tblClasses);
        tblClasses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblClasses.getSelectionModel().addListSelectionListener(new ClassTableSelectionListener(tblClasses));
        tblClasses.setAutoCreateRowSorter(true);
        tblClasses.setEnabled(false);
        tblClasses.setModel(classesDefaultModel);
        tblClasses.setFillsViewportHeight(true);
        tblClasses.setCellSelectionEnabled(true);
        frmMcpMappingViewer.getContentPane().add(splitMain, BorderLayout.CENTER);

        JSplitPane splitMembers = new JSplitPane();
        splitMembers.setBorder(null);
        splitMembers.setDividerSize(3);
        splitMembers.setResizeWeight(0.5);
        splitMembers.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitMain.setRightComponent(splitMembers);

        splitMethods = new JSplitPane();
        splitMethods.setBorder(null);
        splitMethods.setDividerSize(3);
        splitMethods.setResizeWeight(0.5);
        splitMembers.setLeftComponent(splitMethods);

        JScrollPane scrlpnMethods = new JScrollPane();
        scrlpnMethods.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        splitMethods.setLeftComponent(scrlpnMethods);

        tblMethods = new JTable();
        tblMethods.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblMethods.getSelectionModel().addListSelectionListener(new MethodTableSelectionListener(tblMethods));
        tblMethods.setCellSelectionEnabled(true);
        tblMethods.setFillsViewportHeight(true);
        tblMethods.setAutoCreateRowSorter(true);
        tblMethods.setEnabled(false);
        tblMethods.setModel(methodsDefaultModel);
        scrlpnMethods.setViewportView(tblMethods);

        JScrollPane scrlpnParams = new JScrollPane();
        scrlpnParams.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        splitMethods.setRightComponent(scrlpnParams);

        tblParams = new JTable();
        tblParams.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblParams.setCellSelectionEnabled(true);
        tblParams.setFillsViewportHeight(true);
        tblParams.setAutoCreateRowSorter(true);
        tblParams.setEnabled(false);
        tblParams.setModel(paramsDefaultModel);
        scrlpnParams.setViewportView(tblParams);

        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                splitMethods.setDividerLocation(0.8);
            }
        });

        JScrollPane scrlpnFields = new JScrollPane();
        scrlpnFields.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        splitMembers.setRightComponent(scrlpnFields);

        tblFields = new JTable();
        tblFields.setCellSelectionEnabled(true);
        tblFields.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblFields.setAutoCreateRowSorter(true);
        tblFields.setEnabled(false);
        tblFields.setModel(fieldsDefaultModel);
        tblFields.setFillsViewportHeight(true);
        scrlpnFields.setViewportView(tblFields);

        JPanel pnlHeader = new JPanel();
        frmMcpMappingViewer.getContentPane().add(pnlHeader, BorderLayout.NORTH);
        pnlHeader.setLayout(new BorderLayout(0, 0));

        JPanel pnlControls = new JPanel();
        pnlHeader.add(pnlControls, BorderLayout.NORTH);
        pnlControls.setSize(new Dimension(0, 40));
        pnlControls.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 2));

        cmbMappingVersion = new JComboBox<String>(new DefaultComboBoxModel<String>());
        cmbMappingVersion.setEditable(false);
        cmbMappingVersion.setPreferredSize(new Dimension(320, 20));
        cmbMappingVersion.addItemListener(new MappingVersionsComboItemChanged());

        JLabel lblMappingVersion = new JLabel("Mapping 版本");
        pnlControls.add(lblMappingVersion);
        pnlControls.add(cmbMappingVersion);

        btnGetVersions = new JButton("获取版本列表");
        btnGetVersions.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    cmbMappingVersion.removeAllItems();
                    for (String s : versionFetcher.getVersions(chkForceRefresh.isSelected(), chkUseMirror.isSelected()))
                    {
                        cmbMappingVersion.addItem(s);
                    }
                }
                catch (IOException exc)
                {}
            }
        });
        pnlControls.add(btnGetVersions);

        btnRefreshTables = new JButton("加载 Mappings");
        btnRefreshTables.setEnabled(false);
        btnRefreshTables.addActionListener(new RefreshActionListener());
        pnlControls.add(btnRefreshTables);

        chkForceRefresh = new JCheckBox("强制重载");
        chkForceRefresh.setToolTipText("不使用缓存，强制重新下载");
        pnlControls.add(chkForceRefresh);

        chkUseMirror = new JCheckBox("使用镜像服务器", true);
        chkUseMirror.setToolTipText("可以加速读取，由KAAAsS维护，国内建议勾选");
        pnlControls.add(chkUseMirror);

        pnlProgress = new JPanel();
        pnlProgress.setVisible(false);
        pnlHeader.add(pnlProgress, BorderLayout.SOUTH);
        pnlProgress.setLayout(new BorderLayout(0, 0));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setForeground(UIManager.getColor("ProgressBar.foreground"));
        pnlProgress.add(progressBar);

        pnlFilter = new JPanel();
        FlowLayout flowLayout = (FlowLayout) pnlFilter.getLayout();
        flowLayout.setVgap(2);
        flowLayout.setAlignment(FlowLayout.LEFT);
        pnlFilter.setVisible(true);
        pnlHeader.add(pnlFilter, BorderLayout.CENTER);

        JLabel lblFilter = new JLabel("搜索");
        pnlFilter.add(lblFilter);

        cmbFilter = new JComboBox<String>();
        cmbFilter.setEditable(true);
        cmbFilter.setPreferredSize(new Dimension(300, 20));
        cmbFilter.setMaximumRowCount(10);
        pnlFilter.add(cmbFilter);

        btnSearch = new JButton("🔍");
        btnSearch.setToolTipText("");
        btnSearch.addActionListener(new SearchActionListener());
        pnlFilter.add(btnSearch);
        cmbFilter.setEnabled(false);
        cmbFilter.addActionListener(new FilterComboTextEdited());
        cmbFilter.getEditor().getEditorComponent().addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                cmbFilter.getEditor().selectAll();
            }
        });
        cmbFilter.getEditor().getEditorComponent().addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    btnSearch.doClick();
            }
        });
        btnSearch.setEnabled(false);

        JLabel lblSearchInfo = new JLabel("注意事项");
        lblSearchInfo.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                String message = "搜索功能是全局的，并且会返回所有包含匹配输入的类。 \n" +
                        "搜索是大小写敏感的！\n\n可搜索的数据：\n" +
                        "类:\n    ~ 包名\n    ~ SRG名\n    ~ 混淆名\n" +
                        "方法/字段:\n    ~ SRG名\n    ~ 混淆名\n    ~ MCP名\n    ~ 注释\n\n" +
                        "参数尚不能被搜索。";
                JOptionPane.showMessageDialog(MappingGui.this, message, "搜索信息", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        lblSearchInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblSearchInfo.setForeground(Color.BLUE);
        pnlFilter.add(lblSearchInfo);

        JSeparator separator = new JSeparator();
        separator.setPreferredSize(new Dimension(1, 12));
        separator.setOrientation(SwingConstants.VERTICAL);
        pnlFilter.add(separator);

        JLabel lblAbout = new JLabel("关于");
        pnlFilter.add(lblAbout);
        lblAbout.setForeground(Color.BLUE);
        lblAbout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JSeparator separator_1 = new JSeparator();
        separator_1.setPreferredSize(new Dimension(1, 12));
        separator_1.setOrientation(SwingConstants.VERTICAL);
        pnlFilter.add(separator_1);

        btnGetBotCommands = new JButton("复制命令列表");
        btnGetBotCommands.setToolTipText("将你在GUI进行的编辑动作以MCPBot指令导出至系统剪切板");
        btnGetBotCommands.setEnabled(false);
        btnGetBotCommands.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String commands = currentLoader.getBotCommands(chkClearOnCopy.isSelected());
                if (commands != null && !commands.isEmpty())
                {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(commands), null);
                    JOptionPane.showMessageDialog(MappingGui.this, "命令已复制到剪切板: \n" + commands, "MMV - MCPBot命令",
                            JOptionPane.INFORMATION_MESSAGE);

                    if (chkClearOnCopy.isSelected())
                        btnGetBotCommands.setEnabled(false);
                }
                else
                    JOptionPane.showMessageDialog(MappingGui.this, "没有可复制的命令", "MMV - MCPBot命令", JOptionPane.INFORMATION_MESSAGE);

                chkClearOnCopy.setSelected(false);
            }
        });
        pnlFilter.add(btnGetBotCommands);

        chkClearOnCopy = new JCheckBox("清除");
        chkClearOnCopy.setToolTipText("是否在点击按钮后清除MCPBot命令列表");
        pnlFilter.add(chkClearOnCopy);

        lblAbout.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                MappingGui.class.getClassLoader();
                String imgsrc = MappingGui.class.getResource("/bspkrs/mmv/gui/icon/bspkrs.png").toString();
                String year = new SimpleDateFormat("yyyy").format(new Date());
                String message = "<center><img src=\"" + imgsrc + "\"/><br/>" +
                        "MCP Mapping 查看器 v" + VERSION_NUMBER + "<br/>" +
                        "Copyright (C) 2013-" + year + " bspkrs<br/>" +
                        "Portions Copyright (C) 2013 Alex \"immibis\" Campbell<br/><br/>" +
                        "作者: bspkrs<br/>" +
                        "汉化&Mirror: KAAAsS<br/>" +
                        "Credits: immibis (for <a href=\"https://github.com/immibis/bearded-octo-nemesis\">BON</a> code), " +
                        "Searge et al (for <a href=\"http://mcp.ocean-labs.de\">MCP</a>)<br/><br/>" +
                        "<a href=\"" + mcfTopic + "\">MCF原发布帖</a><br/>" +
                        "<a href=\"https://github.com/kaaass/MCPMappingViewer_CN\">Github仓库</a><br/>" +
                        "<a href=\"https://github.com/kaaass/MCPMappingViewer_CN/blob/master/change.log\">变更日志</a><br/>" +
                        "<a href=\"http://bspk.rs/MC/MCPMappingViewer/index.html\">编译版下载</a><br/>" +
                        "<a href=\"https://raw.githubusercontent.com/kaaass/MCPMappingViewer_CN/master/LICENSE\">License</a><br/>" +
                        "<a href=\"https://raw.githubusercontent.com/google/gson/master/LICENSE\">GSON License</a><br/>" +
                        "<a href=\"https://twitter.com/bspkrs\">bspkrs的Twitter</a><br/>" +
                        "<a href=\"https://kaaass.net\">KAAAsS的主页</a></center>";
                showHTMLDialog(MappingGui.this, message, "关于 MCP Mapping 查看器", JOptionPane.PLAIN_MESSAGE);
            }
        });

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                savePrefs();
            }
        });

        try
        {
            loadPrefs(false);
        }
        catch (Throwable e)
        {
            String s = getStackTraceMessage(
                    "An error has occurred - give bspkrs this stack trace (which has been copied to the clipboard) if the error continues to occur on launch.\n",
                    e);

            System.err.println(s);

            final String errMsg = s;
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    progressBar.setString(" ");
                    progressBar.setValue(0);

                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(errMsg), null);
                    JOptionPane.showMessageDialog(MappingGui.this, errMsg, "MMV - Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    class MappingVersionsComboItemChanged implements ItemListener
    {
        @Override
        public void itemStateChanged(ItemEvent e)
        {
            if (e.getStateChange() == ItemEvent.SELECTED)
            {
                @SuppressWarnings("unchecked")
                JComboBox<String> cmb = (JComboBox<String>) e.getSource();
                btnRefreshTables.setEnabled(cmb.getItemCount() > 0);
            }
        }
    }

    class FilterComboTextEdited implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (e.getActionCommand().equals("comboBoxEdited"))
            {
                String filterText = cmbFilter.getSelectedItem().toString();

                if (filterText == null || filterText.trim().isEmpty())
                    return;

                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cmbFilter.getModel();

                if (model.getIndexOf(filterText) != -1)
                    model.removeElement(filterText);

                cmbFilter.insertItemAt(filterText, 0);
                cmbFilter.setSelectedItem(filterText);
            }
        }
    }

    class ClassTableSelectionListener implements ListSelectionListener
    {
        private final JTable table;

        public ClassTableSelectionListener(JTable table)
        {
            this.table = table;
        }

        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting() && !table.getModel().equals(classesDefaultModel))
            {
                int i = table.getSelectedRow();
                if (i > -1)
                {
                    savePrefs();
                    String pkg = (String) table.getModel().getValueAt(table.convertRowIndexToModel(i), 0);
                    String name = (String) table.getModel().getValueAt(table.convertRowIndexToModel(i), 1);
                    tblMethods.setModel(currentLoader.getMethodModel(pkg + "/" + name));
                    tblMethods.setEnabled(true);
                    tblFields.setModel(currentLoader.getFieldModel(pkg + "/" + name));
                    tblFields.setEnabled(true);
                    tblParams.setModel(paramsDefaultModel);
                    tblParams.setEnabled(true);
                    new TableColumnAdjuster(tblMethods).adjustColumns();
                    new TableColumnAdjuster(tblFields).adjustColumns();
                    loadPrefs(true);
                }
                else
                {
                    tblMethods.setModel(methodsDefaultModel);
                    tblMethods.setEnabled(false);
                    tblFields.setModel(fieldsDefaultModel);
                    tblFields.setEnabled(false);
                    tblParams.setModel(paramsDefaultModel);
                    tblParams.setEnabled(false);
                }
            }
        }
    }

    class MethodTableSelectionListener implements ListSelectionListener
    {
        private final JTable table;

        public MethodTableSelectionListener(JTable table)
        {
            this.table = table;
        }

        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (!e.getValueIsAdjusting() && !table.getModel().equals(methodsDefaultModel))
            {
                int i = table.getSelectedRow();
                if (i > -1)
                {
                    savePrefs();
                    String name = (String) table.getModel().getValueAt(table.convertRowIndexToModel(i), 1);
                    tblParams.setModel(currentLoader.getParamModel(name));
                    tblParams.setEnabled(true);
                    new TableColumnAdjuster(tblParams).adjustColumns();
                    loadPrefs(true);
                }
                else
                {
                    tblParams.setModel(paramsDefaultModel);
                    tblParams.setEnabled(false);
                }
            }
        }
    }

    class SearchActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (curTask != null && curTask.isAlive() || cmbFilter.getItemCount() == 0)
                return;

            String filterText = cmbFilter.getSelectedItem().toString();

            if (filterText != null && !filterText.trim().isEmpty())
            {
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) cmbFilter.getModel();

                if (model.getIndexOf(filterText) != -1)
                    model.removeElement(filterText);

                cmbFilter.insertItemAt(filterText, 0);
                cmbFilter.setSelectedItem(filterText);
            }

            savePrefs();

            cmbFilter.setEnabled(false);
            btnSearch.setEnabled(false);
            pnlProgress.setVisible(true);
            tblClasses.setModel(classesDefaultModel);
            tblClasses.setEnabled(false);
            tblMethods.setModel(methodsDefaultModel);
            tblMethods.setEnabled(false);
            tblParams.setModel(paramsDefaultModel);
            tblParams.setEnabled(false);
            tblFields.setModel(fieldsDefaultModel);
            tblFields.setEnabled(false);

            loadPrefs(true);

            curTask = new Thread()
            {
                @Override
                public void run()
                {
                    boolean crashed = false;

                    try
                    {
                        IProgressListener progress = new IProgressListener()
                        {
                            private String currentText;

                            @Override
                            public void start(final int max, final String text)
                            {
                                currentText = text.equals("") ? " " : text;
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setString(currentText);
                                        if (max >= 0)
                                            progressBar.setMaximum(max);
                                        progressBar.setValue(0);
                                    }
                                });
                            }

                            @Override
                            public void set(final int value)
                            {
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setValue(value);
                                    }
                                });
                            }

                            @Override
                            public void set(final int value, final String text)
                            {
                                currentText = text.equals("") ? " " : text;
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setValue(value);
                                        progressBar.setString(currentText);
                                    }
                                });
                            }

                            @Override
                            public void setMax(final int max)
                            {
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setMaximum(max);
                                    }
                                });
                            }
                        };

                        progress.start(0, "正在搜索与输入内容匹配的MCP对象");
                        tblClasses.setModel(currentLoader.getSearchResults(cmbFilter.getSelectedItem().toString(), progress));
                        tblClasses.setEnabled(true);
                        new TableColumnAdjuster(tblClasses).adjustColumns();
                        loadPrefs(true);

                        if (tblClasses.getRowCount() > 0)
                        {
                            String pkg = (String) tblClasses.getModel().getValueAt(0, 0);
                            String name = (String) tblClasses.getModel().getValueAt(0, 1);
                            tblMethods.setModel(currentLoader.getMethodModel(pkg + "/" + name));
                            tblMethods.setEnabled(true);
                            tblFields.setModel(currentLoader.getFieldModel(pkg + "/" + name));
                            tblFields.setEnabled(true);
                            tblParams.setModel(paramsDefaultModel);
                            tblParams.setEnabled(true);
                            new TableColumnAdjuster(tblMethods).adjustColumns();
                            new TableColumnAdjuster(tblFields).adjustColumns();
                            loadPrefs(true);

                            if (cmbFilter.getSelectedItem().toString().trim().startsWith("field") && tblFields.getRowCount() > 0)
                            {
                                for (int i = 0; i < tblFields.getRowCount(); i++)
                                {
                                    if (((String) tblFields.getModel().getValueAt(i, 1)).contains(cmbFilter.getSelectedItem().toString()))
                                    {
                                        final int rowIndex = i;
                                        tblFields.setRowSelectionInterval(rowIndex, rowIndex);
                                        tblFields.setColumnSelectionInterval(1, 1);
                                        tblFields.requestFocus();

                                        SwingUtilities.invokeLater(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                tblFields.scrollRectToVisible(tblFields.getCellRect(rowIndex, 0, true));
                                            }
                                        });

                                        break;
                                    }
                                }
                            }
                            else if (cmbFilter.getSelectedItem().toString().trim().startsWith("func") && tblMethods.getRowCount() > 0)
                            {
                                for (int i = 0; i < tblMethods.getRowCount(); i++)
                                {
                                    if (((String) tblMethods.getModel().getValueAt(i, 1)).contains(cmbFilter.getSelectedItem().toString()))
                                    {
                                        final int rowIndex = i;
                                        tblMethods.setRowSelectionInterval(rowIndex, rowIndex);
                                        tblMethods.setColumnSelectionInterval(1, 1);
                                        tblMethods.requestFocus();

                                        SwingUtilities.invokeLater(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                tblMethods.scrollRectToVisible(tblMethods.getCellRect(rowIndex, 0, true));
                                            }
                                        });

                                        break;
                                    }
                                }

                            }
                        }

                        loadPrefs(true);
                    }
                    catch (Exception e)
                    {
                        String s = getStackTraceMessage(
                                "An error has occurred - give bspkrs this stack trace (which has been copied to the clipboard)\n", e);

                        System.err.println(s);

                        crashed = true;

                        final String errMsg = s;
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressBar.setString(" ");
                                progressBar.setValue(0);

                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(errMsg), null);
                                JOptionPane.showMessageDialog(MappingGui.this, errMsg, "MMV - Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    finally
                    {
                        if (!crashed)
                        {
                            SwingUtilities.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    progressBar.setString(" ");
                                    progressBar.setValue(0);
                                    cmbFilter.setEnabled(true);
                                }
                            });
                        }
                        pnlProgress.setVisible(false);
                        cmbFilter.setEnabled(true);
                        btnSearch.setEnabled(true);
                    }
                }
            };

            curTask.start();
        }
    }

    class RefreshActionListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (curTask != null && curTask.isAlive())
                return;

            final String mappingVersion = (String) cmbMappingVersion.getSelectedItem();
            savePrefs();

            pnlFilter.setVisible(false);
            pnlProgress.setVisible(true);
            tblClasses.setModel(classesDefaultModel);
            tblClasses.setEnabled(false);
            tblMethods.setModel(methodsDefaultModel);
            tblMethods.setEnabled(false);
            tblParams.setModel(paramsDefaultModel);
            tblParams.setEnabled(false);
            tblFields.setModel(fieldsDefaultModel);
            tblFields.setEnabled(false);

            loadPrefs(true);

            curTask = new Thread()
            {
                @Override
                public void run()
                {
                    boolean crashed = false;

                    try
                    {
                        IProgressListener progress = new IProgressListener()
                        {
                            private String currentText;

                            @Override
                            public void start(final int max, final String text)
                            {
                                currentText = text.equals("") ? " " : text;
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setString(currentText);
                                        if (max >= 0)
                                            progressBar.setMaximum(max);
                                        progressBar.setValue(0);
                                    }
                                });
                            }

                            @Override
                            public void set(final int value)
                            {
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setValue(value);
                                    }
                                });
                            }

                            @Override
                            public void set(final int value, final String text)
                            {
                                currentText = text.equals("") ? " " : text;
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setValue(value);
                                        progressBar.setString(currentText);
                                    }
                                });
                            }

                            @Override
                            public void setMax(final int max)
                            {
                                SwingUtilities.invokeLater(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressBar.setMaximum(max);
                                    }
                                });
                            }
                        };

                        if (!mcpInstances.containsKey(mappingVersion) || chkForceRefresh.isSelected())
                        {
                            progress.start(0, "读取 MCP 配置");
                            currentLoader = new McpMappingLoader(MappingGui.this, mappingVersion, progress, chkUseMirror.isSelected());
                            mcpInstances.put(mappingVersion, currentLoader);
                            chkForceRefresh.setSelected(false);
                        }
                        else
                            currentLoader = mcpInstances.get(mappingVersion);

                        tblClasses.setModel(currentLoader.getClassModel());
                        tblClasses.setEnabled(true);
                        new TableColumnAdjuster(tblClasses).adjustColumns();
                        loadPrefs(true);
                    }
                    catch (CantLoadMCPMappingException e)
                    {
                        String s = getStackTraceMessage("", e);

                        System.err.println(s);

                        crashed = true;

                        final String errMsg = s;
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressBar.setString(" ");
                                progressBar.setValue(0);

                                JOptionPane.showMessageDialog(MappingGui.this, errMsg, "MMV - Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    catch (Exception e)
                    {
                        String s = getStackTraceMessage(
                                "An error has occurred - give bspkrs this stack trace (which has been copied to the clipboard)\n", e);

                        System.err.println(s);

                        crashed = true;

                        final String errMsg = s;
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                progressBar.setString(" ");
                                progressBar.setValue(0);

                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(errMsg), null);
                                JOptionPane.showMessageDialog(MappingGui.this, errMsg, "MMV - Error", JOptionPane.ERROR_MESSAGE);
                            }
                        });
                    }
                    finally
                    {
                        if (!crashed)
                        {
                            SwingUtilities.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    progressBar.setString(" ");
                                    progressBar.setValue(0);
                                    cmbFilter.setEnabled(true);
                                }
                            });
                        }
                        pnlProgress.setVisible(false);
                        pnlFilter.setVisible(true);
                        cmbFilter.setEnabled(true);
                        btnSearch.setEnabled(true);
                        //                        btnSave.setEnabled(currentLoader.hasPendingEdits());
                        btnGetBotCommands.setEnabled(currentLoader.hasPendingCommands());
                    }
                }
            };

            curTask.start();
        }
    }

    public static void showHTMLDialog(Component parentComponent, Object message, String title, int messageType)
    {
        JLabel label = new JLabel();
        Font font = label.getFont();

        StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
        style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
        style.append("font-size:" + font.getSize() + "pt;");

        JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" + message.toString() + "</body></html>");

        ep.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
                {
                    try
                    {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    }
                    catch (Throwable ignore)
                    {}
                }
            }
        });
        // Dunno why, but if I do this the About dialog no longer gets cut off...
        ep.setSize(100, 100);
        ep.setEditable(false);
        ep.setBackground(label.getBackground());
        JOptionPane.showMessageDialog(parentComponent, ep, title, messageType);
    }
}
