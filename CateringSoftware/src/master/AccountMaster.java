/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package master;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import cateringsoftware.DeskFrame;
import support.Constants;
import support.HeaderIntFrame1;
import support.Library;
import support.PickList;
import support.ReportTable;
import support.SmallNavigation;
import support.VoucherDisplay;

/**
 *
 * @author @JD@
 */
public class AccountMaster extends javax.swing.JInternalFrame {
    private SmallNavigation navLoad;
    private Library lb = new Library();
    public static int mode = 0;
    public String id = "";
    private Connection dataConnection = DeskFrame.connMpAdmin;
    private ReportTable viewTable = null;
    PickList accountTypePickList = null;

    /**
     * Creates new form AccountMaster
     */
    public AccountMaster() {
        initComponents();
        initOtherComponents();
        setVoucher("Last");
    }

    public AccountMaster(String id) {
        initComponents();
        initOtherComponents();
        this.id = id;
        setVoucher("Edit");
    }

    private void initOtherComponents() {
        connectNavigation();
        navLoad.setComponentEnabledDisabled(false);
        addValidation();
        jcmbRs.removeAllItems();
        jcmbRs.addItem(Constants.drName);
        jcmbRs.addItem(Constants.crName);
        accountTypePickList = new PickList(dataConnection);
        setEnableLockDate(true);
        setLockShortcut();
        tableForView();
        setPickListView();
        setTitle(Constants.ACCOUNT_MASTER_FORM_NAME);
    }

    private void setEnableLockDate(boolean flag) {
        jtxtLockDate.setVisible(flag);
        jLabel18.setVisible(flag);
    }

