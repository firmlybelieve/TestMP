/*
 * TestMP (Test Management Platform)
 * Copyright 2013 and beyond, Zhaowei Ding.
 *
 * TestMP is free software; you can redistribute it and/or modify it
 * under the terms of the MIT License (MIT).
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

package org.testmp.webconsole.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testmp.webconsole.client.FilterWindow.FilterType;
import org.testmp.webconsole.client.ReportWindow.ReportType;
import org.testmp.webconsole.shared.ClientConfig;
import org.testmp.webconsole.shared.ClientUtils;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceFloatField;
import com.smartgwt.client.data.fields.DataSourceImageField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.FetchMode;
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.SummaryFunction;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;

public class TestCaseView extends VLayout {

    private Map<String, DataSource> dataSources;

    private ListGrid testCaseGrid;

    private IButton runAutomationButton;

    private AutomationScheduler automationScheduler;

    private HoverCustomizer hoverCustomizer = ClientUtils.createHoverCustomizer();

    private int heartBeat = Integer.parseInt(ClientConfig.constants.heartBeatPerSecond());

    @Override
    protected void onDraw() {
        if (ClientConfig.currentUser == null) {
            testCaseGrid.fetchData();
        } else {
            Criteria criteria = new Criteria("isDefault", "true");
            dataSources.get("testCaseFilterDS").fetchData(criteria, new DSCallback() {

                @Override
                public void execute(DSResponse response, Object rawData, DSRequest request) {
                    if (rawData.toString().isEmpty()) {
                        testCaseGrid.fetchData();
                    } else {
                        JavaScriptObject jsonObj = JsonUtils.safeEval(rawData.toString());
                        AdvancedCriteria initialCriteria = new AdvancedCriteria(jsonObj);
                        ClientConfig.setCurrentFilterCriteria(initialCriteria, FilterType.TEST_CASE);
                        testCaseGrid.fetchData(initialCriteria);
                    }
                }

            });
        }
        super.onDraw();
    }

    @Override
    protected void onInit() {
        super.onInit();

        prepareDataSources();

        automationScheduler = new AutomationScheduler();
        automationScheduler.schedule(heartBeat);

        testCaseGrid = new ListGrid() {
            @Override
            protected Canvas createRecordComponent(final ListGridRecord record, Integer colNum) {

                String fieldName = this.getFieldName(colNum);

                if (!fieldName.equals("runHistory") && !fieldName.equals("robustnessTrend")) {
                    return super.createRecordComponent(record, colNum);
                }

                HLayout recordCanvas = new HLayout();
                recordCanvas.setWidth100();
                recordCanvas.setHeight(22);
                recordCanvas.setAlign(Alignment.CENTER);

                if (fieldName.equals("runHistory")) {
                    if (record.getAttribute("runHistory") == null) {
                        return super.createRecordComponent(record, colNum);
                    }
                    ImgButton runHistoryImg = new ImgButton();
                    runHistoryImg.setShowDown(false);
                    runHistoryImg.setShowRollOver(false);
                    runHistoryImg.setSize("16", "16");
                    runHistoryImg.setLayoutAlign(VerticalAlignment.CENTER);
                    runHistoryImg.setSrc("history.png");
                    runHistoryImg.setPrompt(ClientConfig.messages.viewRunHistory());
                    runHistoryImg.addClickHandler(new ClickHandler() {
                        public void onClick(ClickEvent event) {
                            RunHistoryWindow window = new RunHistoryWindow(record);
                            window.show();
                        }
                    });
                    recordCanvas.addMember(runHistoryImg);
                } else if (fieldName.equals("robustnessTrend")) {
                    if (record.getAttribute("robustnessTrend") == null) {
                        return super.createRecordComponent(record, colNum);
                    }
                    ImgButton robustnessTrendImg = new ImgButton();
                    robustnessTrendImg.setShowDown(false);
                    robustnessTrendImg.setShowRollOver(false);
                    robustnessTrendImg.setSize("16", "16");
                    robustnessTrendImg.setLayoutAlign(VerticalAlignment.CENTER);
                    robustnessTrendImg.setSrc(record.getAttribute("robustnessTrend"));
                    robustnessTrendImg.addClickHandler(new ClickHandler() {
                        @Override
                        public void onClick(ClickEvent event) {
                            String robustnessTrend = record.getAttribute("robustnessTrend");
                            if (robustnessTrend.startsWith("null")) {
                                AutomationCodeWindow window = new AutomationCodeWindow(record);
                                window.show();
                            } else {
                                String automation = record.getAttribute("automation");
                                if (automationScheduler.contains(automation)) {
                                    automationScheduler.cancel(record);
                                } else {
                                    automationScheduler.launch(record);
                                }
                            }
                        }
                    });
                    recordCanvas.addMember(robustnessTrendImg);
                }

                return recordCanvas;
            }

        };

        testCaseGrid.setShowRollOver(false);
        testCaseGrid.setShowRecordComponents(true);
        testCaseGrid.setShowRecordComponentsByCell(true);

        testCaseGrid.setWidth("99%");
        testCaseGrid.setLayoutAlign(Alignment.CENTER);

        testCaseGrid.setDataSource(dataSources.get("testCaseDS"));
        testCaseGrid.setDataFetchMode(FetchMode.BASIC);

        testCaseGrid.setCanRemoveRecords(true);
        testCaseGrid.setWarnOnRemoval(true);

        testCaseGrid.setGroupByField("project");
        testCaseGrid.setGroupStartOpen(GroupStartOpen.ALL);

        testCaseGrid.setShowGridSummary(true);
        testCaseGrid.setShowGroupSummary(true);

        ListGridField projectField = new ListGridField("project", ClientConfig.messages.project());
        ListGridField nameField = new ListGridField("name", ClientConfig.messages.name());
        ListGridField tagsField = new ListGridField("tags", ClientConfig.messages.groups());
        ListGridField descriptionField = new ListGridField("description", ClientConfig.messages.description());
        ListGridField automationField = new ListGridField("automation", ClientConfig.messages.automation());
        ListGridField robustnessField = new ListGridField("robustness", ClientConfig.messages.robustness());
        ListGridField robustnessTrendField = new ListGridField("robustnessTrend", "*");
        ListGridField avgTestTimeField = new ListGridField("avgTestTime", ClientConfig.messages.avgTestTime());
        ListGridField timeVolatilityField = new ListGridField("timeVolatility", ClientConfig.messages.timeVolatility());
        ListGridField runHistoryField = new ListGridField("runHistory", ClientConfig.messages.runHistory());
        ListGridField createTimeField = new ListGridField("createTime", ClientConfig.messages.createTime());
        ListGridField lastModifyTimeField = new ListGridField("lastModifyTime", ClientConfig.messages.lastModifyTime());

        projectField.setHidden(true);
        projectField.setWidth(100);
        projectField.setShowHover(true);
        projectField.setHoverCustomizer(hoverCustomizer);

        nameField.setWidth(200);
        nameField.setShowHover(true);
        nameField.setHoverCustomizer(hoverCustomizer);
        nameField.setSummaryFunction(new SummaryFunction() {

            @Override
            public Object getSummaryValue(Record[] records, ListGridField field) {
                return records.length + " " + ClientConfig.messages.cases();
            }

        });

        tagsField.setWidth(150);
        tagsField.setShowHover(true);
        tagsField.setHoverCustomizer(hoverCustomizer);

        descriptionField.setWidth(225);
        descriptionField.setShowHover(true);
        descriptionField.setHoverCustomizer(hoverCustomizer);

        automationField.setWidth(225);
        automationField.setShowHover(true);
        automationField.setHoverCustomizer(hoverCustomizer);

        runHistoryField.setWidth(80);
        runHistoryField.setShowGridSummary(false);
        runHistoryField.setShowGroupSummary(false);
        runHistoryField.setCellFormatter(new CellFormatter() {

            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return "";
            }

        });

        CellFormatter decimal = new CellFormatter() {

            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return null;
                }

                String v = value.toString();
                int p = v.indexOf(".");
                if (p == -1) {
                    return v + ".000";
                }
                if (p + 4 <= v.length()) {
                    return v.substring(0, p + 4);
                }
                StringBuilder sb = new StringBuilder(v);
                for (int i = 0; i < p + 4 - v.length(); i++) {
                    sb.append('0');
                }
                return sb.toString();
            }

        };

        robustnessField.setWidth(100);
        robustnessField.setType(ListGridFieldType.FLOAT);
        robustnessField.setAlign(Alignment.CENTER);
        robustnessField.setCellFormatter(decimal);
        robustnessField.setShowGridSummary(false);
        robustnessField.setShowGroupSummary(false);

        robustnessTrendField.setWidth(40);
        robustnessTrendField.setAlign(Alignment.CENTER);
        robustnessTrendField.setCellFormatter(new CellFormatter() {

            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                return "";
            }

        });

        avgTestTimeField.setWidth(100);
        avgTestTimeField.setType(ListGridFieldType.FLOAT);
        avgTestTimeField.setAlign(Alignment.RIGHT);
        avgTestTimeField.setCellFormatter(new CellFormatter() {

            @Override
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value == null) {
                    return null;
                }
                return value.toString() + " ms";
            }

        });
        avgTestTimeField.setShowGridSummary(false);
        avgTestTimeField.setShowGroupSummary(false);

        timeVolatilityField.setWidth(100);
        timeVolatilityField.setType(ListGridFieldType.FLOAT);
        timeVolatilityField.setAlign(Alignment.CENTER);
        timeVolatilityField.setCellFormatter(decimal);
        timeVolatilityField.setShowGridSummary(false);
        timeVolatilityField.setShowGroupSummary(false);

        createTimeField.setWidth(150);
        createTimeField.setType(ListGridFieldType.DATE);

        lastModifyTimeField.setWidth(150);
        lastModifyTimeField.setType(ListGridFieldType.DATE);

        testCaseGrid.setFields(projectField, nameField, tagsField, descriptionField, automationField, robustnessField,
                robustnessTrendField, avgTestTimeField, timeVolatilityField, runHistoryField, createTimeField,
                lastModifyTimeField);

        addMember(testCaseGrid);

        HLayout controls = new HLayout();
        controls.setSize("99%", "20");
        controls.setMargin(10);
        controls.setMembersMargin(5);
        controls.setLayoutAlign(Alignment.CENTER);
        addMember(controls);

        HLayout additionalControls = new HLayout();
        additionalControls.setMembersMargin(5);
        controls.addMember(additionalControls);

        HLayout primaryControls = new HLayout();
        primaryControls.setAlign(Alignment.RIGHT);
        primaryControls.setMembersMargin(5);
        controls.addMember(primaryControls);

        IButton foldOrUnfoldButton = new IButton(ClientConfig.messages.foldOrUnfold());
        foldOrUnfoldButton.setIcon("fold.png");
        foldOrUnfoldButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                Tree groupTree = testCaseGrid.getGroupTree();
                if (groupTree != null) {
                    boolean hasOpenFolders = false;
                    for (TreeNode folder : groupTree.getFolders(groupTree.getRoot())) {
                        if (groupTree.isOpen(folder)) {
                            hasOpenFolders = true;
                            break;
                        }
                    }
                    if (hasOpenFolders) {
                        groupTree.closeAll();
                    } else {
                        groupTree.openAll();
                    }
                }
            }

        });
        additionalControls.addMember(foldOrUnfoldButton);

        runAutomationButton = new IButton(ClientConfig.messages.run());
        runAutomationButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                synchronized (automationScheduler) {
                    runOrCancelAutomations();
                }
            }

            private void runOrCancelAutomations() {
                String title = runAutomationButton.getTitle();
                if (title.equals(ClientConfig.messages.run())) {
                    for (Record testCase : testCaseGrid.getRecords()) {
                        String automation = testCase.getAttribute("automation");
                        String robustnessTrend = testCase.getAttribute("robustnessTrend");
                        if (!robustnessTrend.startsWith("null") && !automationScheduler.contains(automation)) {
                            automationScheduler.launch(testCase);
                        }
                    }
                } else {
                    for (Record testCase : testCaseGrid.getRecords()) {
                        String automation = testCase.getAttribute("automation");
                        String robustnessTrend = testCase.getAttribute("robustnessTrend");
                        if (!robustnessTrend.startsWith("null") && automationScheduler.contains(automation)) {
                            automationScheduler.cancel(testCase);
                        }
                    }
                }
            }

        });
        additionalControls.addMember(runAutomationButton);

        IButton newCaseButton = new IButton(ClientConfig.messages.new_());
        newCaseButton.setIcon("newcase.png");
        newCaseButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                NewCaseWindow window = new NewCaseWindow();
                window.show();
            }

        });
        primaryControls.addMember(newCaseButton);

        IButton filterButton = new IButton(ClientConfig.messages.filter());
        filterButton.setIcon("filter.png");
        filterButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (!automationScheduler.isIdle()) {
                    SC.say(ClientConfig.messages.automationIsRunning());
                    return;
                }
                DataSource ds = dataSources.get("testCaseFilterDS");
                FilterWindow window = new FilterWindow(FilterType.TEST_CASE, testCaseGrid, ds);
                window.show();
            }

        });
        primaryControls.addMember(filterButton);

        IButton reloadButton = new IButton(ClientConfig.messages.reload());
        reloadButton.setIcon("reload.png");
        reloadButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (!automationScheduler.isIdle()) {
                    SC.say(ClientConfig.messages.automationIsRunning());
                    return;
                }
                testCaseGrid.invalidateCache();
            }

        });
        primaryControls.addMember(reloadButton);

        IButton reportButton = new IButton(ClientConfig.messages.report());
        reportButton.setIcon("report.png");
        reportButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (!automationScheduler.isIdle()) {
                    SC.say(ClientConfig.messages.automationIsRunning());
                    return;
                }

                Map<String, List<ListGridRecord>> recordsByProject = new LinkedHashMap<String, List<ListGridRecord>>();

                for (ListGridRecord record : testCaseGrid.getRecords()) {
                    String project = record.getAttribute("project");
                    if (!recordsByProject.containsKey(project)) {
                        recordsByProject.put(project, new ArrayList<ListGridRecord>());
                    }
                    recordsByProject.get(project).add(record);
                }

                JSONObject testMetricsObj = new JSONObject();
                for (String project : recordsByProject.keySet()) {
                    TestMetrics t = new TestMetrics(recordsByProject.get(project));
                    testMetricsObj.put(project, t.toJSONObject());
                }

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("action", "create");
                params.put("testMetricsTable", testMetricsObj.toString());
                ReportWindow reportWindow = new ReportWindow();
                reportWindow.showReport(ReportType.TEST_METRICS, params);
            }

        });
        primaryControls.addMember(reportButton);
    }

    private void prepareDataSources() {
        dataSources = new HashMap<String, DataSource>();

        DataSource testCaseSource = ClientUtils
                .createDataSource("testCaseDS", ClientConfig.constants.testCaseService());
        testCaseSource.setFields(getTestCaseFields());
        dataSources.put("testCaseDS", testCaseSource);

        DataSource testCaseFilterSource = ClientUtils.createDataSource("testCaseFilterDS",
                ClientConfig.constants.userService());
        DataSourceTextField userNameField = new DataSourceTextField("userName");
        userNameField.setPrimaryKey(true);
        DataSourceTextField filterNameField = new DataSourceTextField("filterName");
        filterNameField.setPrimaryKey(true);
        DataSourceTextField criteriaField = new DataSourceTextField("criteria");
        DataSourceBooleanField isDefaultField = new DataSourceBooleanField("isDefault");
        testCaseFilterSource.setFields(userNameField, filterNameField, criteriaField, isDefaultField);
        dataSources.put("testCaseFilterDS", testCaseFilterSource);

        DataSource testProjectSource = ClientUtils.createDataSource("testProjectDS",
                ClientConfig.constants.testCaseService());
        DataSourceTextField projectNameField = new DataSourceTextField("project");
        testProjectSource.setFields(projectNameField);
        dataSources.put("testProjectDS", testProjectSource);

        DataSource testResultSource = ClientUtils.createDataSource("testResultDS",
                ClientConfig.constants.testCaseService());
        testResultSource.setFields(getTestCaseFields());
        dataSources.put("testResultDS", testResultSource);

        DataSource testRunSource = ClientUtils.createDataSource("testRunDS", ClientConfig.constants.testCaseService());
        DataSourceTextField userNameField1 = new DataSourceTextField("userName");
        userNameField1.setPrimaryKey(true);
        DataSourceTextField automationField = new DataSourceTextField("automation");
        automationField.setPrimaryKey(true);
        DataSourceBooleanField isRunningField = new DataSourceBooleanField("isRunning");
        testRunSource.setFields(userNameField1, automationField, isRunningField);
        dataSources.put("testRunDS", testRunSource);
    }

    private DataSourceField[] getTestCaseFields() {
        DataSourceIntegerField idField = new DataSourceIntegerField("id");
        idField.setHidden(true);
        idField.setPrimaryKey(true);
        DataSourceTextField projectField = new DataSourceTextField("project", ClientConfig.messages.project());
        projectField.setRequired(true);
        DataSourceTextField nameField = new DataSourceTextField("name", ClientConfig.messages.name());
        nameField.setRequired(true);
        DataSourceTextField tagsField = new DataSourceTextField("tags", ClientConfig.messages.groups());
        DataSourceTextField descriptionField = new DataSourceTextField("description",
                ClientConfig.messages.description(), 500);
        DataSourceTextField automationField = new DataSourceTextField("automation", ClientConfig.messages.automation());
        DataSourceFloatField robustnessField = new DataSourceFloatField("robustness",
                ClientConfig.messages.robustness());
        DataSourceImageField robustnessTrendField = new DataSourceImageField("robustnessTrend",
                ClientConfig.messages.robustnessTrend());
        DataSourceFloatField avgTestTimeField = new DataSourceFloatField("avgTestTime",
                ClientConfig.messages.avgTestTime());
        DataSourceFloatField timeVolatilityField = new DataSourceFloatField("timeVolatility",
                ClientConfig.messages.timeVolatility());
        DataSourceTextField runHistoryField = new DataSourceTextField("runHistory", ClientConfig.messages.runHistory());
        DataSourceTextField createTimeField = new DataSourceTextField("createTime", ClientConfig.messages.createTime());
        DataSourceTextField lastModifyTimeField = new DataSourceTextField("lastModifyTime",
                ClientConfig.messages.lastModifyTime());
        return new DataSourceField[] { idField, projectField, nameField, tagsField, descriptionField, automationField,
                runHistoryField, robustnessField, robustnessTrendField, avgTestTimeField, timeVolatilityField,
                createTimeField, lastModifyTimeField };
    }

    private class AutomationScheduler extends Timer {

        private Map<String, Record> testCases = new HashMap<String, Record>();

        @Override
        public void run() {
            StringBuilder sb = new StringBuilder();
            for (String automation : automations()) {
                sb.append(automation).append(",");
            }

            if (sb.length() > 0) {
                sb.deleteCharAt(sb.length() - 1);
                schedule(sb.toString());
            } else {
                AutomationScheduler.this.schedule(heartBeat);
                return;
            }
        }

        public synchronized boolean contains(String automation) {
            return testCases.containsKey(automation);
        }

        public synchronized boolean isIdle() {
            return testCases.isEmpty();
        }

        public synchronized void launch(final Record testCase) {
            final String automation = testCase.getAttribute("automation");
            if (!testCases.containsKey(automation)) {
                Record data = new Record();
                data.setAttribute("userName", ClientConfig.currentUser);
                data.setAttribute("automation", automation);
                dataSources.get("testRunDS").addData(data, new DSCallback() {

                    @Override
                    public void execute(DSResponse dsResponse, Object data, DSRequest dsRequest) {
                        if (dsResponse.getStatus() != 0) {
                            return;
                        }

                        Record result = dsResponse.getData()[0];
                        if (!result.getAttributeAsBoolean("isRunning")) {
                            return;
                        }

                        synchronized (AutomationScheduler.this) {
                            testCases.put(automation, testCase);
                            int rowNum = testCaseGrid.getRecordIndex(testCase);
                            int colNum = testCaseGrid.getFieldNum("robustnessTrend");
                            Layout recordComp = (Layout) testCaseGrid.getRecordComponent(rowNum, colNum);
                            ImgButton robustnessTrendImg = (ImgButton) recordComp.getMember(0);
                            robustnessTrendImg.setSrc("running.gif");
                            testCaseGrid.refreshRow(rowNum);

                            if (runAutomationButton.getTitle().equals(ClientConfig.messages.run())) {
                                runAutomationButton.setTitle(ClientConfig.messages.cancel());
                            }
                        }
                    }

                });
            }
        }

        public synchronized void cancel(final Record testCase) {
            final String automation = testCase.getAttribute("automation");
            if (testCases.containsKey(automation)) {
                Record data = new Record();
                data.setAttribute("userName", ClientConfig.currentUser);
                data.setAttribute("automation", automation);
                dataSources.get("testRunDS").removeData(data, new DSCallback() {

                    @Override
                    public void execute(DSResponse dsResponse, Object data, DSRequest dsRequest) {
                        if (dsResponse.getStatus() != 0) {
                            return;
                        }

                        Record result = dsResponse.getData()[0];
                        if (result.getAttributeAsBoolean("isRunning")) {
                            return;
                        }

                        finish(testCase);
                    }

                });
            }
        }

        private synchronized String[] automations() {
            return testCases.keySet().toArray(new String[0]);
        }

        private void finish(final Record testCase) {
            Criteria criteria = new Criteria();
            criteria.setAttribute("automation", testCase.getAttribute("automation"));
            dataSources.get("testResultDS").fetchData(criteria, new DSCallback() {

                @Override
                public void execute(DSResponse dsResponse, Object data, DSRequest dsRequest) {
                    if (dsResponse.getStatus() != 0) {
                        return;
                    }

                    synchronized (AutomationScheduler.this) {
                        testCases.remove(testCase.getAttribute("automation"));
                        Record result = dsResponse.getData()[0];
                        int rowNum = testCaseGrid.getRecordIndex(testCase);
                        int colNum = testCaseGrid.getFieldNum("robustnessTrend");
                        Layout recordComp = (Layout) testCaseGrid.getRecordComponent(rowNum, colNum);
                        ImgButton robustnessTrendImg = (ImgButton) recordComp.getMember(0);
                        testCase.setJsObj(result.getJsObj());
                        robustnessTrendImg.setSrc(testCase.getAttribute("robustnessTrend"));
                        testCaseGrid.refreshRow(rowNum);

                        String title = runAutomationButton.getTitle();
                        if (testCases.isEmpty() && title.equals(ClientConfig.messages.cancel())) {
                            runAutomationButton.setTitle(ClientConfig.messages.run());
                        }
                    }
                }

            });
        }

        private void schedule(String automations) {
            Criteria criteria = new Criteria();
            criteria.setAttribute("automation", automations);
            dataSources.get("testRunDS").fetchData(criteria, new DSCallback() {

                @Override
                public void execute(DSResponse dsResponse, Object data, DSRequest dsRequest) {
                    if (dsResponse.getStatus() != 0) {
                        return;
                    }

                    Record[] results = dsResponse.getData();
                    for (Record result : results) {
                        String automation = result.getAttribute("automation");
                        Boolean isRunning = result.getAttributeAsBoolean("isRunning");
                        if (!isRunning) {
                            Record testCase = null;
                            synchronized (AutomationScheduler.this) {
                                testCase = testCases.get(automation);
                            }
                            if (testCase != null) {
                                finish(testCase);
                            }
                        }
                    }

                    AutomationScheduler.this.schedule(heartBeat);
                }

            });
        }
    }

    private class NewCaseWindow extends Window {

        NewCaseWindow() {
            setWidth(400);
            setHeight(300);
            setTitle(ClientConfig.messages.newTestCase());
            ClientUtils.unifySimpleWindowStyle(this);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            addItem(layout);

            final DynamicForm form = new DynamicForm();

            final ComboBoxItem projectItem = new ComboBoxItem("project", ClientConfig.messages.project());
            final TextItem nameItem = new TextItem("name", ClientConfig.messages.name());
            final TextItem tagsItem = new TextItem("tags", ClientConfig.messages.groups());
            final TextAreaItem descriptionItem = new TextAreaItem("description", ClientConfig.messages.description());
            final TextItem automationItem = new TextItem("automation", ClientConfig.messages.automation());

            automationItem.setRequired(true);
            automationItem.setWidth(300);

            projectItem.setWidth(300);
            projectItem.setOptionDataSource(dataSources.get("testProjectDS"));

            nameItem.setWidth(300);
            tagsItem.setWidth(300);
            descriptionItem.setWidth(300);

            form.setItems(projectItem, nameItem, tagsItem, descriptionItem, automationItem);
            form.setWidth("99%");
            layout.addMember(form);

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);
            layout.addMember(controls);

            IButton okButton = new IButton(ClientConfig.messages.ok());
            okButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    if (form.validate()) {
                        ListGridRecord record = new ListGridRecord();
                        record.setAttribute("automation", automationItem.getValueAsString());
                        record.setAttribute("project", projectItem.getValueAsString());
                        record.setAttribute("name", nameItem.getValueAsString());
                        record.setAttribute("description", descriptionItem.getValueAsString());
                        record.setAttribute("tags", tagsItem.getValueAsString());
                        testCaseGrid.addData(record, new DSCallback() {

                            @Override
                            public void execute(DSResponse response, Object rawData, DSRequest request) {
                                if (response.getStatus() == DSResponse.STATUS_SUCCESS) {
                                    NewCaseWindow.this.destroy();
                                }
                            }

                        });
                    }
                }

            });
            controls.addMember(okButton);

            IButton cancelButton = new IButton(ClientConfig.messages.cancel());
            cancelButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    NewCaseWindow.this.destroy();
                }

            });
            controls.addMember(cancelButton);
        }

    }

    private class TestMetrics {

        private int testsTotal;

        private Set<String> allRelatedTags = new HashSet<String>();

        private int failuresTotal, falseFailuresNum, relatedBugsNum;

        private int alwaysBadNum, degradingNum, upgradingNum, alwaysGoodNum;

        private double totalTime, avgTime, minVolatility = Double.MAX_VALUE, maxVolatility = Double.MIN_VALUE;

        TestMetrics(List<ListGridRecord> records) {
            testsTotal = records.size();
            for (ListGridRecord record : records) {
                for (String tag : record.getAttribute("tags").split(",")) {
                    allRelatedTags.add(tag.trim());
                }
                String robustnessTrend = record.getAttribute("robustnessTrend");
                if (robustnessTrend.contains("degrading")) {
                    degradingNum++;
                } else if (robustnessTrend.contains("alwaysBad")) {
                    alwaysBadNum++;
                } else if (robustnessTrend.contains("upgrading")) {
                    upgradingNum++;
                } else if (robustnessTrend.contains("alwaysGood")) {
                    alwaysGoodNum++;
                }

                JSONArray runHistory = JSONParser.parseStrict(record.getAttribute("runHistory")).isArray();
                if (runHistory.size() > 0) {
                    JSONObject runRecord = runHistory.get(0).isObject();
                    totalTime += (long) runRecord.get("duration").isNumber().doubleValue();
                    if (!runRecord.get("passed").isBoolean().booleanValue()) {
                        failuresTotal++;
                    }
                    if (runRecord.get("falseFailure").isBoolean().booleanValue()) {
                        falseFailuresNum++;
                    }
                    JSONValue relatedBug = runRecord.get("relatedBug");
                    if (!(relatedBug instanceof JSONNull) && !relatedBug.isString().stringValue().isEmpty()) {
                        relatedBugsNum++;
                    }
                }

                avgTime += Double.parseDouble(record.getAttribute("avgTestTime"));
                double volatility = Double.parseDouble(record.getAttribute("timeVolatility"));
                if (volatility < minVolatility) {
                    minVolatility = volatility;
                }
                if (volatility > maxVolatility) {
                    maxVolatility = volatility;
                }
            }
        }

        public JSONObject toJSONObject() {
            JSONObject params = new JSONObject();
            List<String> tagList = new ArrayList<String>(allRelatedTags);
            Collections.sort(tagList);
            StringBuilder sb = new StringBuilder();
            for (String tag : tagList) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(tag);
            }
            params.put("allRelatedTags", new JSONString(sb.toString()));
            params.put("testsTotal", new JSONNumber(testsTotal));
            params.put("failuresTotal", new JSONNumber(failuresTotal));
            params.put("falseFailuresNum", new JSONNumber(falseFailuresNum));
            params.put("relatedBugsNum", new JSONNumber(relatedBugsNum));
            params.put("alwaysBadNum", new JSONNumber(alwaysBadNum));
            params.put("alwaysBadPercentage", new JSONNumber(100.0 * alwaysBadNum / testsTotal));
            params.put("degradingNum", new JSONNumber(degradingNum));
            params.put("degradingPercentage", new JSONNumber(100.0 * degradingNum / testsTotal));
            params.put("upgradingNum", new JSONNumber(upgradingNum));
            params.put("upgradingPercentage", new JSONNumber(100.0 * upgradingNum / testsTotal));
            params.put("alwaysGoodNum", new JSONNumber(alwaysGoodNum));
            params.put("alwaysGoodPercentage", new JSONNumber(100.0 * alwaysGoodNum / testsTotal));
            params.put("totalTime", new JSONNumber(totalTime));
            params.put("avgTime", new JSONNumber(avgTime));
            params.put("minVolatility", new JSONNumber(minVolatility));
            params.put("maxVolatility", new JSONNumber(maxVolatility));
            return params;
        }
    }

    private class RunHistoryWindow extends Window {

        RunHistoryWindow(final ListGridRecord record) {
            setWidth(900);
            setHeight(300);
            setTitle(ClientConfig.messages.runHistory());
            ClientUtils.unifySimpleWindowStyle(this);

            final ListGrid runHistoryGrid = new ListGrid();
            runHistoryGrid.setShowRecordComponents(true);
            runHistoryGrid.setShowRecordComponentsByCell(true);

            runHistoryGrid.setWidth("99%");
            runHistoryGrid.setLayoutAlign(Alignment.CENTER);

            ListGridField recordTimeField = new ListGridField("recordTime", ClientConfig.messages.recordTime(), 150);
            recordTimeField.setType(ListGridFieldType.DATE);
            recordTimeField.setAlign(Alignment.CENTER);
            recordTimeField.setCellFormatter(new CellFormatter() {

                @SuppressWarnings("deprecation")
                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    if (value == null) {
                        return null;
                    }
                    Date date = (Date) value;
                    String[] elem = new String[6];
                    elem[0] = String.valueOf(1900 + date.getYear());
                    elem[1] = String.valueOf(date.getMonth() + 1);
                    elem[2] = String.valueOf(date.getDate());
                    elem[3] = String.valueOf(date.getHours());
                    elem[4] = String.valueOf(date.getMinutes());
                    elem[5] = String.valueOf(date.getSeconds());
                    for (int i = 1; i < elem.length; i++) {
                        if (elem[i].length() == 1) {
                            elem[i] = "0" + elem[i];
                        }
                    }
                    return elem[0] + "-" + elem[1] + "-" + elem[2] + " " + elem[3] + ":" + elem[4] + ":" + elem[5];
                }

            });

            ListGridField durationField = new ListGridField("duration", ClientConfig.messages.duration(), 150);
            durationField.setType(ListGridFieldType.FLOAT);
            durationField.setAlign(Alignment.CENTER);

            ListGridField passedField = new ListGridField("passed", ClientConfig.messages.passed(), 100);
            passedField.setType(ListGridFieldType.BOOLEAN);

            ListGridField failureTraceField = new ListGridField("failureTrace", ClientConfig.messages.failureTrace(),
                    200);
            failureTraceField.setShowHover(true);
            failureTraceField.setCellFormatter(new CellFormatter() {

                @Override
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    return value == null ? null : value.toString().replace("<", "&lt;").replace(">", "&gt;");
                }

            });
            failureTraceField.setHoverCustomizer(hoverCustomizer);

            ListGridField falseFailureField = new ListGridField("falseFailure", ClientConfig.messages.falseFailure(),
                    100);
            falseFailureField.setType(ListGridFieldType.BOOLEAN);
            falseFailureField.setCanEdit(true);
            falseFailureField.setValidateOnChange(true);
            falseFailureField.setValidators(new CustomValidator() {
                @Override
                protected boolean condition(Object value) {
                    boolean passed = record.getAttributeAsBoolean("passed");
                    boolean falseFailure = record.getAttributeAsBoolean("falseFailure");
                    return !(passed && falseFailure);
                }
            });

            ListGridField relatedBugField = new ListGridField("relatedBug", ClientConfig.messages.relatedBug(), 150);
            relatedBugField.setType(ListGridFieldType.LINK);
            relatedBugField.setCanEdit(true);

            runHistoryGrid.setFields(recordTimeField, durationField, passedField, failureTraceField, falseFailureField,
                    relatedBugField);

            ArrayList<ListGridRecord> recordList = new ArrayList<ListGridRecord>();
            JSONArray runRecordArray = JSONParser.parseStrict((record.getAttribute("runHistory"))).isArray();
            for (int i = 0; i < runRecordArray.size(); i++) {
                ListGridRecord r = new ListGridRecord();
                JSONObject runRecordObj = runRecordArray.get(i).isObject();
                r.setAttribute("recordTime",
                        new Date(Math.round(runRecordObj.get("recordTime").isNumber().doubleValue())));
                r.setAttribute("duration", Math.round(runRecordObj.get("duration").isNumber().doubleValue()));
                r.setAttribute("passed", runRecordObj.get("passed").isBoolean().booleanValue());
                r.setAttribute("falseFailure", runRecordObj.get("falseFailure").isBoolean().booleanValue());
                if (!(runRecordObj.get("failureTrace") instanceof JSONNull)) {
                    r.setAttribute("failureTrace", runRecordObj.get("failureTrace").isString().stringValue());
                }
                if (!(runRecordObj.get("relatedBug") instanceof JSONNull)) {
                    r.setAttribute("relatedBug", runRecordObj.get("relatedBug").isString().stringValue());
                }
                recordList.add(r);
            }
            runHistoryGrid.setData(recordList.toArray(new ListGridRecord[0]));

            HLayout controls = new HLayout();
            ClientUtils.unifyControlsLayoutStyle(controls);

            IButton saveButton = new IButton(ClientConfig.messages.save());
            saveButton.addClickHandler(new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                    JSONArray runRecordArray = new JSONArray();
                    int i = 0;
                    for (ListGridRecord r : runHistoryGrid.getRecords()) {
                        JSONObject runRecordObj = new JSONObject();
                        runRecordObj.put("recordTime", new JSONNumber(r.getAttributeAsDate("recordTime").getTime()));
                        runRecordObj.put("duration", new JSONNumber(r.getAttributeAsDouble("duration")));
                        runRecordObj.put("passed", JSONBoolean.getInstance(r.getAttributeAsBoolean("passed")));
                        runRecordObj.put("falseFailure",
                                JSONBoolean.getInstance(r.getAttributeAsBoolean("falseFailure")));
                        if (r.getAttribute("failureTrace") == null) {
                            runRecordObj.put("failureTrace", JSONNull.getInstance());
                        } else {
                            runRecordObj.put("failureTrace", new JSONString(r.getAttributeAsString("failureTrace")));
                        }
                        if (r.getAttribute("relatedBug") == null) {
                            runRecordObj.put("relatedBug", JSONNull.getInstance());
                        } else {
                            runRecordObj.put("relatedBug", new JSONString(r.getAttributeAsString("relatedBug")));
                        }
                        runRecordArray.set(i++, runRecordObj);
                    }
                    String runHistory = runRecordArray.toString();
                    record.setAttribute("runHistory", runHistory);
                    testCaseGrid.updateData(record);
                    RunHistoryWindow.this.destroy();
                }

            });
            controls.addMember(saveButton);

            VLayout layout = new VLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            layout.addMember(runHistoryGrid);
            layout.addMember(controls);

            addItem(layout);
        }
    }

    private class AutomationCodeWindow extends Window {

        AutomationCodeWindow(ListGridRecord record) {
            setWidth(750);
            setHeight(300);
            setTitle(ClientConfig.messages.automation());
            ClientUtils.unifySimpleWindowStyle(this);

            HTMLPane codePaneForJUnit = new HTMLPane();
            codePaneForJUnit.setBorder("1px solid black");
            codePaneForJUnit.setWidth("50%");
            codePaneForJUnit.setContents(generateCode(record, "JUnit"));

            HTMLPane codePaneForTestNG = new HTMLPane();
            codePaneForTestNG.setBorder("1px solid black");
            codePaneForTestNG.setWidth("50%");
            codePaneForTestNG.setContents(generateCode(record, "TestNG"));

            HLayout layout = new HLayout();
            ClientUtils.unifyWindowLayoutStyle(layout);
            layout.addMember(codePaneForJUnit);
            layout.addMember(codePaneForTestNG);
            addItem(layout);
        }

        private String getTemplateForJUnit() {
            StringBuilder sb = new StringBuilder();
            sb.append("<p><b>JUnit:</b></p>");
            sb.append("<pre>\n");
            sb.append("    package ${packageName}\n");
            sb.append("\n");
            sb.append("    public class ${className} {\n");
            sb.append("\n");
            sb.append("        @Test\n");
            sb.append("        @TestDoc(\n");
            sb.append("            project = \"${project}\",\n");
            sb.append("            name = \"${name}\",\n");
            sb.append("            description = \"${description}\",\n");
            sb.append("            groups = { ${tags} })\n");
            sb.append("        public void ${methodName}() throws Exception {\n");
            sb.append("            // TODO: add the test logic\n");
            sb.append("        }\n");
            sb.append("\n");
            sb.append("    }\n");
            sb.append("</pre>\n");
            return sb.toString();
        }

        private String getTemplateForTestNG() {
            StringBuilder sb = new StringBuilder();
            sb.append("<p><b>TestNG:</b></p>");
            sb.append("<pre>\n");
            sb.append("    package ${packageName}\n");
            sb.append("\n");
            sb.append("    public class ${className} {\n");
            sb.append("\n");
            sb.append("        @Test(groups = { ${tags} })\n");
            sb.append("        @TestDoc(\n");
            sb.append("            project = \"${project}\",\n");
            sb.append("            name = \"${name}\",\n");
            sb.append("            description = \"${description}\",\n");
            sb.append("        )\n");
            sb.append("        public void ${methodName}() throws Exception {\n");
            sb.append("            // TODO: add the test logic\n");
            sb.append("        }\n");
            sb.append("\n");
            sb.append("    }\n");
            sb.append("</pre>\n");
            return sb.toString();
        }

        private String generateCode(ListGridRecord record, String type) {
            String project = record.getAttribute("project");
            String name = record.getAttribute("name");
            String description = record.getAttribute("description");
            description = description == null ? "" : description;

            String tags = "";
            for (String tag : record.getAttribute("tags").split("\\s*,\\s*")) {
                if (tag.isEmpty()) {
                    continue;
                }
                tags += "\"" + tag + "\",";
            }
            if (!tags.isEmpty()) {
                tags = tags.substring(0, tags.length() - 1);
            }

            String packageName = null, className = null, methodName = null;
            String automation = record.getAttribute("automation");
            int sec = automation.lastIndexOf('.');
            if (sec != -1) {
                className = automation.substring(0, sec);
                methodName = automation.substring(sec + 1);
                sec = className.lastIndexOf('.');
                if (sec != -1) {
                    packageName = className.substring(0, sec);
                    className = className.substring(sec + 1);
                }
            }

            String code = null;
            if (type.equals("JUnit")) {
                code = getTemplateForJUnit();
            } else {
                code = getTemplateForTestNG();
            }
            code = code.replace("${project}", project);
            code = code.replace("${name}", name);
            code = code.replace("${description}", description);
            code = code.replace("${tags}", tags);
            code = code.replace("${packageName}", packageName);
            code = code.replace("${className}", className);
            code = code.replace("${methodName}", methodName);

            return code;
        }
    }
}