    private void setLockShortcut() {
        KeyStroke lockKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_MASK, false);
        Action lockKeyAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getLockDate();
            }
        };
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(lockKeyStroke, "Lock");
        this.getActionMap().put("Lock", lockKeyAction);
    }

    private void getLockDate() {
        if(!navLoad.getMode().equalsIgnoreCase("")) {
            String lock = JOptionPane.showInputDialog(this, "Enter Lock Date (DD/MM/YYYY) format", jtxtLockDate.getText());
            String current = jtxtLockDate.getText();
            jtxtLockDate.setText(lock);
            if(!lb.checkDate2(jtxtLockDate)) {
                jtxtLockDate.setText(current);
            }
        }
    }

    private void tableForView() {
        viewTable = new ReportTable();

        viewTable.AddColumn(0, "Name", 200, java.lang.String.class, null, false);
        viewTable.AddColumn(1, "Group", 200, java.lang.String.class, null, false);
        viewTable.AddColumn(2, "OPB RS", 70, java.lang.String.class, null, false);
        viewTable.AddColumn(3, "RS", 70, java.lang.String.class, null, false);
        viewTable.makeTable();
    }

    private void setPickListView() {
        accountTypePickList.setLayer(getLayeredPane());
        accountTypePickList.setPickListComponent(jtxtAccountType);
        accountTypePickList.setNextComponent(jtxtOPBRs);
        accountTypePickList.setReturnComponent(new JTextField[]{jtxtAccountType});
    }

    public void setID(String code) {
        id = lb.getAccountCode(code, "C");
        setVoucher("Edit");
    }

    private void onViewVoucher() {
        this.dispose();

        String sql = "SELECT a.name, g.name, CAST(a.opening_rs AS DECIMAL(15,2)) AS opening_rs, CASE WHEN account_effect_rs='0' THEN 'DR' ELSE 'CR' END AS rs FROM account_master a, account_type g WHERE a.fk_account_type_id = g.id";
        viewTable.setColumnValue(new int[]{1, 2, 3, 4});
        String view_title = Constants.ACCOUNT_MASTER_FORM_NAME +" VIEW";

        HeaderIntFrame1 rptDetail = new HeaderIntFrame1(dataConnection, lb.getAccountCode(id, "N"), view_title, viewTable, sql, Constants.ACCOUNT_MASTER_FORM_ID, 1, this, this.getTitle());
        rptDetail.makeView();
        rptDetail.setVisible(true);

        Component c = DeskFrame.tabbedPane.add(view_title, rptDetail);
        c.setName(view_title);
        DeskFrame.tabbedPane.setSelectedComponent(c);
    }

    @Override
    public void dispose() {
        try {
            DeskFrame.removeFromScreen(DeskFrame.tabbedPane.getSelectedIndex());
            super.dispose();
        } catch (Exception ex) {
            lb.printToLogFile("Exception at dispose In Account Master", ex);
        }
    }

    private void addValidation() {
        fldValidation validation = new fldValidation();
        jtxtAccountType.setInputVerifier(validation);
        jtxtAccountName.setInputVerifier(validation);
    }

    public void setId(String id) {
        this.id = id;
        setVoucher("edit");
    }

    class fldValidation extends InputVerifier {
        @Override
        public boolean verify(JComponent input) {
            ((JTextField) input).setText(((JTextField) input).getText().toUpperCase());
            boolean val = fldValid(input);
            return val;
        }
    }

    private boolean fldValid(Component comp) {
        if(!navLoad.getMode().equalsIgnoreCase("")) {
            if (comp == jtxtAccountType) {
                String fldVal = jtxtAccountType.getText();
                if (fldVal.equalsIgnoreCase("")) {
                    navLoad.setMessage("Account Type should not be blank");
                    jtxtAccountType.requestFocusInWindow();
                    return false;
                }
                String code = lb.getAccountType(fldVal, "C");
                if(code.equalsIgnoreCase("0") || code.equalsIgnoreCase("")) {
                    navLoad.setMessage("Account Type is invalid");
                    jtxtAccountType.requestFocusInWindow();
                    return false;
                }
            }
            if (comp == jtxtAccountName) {
                String fldVal = jtxtAccountName.getText();
                if (fldVal.equalsIgnoreCase("")) {
                    navLoad.setMessage("Account Name should not be blank");
                    jtxtAccountName.requestFocusInWindow();
                    return false;
                } else {
                    if (navLoad.getMode().equalsIgnoreCase("E")) {
                        if (lb.isExistForEdit("account_master", "name", jtxtAccountName.getText(), "id", id, dataConnection)) {
                            jtxtAccountName.requestFocusInWindow();
                            navLoad.setMessage("Account Name is already exist!");
                            return false;
                        }
                    } else if (navLoad.getMode().equalsIgnoreCase("N")) {
                        if (lb.isExist("account_master", "name", jtxtAccountName.getText(), dataConnection)) {
                            jtxtAccountName.requestFocusInWindow();
                            navLoad.setMessage("Account Name is already exist!");
                            return false;
                        }
                    }
                }
            }
            if (comp == jtxtGSTNo) {
                String fldVal = jtxtGSTNo.getText();
                if (!fldVal.equalsIgnoreCase("") && fldVal.length() != 15) {
                    navLoad.setMessage("GST No is invalid");
                    jtxtGSTNo.requestFocusInWindow();
                    return false;
                }
            }
        }
        return true;
    }

    private void cancelOrClose() {
        if (navLoad.getSaveFlag()) {
            this.dispose();
        } else {
            accountTypePickList.setVisible(false);
            navLoad.setMode("");
            navLoad.setComponentEnabledDisabled(false);
            navLoad.setMessage("");
            navLoad.setSaveFlag(true);
        }
    }

    private void setVoucher(String tag) {
        try {
            navLoad.setMessage("");
            String sql = "SELECT * FROM account_master";
            if (tag.equalsIgnoreCase("first")) {
                sql += " ORDER BY id";
            } else if (tag.equalsIgnoreCase("previous")) {
                sql += " WHERE id < '"+ id +"' ORDER BY id DESC";
            } else if (tag.equalsIgnoreCase("next")) {
                sql += " WHERE id > '"+ id +"'";
            } else if (tag.equalsIgnoreCase("last")) {
                sql += " ORDER BY id DESC";
            } else if (tag.equalsIgnoreCase("edit")) {
                sql += " WHERE id = '"+ id +"'";
            }
            navLoad.viewData = lb.fetchData(sql, dataConnection);
            if (navLoad.viewData.next()) {
                navLoad.setComponentTextFromResultSet();
            } else {
                if(tag.equalsIgnoreCase("last")) {
                    setComponentText();
                }
            }
            navLoad.setComponentEnabledDisabled(false);
            navLoad.setLastFocus();
            lb.setPermission(navLoad, Constants.ACCOUNT_MASTER_FORM_ID);
        } catch (Exception ex) {
            lb.printToLogFile("Exception at setVoucher In Account Master", ex);
        }
    }

    private int saveVoucher() {
        int i = 0;
        String sql = "";
        try {
            if (navLoad.getMode().equalsIgnoreCase("N")) {
                sql = "INSERT INTO account_master(name, fk_account_type_id, account_effect_rs, opening_rs, maximum_rs, minimum_rs, "+
                    "lock_date, mobile_no1, phone_no1, fax_no, email_id, address1, address2, contact_prsn, refby, shortname, "+
                    "gst_no, pan_no, user_cd, edit_no, id) "+
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)";
                id = lb.generateKey("account_master", "id", Constants.ACCOUNT_MASTER_INITIAL, 7);
                dataConnection.setAutoCommit(false);
            } else if (navLoad.getMode().equalsIgnoreCase("E")) {
                dataConnection.setAutoCommit(false);
                sql = "UPDATE account_master SET name = ?, fk_account_type_id = ?, account_effect_rs = ?, opening_rs = ?, "+
                    "maximum_rs = ?, minimum_rs = ?, lock_date = ?, mobile_no1 = ?, phone_no1 = ?, fax_no = ?, email_id = ?, "+
                    "address1 = ?, address2 = ?, contact_prsn = ?, refby = ?, shortname = ?, gst_no = ?, pan_no = ?, user_cd = ?, "+
                    "edit_no = edit_no + 1, time_stamp = CURRENT_TIMESTAMP WHERE id = ?";
            }
            PreparedStatement pstLocal = dataConnection.prepareStatement(sql);
            pstLocal.setString(1, jtxtAccountName.getText().toUpperCase()); // name
            pstLocal.setString(2, lb.getAccountType(jtxtAccountType.getText(), "c")); // fk_account_type_id
            pstLocal.setString(3, jcmbRs.getSelectedIndex() + ""); // fk_account_type_id
            pstLocal.setDouble(4, lb.replaceAll(jtxtOPBRs.getText())); // opening_rs
            pstLocal.setDouble(5, lb.replaceAll(jtxtMaxRs.getText())); // maximum_rs
            pstLocal.setDouble(6, lb.replaceAll(jtxtMinRs.getText())); // minimum_rs
            pstLocal.setString(7, lb.ConvertDateFormetForDB(jtxtLockDate.getText())); // lock_date
            pstLocal.setString(8, jtxtMobile.getText()); // mobile_no1
            pstLocal.setString(9, jtxtPhone.getText()); // phone_no1
            pstLocal.setString(10, jtxtFaxNo.getText()); // fax_no
            pstLocal.setString(11, jtxtEmail1.getText()); // email_id
            pstLocal.setString(12, jtxtAddress1.getText()); // address1
            pstLocal.setString(13, jtxtAddress2.getText()); // address2
            pstLocal.setString(14, jtxtContactName.getText()); // contact_prsn
            pstLocal.setString(15, jtxtReferenceName.getText()); // refby
            pstLocal.setString(16, jtxtShortName.getText()); // shortname
            pstLocal.setString(17, jtxtGSTNo.getText()); // gst_no
            pstLocal.setString(18, jtxtPanNo.getText()); // pan_no
            pstLocal.setInt(19, DeskFrame.user_id); // user_cd
            pstLocal.setString(20, id); // id
            i = pstLocal.executeUpdate();
            if (pstLocal != null) {
                pstLocal.close();
            }
            dataConnection.commit();
            dataConnection.setAutoCommit(true);
        } catch (Exception ex) {
            i = 0;
            lb.printToLogFile("Exception at saveVoucher In Account Master", ex);
            try {
                dataConnection.rollback();
                dataConnection.setAutoCommit(true);
            } catch (Exception ex1) {
                lb.printToLogFile("exception at rollback saveVoucher In Account Master", ex1);
            }
        }
        return i;
    }

    private boolean validateForm() {
        boolean flag = true;
        flag = flag && fldValid(jtxtAccountName);
        flag = flag && fldValid(jtxtAccountType);
        flag = flag && fldValid(jtxtGSTNo);

        return flag;
    }

    private void setComponentText() {
        jtxtAccountCD.setText("");
        jtxtAccountName.setText("");
        jtxtAccountType.setText("");
        jtxtOPBRs.setText("0.00");
        jtxtMaxRs.setText("0.00");
        jtxtMinRs.setText("0.00");
        jtxtReferenceName.setText("");
        jtxtShortName.setText("");
        jtxtContactName.setText("");
        jtxtAddress1.setText("");
        jtxtAddress2.setText("");
        jtxtEmail1.setText("");
        jtxtFaxNo.setText("");
        jtxtPincode.setText("");
        jtxtMobile.setText("");
        jtxtPhone.setText("");
        jtxtLockDate.setText("");
        jtxtGSTNo.setText("");
        jtxtPanNo.setText("");
        lb.setDateChooserProperty(jtxtLockDate);
    }

    private void connectNavigation() {
        class smallNavigation extends SmallNavigation {
            @Override
            public void callNew() {
                setComponentEnabledDisabled(true);
                setComponentText();
                setSaveFlag(false);
                setMode("N");
                jtxtAccountName.requestFocusInWindow();
            }

            @Override
            public void callEdit() {
                setComponentEnabledDisabled(true);
                setSaveFlag(false);
                setMode("E");
                jtxtAccountName.requestFocusInWindow();
            }

            @Override
            public void callSave() {
                if(validateForm()) {
                    int i = saveVoucher();
                    if(i != 0) {
                        accountTypePickList.setVisible(false);
                        navLoad.setComponentEnabledDisabled(false);
                        navLoad.setMessage("");
                        navLoad.setSaveFlag(true);
                        if(getMode().equalsIgnoreCase("N")) {
                            navLoad.setMode("");
                            setVoucher("Last");
                        } else {
                            navLoad.setMode("");
                            setVoucher("Edit");
                        }
                    } else {
                        jtxtAccountName.requestFocusInWindow();
                    }
                }
            }

            @Override
            public void callDelete() {
                lb.confirmDialog(Constants.DELETE_RECORD);
                if (lb.type) {
                    try {
                        dataConnection.setAutoCommit(false);
                        String sql = "DELETE FROM account_master WHERE id = ?";
                        PreparedStatement pstLocal = dataConnection.prepareStatement(sql);
                        pstLocal.setString(1, id);
                        pstLocal.executeUpdate();

                        setVoucher("Previous");
                        dataConnection.commit();
                        dataConnection.setAutoCommit(true);
                    } catch (Exception ex) {
                        lb.printToLogFile("Exception at callDelete In Account Master", ex);
                        try {
                            dataConnection.rollback();
                            dataConnection.setAutoCommit(true);
                        } catch (Exception ex1) {
                            lb.printToLogFile("Exception at rollback callDelete In Account Master", ex1);
                        }
                    }
                } else {
                    navLoad.setSaveFocus();
                }
            }

            @Override
            public void callView() {
                onViewVoucher();
            }

            @Override
            public void callFirst() {
                setVoucher("First");
            }

            @Override
            public void callPrevious() {
                setVoucher("Previous");
            }

            @Override
            public void callNext() {
                setVoucher("Next");
            }

            @Override
            public void callLast() {
                setVoucher("Last");
            }

            @Override
            public void callClose() {
                cancelOrClose();
                setVoucher("Edit");
            }

            @Override
            public void callPrint() {
                try {
                    VoucherDisplay vd = new VoucherDisplay(id, ""+ Constants.ACCOUNT_MASTER_INITIAL +"M");
                    DeskFrame.addOnScreen(vd, Constants.ACCOUNT_MASTER_FORM_NAME +" PRINT");
                } catch(Exception ex) {
                    lb.printToLogFile("Exception at callPrint In Account Master", ex);
                }
            }

            @Override
            public void setComponentTextFromResultSet() {
                try {
                    id = viewData.getString("id");
                    jtxtAccountName.setText(viewData.getString("name"));
                    jtxtAccountCD.setText(id);
                    jtxtAccountType.setText(lb.getAccountType(viewData.getString("fk_account_type_id"), "N"));
                    jtxtOPBRs.setText(lb.getIndianFormat(viewData.getDouble("opening_rs")));
                    jtxtMaxRs.setText(lb.getIndianFormat(viewData.getDouble("maximum_rs")));
                    jtxtMinRs.setText(lb.getIndianFormat(viewData.getDouble("minimum_rs")));
                    jcmbRs.setSelectedIndex(viewData.getInt("account_effect_rs"));
                    jlblEditNo.setText(viewData.getString("edit_no"));
                    jlblUserName.setText(lb.getUserName(viewData.getString("user_cd"), "N"));
                    if (viewData.getString("lock_date") != null) {
                        jtxtLockDate.setText(lb.ConvertDateFormetForDBForConcurrency(viewData.getString("lock_date")));
                    } else {
                        lb.setDateChooserProperty(jtxtLockDate);
                    }

                    jtxtContactName.setText(viewData.getString("contact_prsn"));
                    jtxtAddress1.setText(viewData.getString("address1"));
                    jtxtAddress2.setText(viewData.getString("address2"));
                    jtxtMobile.setText(viewData.getString("mobile_no1"));
                    jtxtPhone.setText(viewData.getString("phone_no1"));
                    jtxtFaxNo.setText(viewData.getString("fax_no"));
                    jtxtEmail1.setText(viewData.getString("email_id"));
                    jtxtReferenceName.setText(viewData.getString("refby"));
                    jtxtShortName.setText(viewData.getString("shortname"));
                    jtxtGSTNo.setText(viewData.getString("gst_no"));
                    jtxtPanNo.setText(viewData.getString("pan_no"));
                } catch (Exception ex) {
                    lb.printToLogFile("Exception at setComponentTextFromResultSet In Account Master", ex);
                }
            }

            @Override
            public void setComponentEnabledDisabled(boolean flag) {
                jtxtAccountCD.setEnabled(!flag);
                jtxtAccountName.setEnabled(flag);
                jtxtAccountType.setEnabled(flag);
                jtxtOPBRs.setEnabled(flag);
                jtxtMaxRs.setEnabled(flag);
                jtxtMinRs.setEnabled(flag);
                jcmbRs.setEnabled(flag);
                jtxtReferenceName.setEnabled(flag);
                jtxtContactName.setEnabled(flag);
                jtxtAddress1.setEnabled(flag);
                jtxtAddress2.setEnabled(flag);
                jtxtEmail1.setEnabled(flag);
                jtxtPincode.setEnabled(flag);
                jtxtMobile.setEnabled(flag);
                jtxtPhone.setEnabled(flag);
                jtxtFaxNo.setEnabled(flag);
                jtxtShortName.setEnabled(flag);
                jtxtGSTNo.setEnabled(flag);
                jtxtPanNo.setEnabled(flag);
                jtxtLockDate.setEnabled(flag);
            }
        }
        navLoad = new smallNavigation();
        jpanelNavigation.add(navLoad);
        navLoad.setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpanelNavigation = new javax.swing.JPanel();
        jPanelContact = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jtxtContactName = new javax.swing.JTextField();
        jtxtAddress1 = new javax.swing.JTextField();
        jtxtAddress2 = new javax.swing.JTextField();
        jtxtPincode = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        jtxtMobile = new javax.swing.JTextField();
        jtxtEmail1 = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        jtxtPhone = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        jtxtFaxNo = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        jtxtGSTNo = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        jtxtPanNo = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jtxtAccountCD = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jcmbRs = new javax.swing.JComboBox();
        jtxtAccountName = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jtxtOPBRs = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jtxtAccountType = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        jtxtReferenceName = new javax.swing.JTextField();
        jLabel31 = new javax.swing.JLabel();
        jtxtShortName = new javax.swing.JTextField();
        jtxtMaxRs = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jtxtMinRs = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jlblUserName = new javax.swing.JLabel();
        jtxtLockDate = new javax.swing.JTextField();
        jlblEditNo = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();

        setClosable(true);
        setIconifiable(true);
        setResizable(true);
        setPreferredSize(new java.awt.Dimension(1000, 644));

        jpanelNavigation.setBackground(new java.awt.Color(253, 243, 243));
        jpanelNavigation.setBorder(javax.swing.BorderFactory.createMatteBorder(3, 3, 1, 1, new java.awt.Color(235, 35, 35)));
        jpanelNavigation.setLayout(new java.awt.BorderLayout());

        jPanelContact.setBackground(new java.awt.Color(215, 227, 208));
        jPanelContact.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 3, 3, new java.awt.Color(53, 154, 141)), "Contact Details", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Arial", 2, 16), new java.awt.Color(235, 35, 35))); // NOI18N

        jLabel20.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel20.setText("Pincode");

        jLabel23.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel23.setText("Address");

        jLabel24.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel24.setText("Contact Person");

        jtxtContactName.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtContactName.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtContactName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtContactNameFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtContactNameFocusLost(evt);
            }
        });
        jtxtContactName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtContactNameKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtContactNameKeyTyped(evt);
            }
        });

        jtxtAddress1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtAddress1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtAddress1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtAddress1FocusGained(evt);
            }
        });
        jtxtAddress1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtAddress1KeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtAddress1KeyTyped(evt);
            }
        });

        jtxtAddress2.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtAddress2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtAddress2.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtAddress2FocusGained(evt);
            }
        });
        jtxtAddress2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtAddress2KeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtAddress2KeyTyped(evt);
            }
        });

        jtxtPincode.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtPincode.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtPincode.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtPincodeFocusGained(evt);
            }
        });
        jtxtPincode.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtPincodeKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtPincodeKeyTyped(evt);
            }
        });

        jLabel25.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel25.setText("Email");

        jLabel26.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel26.setText("Mobile");

        jtxtMobile.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtMobile.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtMobile.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtMobileFocusGained(evt);
            }
        });
        jtxtMobile.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtMobileKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtMobileKeyTyped(evt);
            }
        });

        jtxtEmail1.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtEmail1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtEmail1.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtEmail1FocusGained(evt);
            }
        });
        jtxtEmail1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtEmail1KeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtEmail1KeyTyped(evt);
            }
        });

        jLabel27.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel27.setText("Phone");

        jtxtPhone.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtPhone.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtPhone.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtPhoneFocusGained(evt);
            }
        });
        jtxtPhone.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtPhoneKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtPhoneKeyTyped(evt);
            }
        });

        jLabel28.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel28.setText("Fax No");

        jtxtFaxNo.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtFaxNo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtFaxNo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtFaxNoFocusGained(evt);
            }
        });
        jtxtFaxNo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtFaxNoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtFaxNoKeyTyped(evt);
            }
        });

        jLabel37.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel37.setText("GST No");

        jtxtGSTNo.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtGSTNo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtGSTNo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtGSTNoFocusGained(evt);
            }
        });
        jtxtGSTNo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtGSTNoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtGSTNoKeyTyped(evt);
            }
        });

        jLabel38.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel38.setText("Pan No.");

        jtxtPanNo.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtPanNo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtPanNo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtPanNoFocusGained(evt);
            }
        });
        jtxtPanNo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtPanNoKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtPanNoKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout jPanelContactLayout = new javax.swing.GroupLayout(jPanelContact);
        jPanelContact.setLayout(jPanelContactLayout);
        jPanelContactLayout.setHorizontalGroup(
            jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelContactLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jLabel25, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel23, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel37, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtContactName, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtAddress1, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtAddress2, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmail1, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtGSTNo, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelContactLayout.createSequentialGroup()
                        .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtxtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelContactLayout.createSequentialGroup()
                        .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel38, javax.swing.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
                            .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jtxtFaxNo, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jtxtPanNo, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanelContactLayout.createSequentialGroup()
                        .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtxtMobile, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelContactLayout.createSequentialGroup()
                        .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtxtPincode, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelContactLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jtxtAddress1, jtxtAddress2, jtxtContactName, jtxtEmail1, jtxtGSTNo});

        jPanelContactLayout.setVerticalGroup(
            jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelContactLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jtxtContactName)
                    .addComponent(jLabel20)
                    .addComponent(jtxtPincode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel23)
                    .addComponent(jtxtAddress1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26)
                    .addComponent(jtxtMobile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtAddress2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel27, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtEmail1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel28)
                    .addComponent(jtxtFaxNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelContactLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel37)
                    .addComponent(jtxtGSTNo, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel38, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtPanNo, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelContactLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel20, jLabel23, jLabel24, jLabel25, jLabel26, jLabel27, jLabel28, jtxtAddress1, jtxtAddress2, jtxtContactName, jtxtEmail1, jtxtFaxNo, jtxtMobile, jtxtPhone, jtxtPincode});

        jPanelContactLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel37, jLabel38, jtxtGSTNo, jtxtPanNo});

        jPanel1.setBackground(new java.awt.Color(253, 243, 243));
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 3, 3, new java.awt.Color(235, 35, 35)), "Account Information", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Arial", 2, 16), new java.awt.Color(0, 0, 255))); // NOI18N

        jtxtAccountCD.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtAccountCD.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtAccountCD.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtAccountCDFocusGained(evt);
            }
        });
        jtxtAccountCD.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtAccountCDKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtAccountCDKeyTyped(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel6.setText("A/C Eff Rs");

        jcmbRs.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jcmbRs.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Dr", "Cr" }));
        jcmbRs.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jcmbRs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jcmbRsKeyPressed(evt);
            }
        });

        jtxtAccountName.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtAccountName.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 0, 0)));
        jtxtAccountName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtAccountNameFocusLost(evt);
            }
        });
        jtxtAccountName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtAccountNameKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtAccountNameKeyTyped(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel8.setText("Account Code");

        jLabel3.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 0, 0));
        jLabel3.setText("Account Name");

        jLabel4.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel4.setText("OPB Rs");

        jtxtOPBRs.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtOPBRs.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtOPBRs.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtOPBRsFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtOPBRsFocusLost(evt);
            }
        });
        jtxtOPBRs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtOPBRsKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtOPBRsKeyTyped(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 0, 0));
        jLabel1.setText("Account Type");

        jtxtAccountType.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtAccountType.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 0, 0)));
        jtxtAccountType.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtAccountTypeFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtAccountTypeFocusLost(evt);
            }
        });
        jtxtAccountType.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtAccountTypeKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jtxtAccountTypeKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtAccountTypeKeyTyped(evt);
            }
        });

        jLabel30.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel30.setText("Ref. Name");

        jtxtReferenceName.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtReferenceName.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtReferenceName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtReferenceNameFocusGained(evt);
            }
        });
        jtxtReferenceName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtReferenceNameKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtReferenceNameKeyTyped(evt);
            }
        });

        jLabel31.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel31.setText("Short Name");

        jtxtShortName.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtShortName.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtShortName.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtShortNameFocusGained(evt);
            }
        });
        jtxtShortName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtShortNameKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtShortNameKeyTyped(evt);
            }
        });

        jtxtMaxRs.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtMaxRs.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtMaxRs.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtMaxRsFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtMaxRsFocusLost(evt);
            }
        });
        jtxtMaxRs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtMaxRsKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtMaxRsKeyTyped(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel5.setText("Max Rs");

        jLabel7.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel7.setText("Min Rs");

        jtxtMinRs.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        jtxtMinRs.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(4, 110, 152)));
        jtxtMinRs.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtMinRsFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtMinRsFocusLost(evt);
            }
        });
        jtxtMinRs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtMinRsKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jtxtMinRsKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(130, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel30, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtMaxRs, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtOPBRs, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtAccountName, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtAccountCD, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtReferenceName, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 72, Short.MAX_VALUE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
                    .addComponent(jLabel31, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jtxtAccountType, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jcmbRs, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jtxtMinRs, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jtxtShortName, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(130, Short.MAX_VALUE))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel1, jLabel3, jLabel30, jLabel4, jLabel5, jLabel6, jLabel7, jLabel8});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jtxtAccountCD, jtxtAccountName, jtxtMaxRs, jtxtOPBRs, jtxtReferenceName});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtAccountCD, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
                    .addComponent(jtxtAccountType, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jtxtAccountName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jcmbRs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtOPBRs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtMinRs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jtxtMaxRs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtShortName, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel31, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jtxtReferenceName, javax.swing.GroupLayout.DEFAULT_SIZE, 22, Short.MAX_VALUE)
                    .addComponent(jLabel30, javax.swing.GroupLayout.DEFAULT_SIZE, 21, Short.MAX_VALUE))
                .addGap(7, 7, 7))
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel1, jtxtAccountType});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel3, jtxtAccountName});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel30, jLabel4, jtxtOPBRs, jtxtReferenceName});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel31, jLabel6, jcmbRs});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel8, jtxtAccountCD});

        jPanel1Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel5, jLabel7, jtxtMaxRs, jtxtMinRs});

        jLabel9.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel9.setText("Edit No:");

        jLabel18.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(255, 0, 0));
        jLabel18.setText("Lock Date");

        jlblUserName.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jlblUserName.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jtxtLockDate.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jtxtLockDate.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(255, 0, 0)));
        jtxtLockDate.setMinimumSize(new java.awt.Dimension(2, 20));
        jtxtLockDate.setPreferredSize(new java.awt.Dimension(2, 20));
        jtxtLockDate.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtLockDateFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtLockDateFocusLost(evt);
            }
        });
        jtxtLockDate.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtLockDateKeyPressed(evt);
            }
        });

        jlblEditNo.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jlblEditNo.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel10.setFont(new java.awt.Font("SansSerif", 0, 14)); // NOI18N
        jLabel10.setText("User Name:");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlblUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jlblEditNo, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jtxtLockDate, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jtxtLockDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jlblEditNo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel10, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jlblUserName, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(7, 7, 7))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel18, jtxtLockDate});

        jPanel2Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jLabel9, jlblEditNo, jlblUserName});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jpanelNavigation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelContact, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelContact, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jpanelNavigation, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void jtxtAccountTypeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountTypeKeyReleased
    try {
        PreparedStatement pstLocal = dataConnection.prepareStatement("SELECT name FROM account_type WHERE name LIKE '%" + jtxtAccountType.getText().toUpperCase() + "%'");
        accountTypePickList.setPreparedStatement(pstLocal);
        accountTypePickList.setValidation(dataConnection.prepareStatement("SELECT name FROM account_type WHERE name = ?"));
        accountTypePickList.pickListKeyRelease(evt);
    } catch (Exception ex) {
        lb.printToLogFile("Exception at jtxtAccountTypeKeyReleased In Account Master", ex);
    }
}//GEN-LAST:event_jtxtAccountTypeKeyReleased

    private void jtxtOPBRsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtOPBRsFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtOPBRsFocusGained

    private void jtxtAccountTypeFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtAccountTypeFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtAccountTypeFocusGained

    private void jtxtOPBRsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtOPBRsFocusLost
        if (jtxtOPBRs.getText().trim().equalsIgnoreCase("")) {
            jtxtOPBRs.setText("0");
        }
    }//GEN-LAST:event_jtxtOPBRsFocusLost

    private void jtxtAccountNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtAccountNameFocusLost
        if (lb.checkAccountName(jtxtAccountName.getText(), id)) {
            jtxtAccountName.setText(jtxtAccountName.getText().toUpperCase());
            navLoad.setMessage("");
        } else {
            navLoad.setMessage("Account Name already exist!");
            jtxtAccountName.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtAccountNameFocusLost

    private void jtxtAccountTypeFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtAccountTypeFocusLost
        accountTypePickList.setVisible(false);
    }//GEN-LAST:event_jtxtAccountTypeFocusLost

    private void jtxtAccountNameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountNameKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            evt.consume();
            jtxtAccountType.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtAccountNameKeyPressed

    private void jtxtAccountCDFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtAccountCDFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtAccountCDFocusGained

    private void jtxtOPBRsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtOPBRsKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            evt.consume();
            jcmbRs.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtOPBRsKeyPressed

    private void jtxtAccountCDKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountCDKeyPressed
        if(navLoad.getMode().equalsIgnoreCase("")) {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                if(lb.isExist("account_master", "id", jtxtAccountCD.getText(), dataConnection)) {
                    id = jtxtAccountCD.getText();
                    navLoad.setMessage("");
                    setVoucher("edit");
                } else {
                    navLoad.setMessage("Account ID is invalid");
                }
            }
            jtxtAccountCD.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtAccountCDKeyPressed

    private void jtxtLockDateFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtLockDateFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtLockDateFocusGained

    private void jtxtLockDateFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtLockDateFocusLost
        lb.setDateUsingJTextField(jtxtLockDate);
    }//GEN-LAST:event_jtxtLockDateFocusLost

    private void jtxtLockDateKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtLockDateKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            evt.consume();
            navLoad.setSaveFocus();
        }
    }//GEN-LAST:event_jtxtLockDateKeyPressed

    private void jtxtOPBRsKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtOPBRsKeyTyped
        lb.onlyNumber(evt, 12);
    }//GEN-LAST:event_jtxtOPBRsKeyTyped

    private void jtxtContactNameFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtContactNameFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtContactNameFocusGained

    private void jtxtContactNameFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtContactNameFocusLost
        if (jtxtContactName.getText().trim().isEmpty()) {
            jtxtContactName.setText(jtxtAccountName.getText());
        }
    }//GEN-LAST:event_jtxtContactNameFocusLost

    private void jtxtContactNameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtContactNameKeyPressed
        lb.enterFocus(evt, jtxtAddress1);
    }//GEN-LAST:event_jtxtContactNameKeyPressed

    private void jtxtAddress1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtAddress1FocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtAddress1FocusGained

    private void jtxtAddress1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAddress1KeyPressed
        lb.enterFocus(evt, jtxtAddress2);
    }//GEN-LAST:event_jtxtAddress1KeyPressed

    private void jtxtAddress2FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtAddress2FocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtAddress2FocusGained

    private void jtxtAddress2KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAddress2KeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            jtxtPincode.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtAddress2KeyPressed

    private void jtxtPincodeFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtPincodeFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtPincodeFocusGained

    private void jtxtPincodeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtPincodeKeyPressed
        lb.enterFocus(evt, jtxtMobile);
    }//GEN-LAST:event_jtxtPincodeKeyPressed

    private void jtxtPincodeKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtPincodeKeyTyped
        lb.onlyNumber(evt, 10);
    }//GEN-LAST:event_jtxtPincodeKeyTyped

    private void jtxtMobileFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtMobileFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtMobileFocusGained

    private void jtxtMobileKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtMobileKeyPressed
        lb.enterFocus(evt, jtxtFaxNo);
    }//GEN-LAST:event_jtxtMobileKeyPressed

    private void jtxtMobileKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtMobileKeyTyped
        lb.onlyNumber(evt, 17);
    }//GEN-LAST:event_jtxtMobileKeyTyped

    private void jtxtEmail1FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtEmail1FocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtEmail1FocusGained

    private void jtxtEmail1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtEmail1KeyPressed
        lb.enterFocus(evt, jtxtMobile);
    }//GEN-LAST:event_jtxtEmail1KeyPressed

    private void jcmbRsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jcmbRsKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            evt.consume();
            jtxtMaxRs.requestFocusInWindow();
        }
    }//GEN-LAST:event_jcmbRsKeyPressed

    private void jtxtPhoneFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtPhoneFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtPhoneFocusGained

    private void jtxtPhoneKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtPhoneKeyPressed
        lb.enterFocus(evt, jtxtGSTNo);
    }//GEN-LAST:event_jtxtPhoneKeyPressed

    private void jtxtPhoneKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtPhoneKeyTyped
        lb.onlyNumber(evt, 17);
    }//GEN-LAST:event_jtxtPhoneKeyTyped

    private void jtxtFaxNoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtFaxNoFocusGained
       lb.selectAll(evt);
    }//GEN-LAST:event_jtxtFaxNoFocusGained

    private void jtxtFaxNoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtFaxNoKeyPressed
        lb.enterFocus(evt, jtxtPhone);
    }//GEN-LAST:event_jtxtFaxNoKeyPressed

    private void jtxtReferenceNameFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtReferenceNameFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtReferenceNameFocusGained

    private void jtxtReferenceNameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtReferenceNameKeyPressed
        lb.enterFocus(evt, jtxtShortName);
    }//GEN-LAST:event_jtxtReferenceNameKeyPressed

    private void jtxtShortNameFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtShortNameFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtShortNameFocusGained

    private void jtxtShortNameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtShortNameKeyPressed
        lb.enterFocus(evt, jtxtContactName);
    }//GEN-LAST:event_jtxtShortNameKeyPressed

    private void jtxtGSTNoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtGSTNoFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtGSTNoFocusGained

    private void jtxtGSTNoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtGSTNoKeyPressed
        lb.enterEvent(evt, jtxtPanNo);
    }//GEN-LAST:event_jtxtGSTNoKeyPressed

    private void jtxtPanNoFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtPanNoFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtPanNoFocusGained

    private void jtxtPanNoKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtPanNoKeyPressed
        lb.enterEvent(evt, jtxtLockDate);
    }//GEN-LAST:event_jtxtPanNoKeyPressed

    private void jtxtPanNoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtPanNoKeyTyped
        lb.fixLength(evt, 10);
    }//GEN-LAST:event_jtxtPanNoKeyTyped

    private void jtxtGSTNoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtGSTNoKeyTyped
        lb.fixLength(evt, 15);
    }//GEN-LAST:event_jtxtGSTNoKeyTyped

    private void jtxtMaxRsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtMaxRsFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtMaxRsFocusGained

    private void jtxtMaxRsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtMaxRsFocusLost
        if (jtxtMaxRs.getText().trim().equalsIgnoreCase("")) {
            jtxtMaxRs.setText("0");
        }
    }//GEN-LAST:event_jtxtMaxRsFocusLost

    private void jtxtMaxRsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtMaxRsKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            evt.consume();
            jtxtMinRs.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtMaxRsKeyPressed

    private void jtxtMaxRsKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtMaxRsKeyTyped
        lb.onlyNumber(evt, 12);
    }//GEN-LAST:event_jtxtMaxRsKeyTyped

    private void jtxtMinRsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtMinRsFocusGained
        lb.selectAll(evt);
    }//GEN-LAST:event_jtxtMinRsFocusGained

    private void jtxtMinRsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtMinRsFocusLost
        if (jtxtMinRs.getText().trim().equalsIgnoreCase("")) {
            jtxtMinRs.setText("0");
        }
    }//GEN-LAST:event_jtxtMinRsFocusLost

    private void jtxtMinRsKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtMinRsKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            evt.consume();
            jtxtReferenceName.requestFocusInWindow();
        }
    }//GEN-LAST:event_jtxtMinRsKeyPressed

    private void jtxtMinRsKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtMinRsKeyTyped
        lb.onlyNumber(evt, 12);
    }//GEN-LAST:event_jtxtMinRsKeyTyped

    private void jtxtAccountCDKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountCDKeyTyped
        lb.fixLength(evt, 7);
    }//GEN-LAST:event_jtxtAccountCDKeyTyped

    private void jtxtAccountNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountNameKeyTyped
        lb.fixLength(evt, 255);
    }//GEN-LAST:event_jtxtAccountNameKeyTyped

    private void jtxtAccountTypeKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountTypeKeyTyped
        lb.fixLength(evt, 50);
    }//GEN-LAST:event_jtxtAccountTypeKeyTyped

    private void jtxtReferenceNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtReferenceNameKeyTyped
        lb.fixLength(evt, 100);
    }//GEN-LAST:event_jtxtReferenceNameKeyTyped

    private void jtxtShortNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtShortNameKeyTyped
        lb.fixLength(evt, 5);
    }//GEN-LAST:event_jtxtShortNameKeyTyped

    private void jtxtContactNameKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtContactNameKeyTyped
        lb.fixLength(evt, 100);
    }//GEN-LAST:event_jtxtContactNameKeyTyped

    private void jtxtAddress1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAddress1KeyTyped
        lb.fixLength(evt, 255);
    }//GEN-LAST:event_jtxtAddress1KeyTyped

    private void jtxtAddress2KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAddress2KeyTyped
        lb.fixLength(evt, 255);
    }//GEN-LAST:event_jtxtAddress2KeyTyped

    private void jtxtEmail1KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtEmail1KeyTyped
        lb.fixLength(evt, 100);
    }//GEN-LAST:event_jtxtEmail1KeyTyped

    private void jtxtFaxNoKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtFaxNoKeyTyped
        lb.fixLength(evt, 17);
    }//GEN-LAST:event_jtxtFaxNoKeyTyped

    private void jtxtAccountTypeKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtAccountTypeKeyPressed
        accountTypePickList.setLocation(jtxtAccountType.getX() + jPanel1.getX(), jPanel1.getY() + jtxtAccountType.getY() + jtxtAccountType.getHeight());
        accountTypePickList.pickListKeyPress(evt);
    }//GEN-LAST:event_jtxtAccountTypeKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanelContact;
    private javax.swing.JComboBox jcmbRs;
    private javax.swing.JLabel jlblEditNo;
    private javax.swing.JLabel jlblUserName;
    private javax.swing.JPanel jpanelNavigation;
    private javax.swing.JTextField jtxtAccountCD;
    private javax.swing.JTextField jtxtAccountName;
    private javax.swing.JTextField jtxtAccountType;
    private javax.swing.JTextField jtxtAddress1;
    private javax.swing.JTextField jtxtAddress2;
    private javax.swing.JTextField jtxtContactName;
    private javax.swing.JTextField jtxtEmail1;
    private javax.swing.JTextField jtxtFaxNo;
    private javax.swing.JTextField jtxtGSTNo;
    private javax.swing.JTextField jtxtLockDate;
    private javax.swing.JTextField jtxtMaxRs;
    private javax.swing.JTextField jtxtMinRs;
    private javax.swing.JTextField jtxtMobile;
    private javax.swing.JTextField jtxtOPBRs;
    private javax.swing.JTextField jtxtPanNo;
    private javax.swing.JTextField jtxtPhone;
    public javax.swing.JTextField jtxtPincode;
    private javax.swing.JTextField jtxtReferenceName;
    private javax.swing.JTextField jtxtShortName;
    // End of variables declaration//GEN-END:variables
}