package org.sakaiproject.atriculate.ui.console.pages;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.sakaiproject.articulate.tincan.ArticulateTCConstants;
import org.sakaiproject.articulate.tincan.api.ArticulateTCConfigurationService;
import org.sakaiproject.articulate.tincan.model.hibernate.ArticulateTCContentPackage;
import org.sakaiproject.scorm.service.api.LearningManagementSystem;
import org.sakaiproject.scorm.ui.console.pages.ConsoleBasePage;
import org.sakaiproject.scorm.ui.console.pages.DisplayDesignatedPackage;
import org.sakaiproject.scorm.ui.console.pages.PackageListPage;
import org.sakaiproject.service.gradebook.shared.Assignment;
import org.sakaiproject.wicket.markup.html.form.CancelButton;
import org.sakaiproject.wicket.model.DecoratedPropertyModel;
import org.sakaiproject.wicket.model.SimpleDateFormatPropertyModel;

/**
 * @author Robert Long (rlong @ unicon.net)
 */
public class ArticulateTCPackageConfigurationPage extends ConsoleBasePage implements ArticulateTCConstants {

    private static final long serialVersionUID = 1L;

    @SpringBean(name="articulateTCConfigurationService")
    private ArticulateTCConfigurationService articulateTCConfigurationService;

    @SpringBean
    private LearningManagementSystem lms;

    private boolean hasGradebookInSite = false;
    private boolean hasGradebookItem = false;
    private String unlimitedMessage;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ArticulateTCPackageConfigurationPage(final PageParameters params) {
        super(params);

        long contentPackageId = params.getLong("contentPackageId");
        final ArticulateTCContentPackage articulateTCContentPackage = articulateTCConfigurationService.getContentPackage(contentPackageId);

        Form<Object> form = new Form<Object>("configurationForm") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                articulateTCConfigurationService.updateContentPackage(articulateTCContentPackage);
                articulateTCConfigurationService.processGradebookItem(articulateTCContentPackage);

                setResponsePage(params.getBoolean("no-toolbar") ? DisplayDesignatedPackage.class : PackageListPage.class);
            }
        };

        List<Integer> tryList = new LinkedList<>();

        tryList.add(-1);

        for (int i = 1; i <= CONFIGURATION_DEFAULT_ATTEMPTS; i++) {
            tryList.add(i);
        }

        this.unlimitedMessage = getLocalizer().getString("unlimited", this);

        TextField<?> nameField = new TextField<ArticulateTCContentPackage>("packageName", new PropertyModel<ArticulateTCContentPackage>(articulateTCContentPackage, "title"));
        nameField.setRequired(true);
        form.add(nameField);

        DateTimeField releaseOnDTF = new DateTimeField("releaseOnDTF", new PropertyModel(articulateTCContentPackage, "releaseOn"));
        releaseOnDTF.setRequired(true);
        form.add(releaseOnDTF);
        form.add(new DateTimeField("dueOnDTF", new PropertyModel(articulateTCContentPackage, "dueOn")));
        form.add(new DateTimeField("acceptUntilDTF", new PropertyModel(articulateTCContentPackage, "acceptUntil")));
        form.add(new DropDownChoice<Object>("numberOfTries", new PropertyModel<Object>(articulateTCContentPackage, "numberOfTries"), tryList, new TryChoiceRenderer()));
        form.add(new Label("createdBy", new DisplayNamePropertyModel(articulateTCContentPackage, "createdBy")));
        form.add(new Label("createdOn", new SimpleDateFormatPropertyModel(articulateTCContentPackage, "createdOn")));
        form.add(new Label("modifiedBy", new DisplayNamePropertyModel(articulateTCContentPackage, "modifiedBy")));
        form.add(new Label("modifiedOn", new SimpleDateFormatPropertyModel(articulateTCContentPackage, "modifiedOn")));

        hasGradebookInSite = articulateTCConfigurationService.isGradebookDefined(articulateTCContentPackage);

        // get the current gradebook item, if it exists
        Assignment assignment = null;
        if (hasGradebookInSite && articulateTCContentPackage.getAssignmentId() != null) {
            assignment = articulateTCConfigurationService.getAssignment(articulateTCContentPackage);
            hasGradebookItem = assignment != null;
        }

        // set if this item is already in the gradebook
        articulateTCContentPackage.setGraded(hasGradebookItem);
        // set default gb title to content package
        articulateTCContentPackage.setGradebookItemTitle(hasGradebookItem ? assignment.getName() : articulateTCContentPackage.getTitle());
        // set default points
        articulateTCContentPackage.setPoints(hasGradebookItem ? assignment.getPoints() : CONFIGURATION_DEFAULT_POINTS);

        /**
         * Verification message
         */
        final WebMarkupContainer gradebookSettingsVerifyMessageContainer = new WebMarkupContainer("gradebook-verify-message");
        gradebookSettingsVerifyMessageContainer.setOutputMarkupId(true);
        gradebookSettingsVerifyMessageContainer.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsVerifyMessageContainer.setMarkupId("gradebook-verify-message");
        gradebookSettingsVerifyMessageContainer.setVisible(false);
        form.add(gradebookSettingsVerifyMessageContainer);

        /**
         * GB Sync checkbox container
         */
        final WebMarkupContainer gradebookSettingsCheckboxContainer = new WebMarkupContainer("gradebook-checkbox-sync");
        gradebookSettingsCheckboxContainer.setOutputMarkupId(true);
        gradebookSettingsCheckboxContainer.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsCheckboxContainer.setMarkupId("gradebook-checkbox-sync");
        gradebookSettingsCheckboxContainer.setVisible(hasGradebookInSite);
        form.add(gradebookSettingsCheckboxContainer);

        /**
         * GB Title container
         */
        final WebMarkupContainer gradebookSettingsTitleContainer = new WebMarkupContainer("gradebook-text-title");
        gradebookSettingsTitleContainer.setOutputMarkupId(true);
        gradebookSettingsTitleContainer.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsTitleContainer.setMarkupId("gradebook-text-title");
        gradebookSettingsTitleContainer.setVisible(hasGradebookInSite && hasGradebookItem);
        form.add(gradebookSettingsTitleContainer);

        /**
         * GB Title input
         */
        final TextField<?> gradebookSettingsTitle = new TextField<String>("gradebook-input-text-title", new PropertyModel<String>(articulateTCContentPackage, "gradebookItemTitle"));
        gradebookSettingsTitle.setOutputMarkupId(true);
        gradebookSettingsTitle.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsTitle.setMarkupId("gradebook-input-text-title");
        gradebookSettingsTitle.setVisible(!hasGradebookItem); // only editable on first load
        gradebookSettingsTitleContainer.add(gradebookSettingsTitle);

        /**
         * GB Title non-editable text
         */
        final Label gradebookSettingsTitleLabel = new Label("gradebook-input-text-title-label", new PropertyModel<String>(articulateTCContentPackage, "gradebookItemTitle"));
        gradebookSettingsTitleLabel.setOutputMarkupId(true);
        gradebookSettingsTitleLabel.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsTitleLabel.setMarkupId("gradebook-input-text-title-label");
        gradebookSettingsTitleLabel.setVisible(hasGradebookItem);
        gradebookSettingsTitleContainer.add(gradebookSettingsTitleLabel);

        /**
         * GB Points container
         */
        final WebMarkupContainer gradebookSettingsPointsContainer = new WebMarkupContainer("gradebook-text-points");
        gradebookSettingsPointsContainer.setOutputMarkupId(true);
        gradebookSettingsPointsContainer.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsPointsContainer.setMarkupId("gradebook-text-points");
        gradebookSettingsPointsContainer.setVisible(hasGradebookInSite && hasGradebookItem);
        form.add(gradebookSettingsPointsContainer);

        /**
         * GB Points input
         */
        final TextField<?> gradebookSettingsPoints = new TextField<Double>("gradebook-input-text-points", new PropertyModel<Double>(articulateTCContentPackage, "points"));
        gradebookSettingsPoints.setOutputMarkupId(true);
        gradebookSettingsPoints.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsPoints.setMarkupId("gradebook-input-text-points");
        gradebookSettingsPointsContainer.add(gradebookSettingsPoints);

        /**
         * GB Record Score container
         */
        final WebMarkupContainer gradebookSettingsRecordScoreContainer = new WebMarkupContainer("gradebook-checkbox-record-score");
        gradebookSettingsRecordScoreContainer.setOutputMarkupId(true);
        gradebookSettingsRecordScoreContainer.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsRecordScoreContainer.setMarkupId("gradebook-checkbox-record-score");
        gradebookSettingsRecordScoreContainer.setVisible(hasGradebookInSite && hasGradebookItem);
        form.add(gradebookSettingsRecordScoreContainer);

        /**
         * GB Record Score radio group input
         */
        final RadioGroup gradebookSettingsRecordScoreRadioGroup = new RadioGroup("gradebook-checkbox-record-score-radio-group", new PropertyModel<String>(articulateTCContentPackage, "recordType"));
        gradebookSettingsRecordScoreRadioGroup.setOutputMarkupId(true);
        gradebookSettingsRecordScoreRadioGroup.setOutputMarkupPlaceholderTag(true);
        gradebookSettingsRecordScoreRadioGroup.setMarkupId("gradebook-checkbox-record-score-radio-group");
        gradebookSettingsRecordScoreRadioGroup.add(new Radio("gradebook-input-record-best", new Model<String>(CONFIGURATION_RECORD_SCORE_TYPE_BEST)));
        gradebookSettingsRecordScoreRadioGroup.add(new Radio("gradebook-input-record-latest", new Model<String>(CONFIGURATION_RECORD_SCORE_TYPE_LATEST)));
        gradebookSettingsRecordScoreContainer.add(gradebookSettingsRecordScoreRadioGroup);

        /**
         * GB checkbox input
         */
        AjaxCheckBox gradebookCheckboxSync = new AjaxCheckBox("gradebook-input-checkbox-sync", new PropertyModel<Boolean>(articulateTCContentPackage, "graded")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean isChecked = this.getConvertedInput();
                // delete verification message
                gradebookSettingsVerifyMessageContainer.setVisible(hasGradebookItem && !isChecked);
                target.addComponent(gradebookSettingsVerifyMessageContainer);
                // title row
                gradebookSettingsTitleContainer.setVisible(isChecked);
                target.addComponent(gradebookSettingsTitleContainer);
                // points row
                gradebookSettingsPointsContainer.setVisible(isChecked);
                target.addComponent(gradebookSettingsPointsContainer);
                // record type row
                gradebookSettingsRecordScoreContainer.setVisible(isChecked);
                target.addComponent(gradebookSettingsRecordScoreContainer);
            }
        };
        gradebookCheckboxSync.setOutputMarkupId(true);
        gradebookCheckboxSync.setOutputMarkupPlaceholderTag(true);
        gradebookCheckboxSync.setMarkupId("gradebook-input-checkbox-sync");
        gradebookSettingsCheckboxContainer.add(gradebookCheckboxSync);

        form.add(new CancelButton("cancel", (params.getBoolean("no-toolbar")) ? DisplayDesignatedPackage.class : PackageListPage.class));

        add(form);
    }

    public class DisplayNamePropertyModel extends DecoratedPropertyModel implements Serializable {
        private static final long serialVersionUID = 1L;

        public DisplayNamePropertyModel(Object modelObject, String expression) {
            super(modelObject, expression);
        }

        @Override
        public Object convertObject(Object object) {
            String userId = String.valueOf(object);

            return lms.getLearnerName(userId);
        }
    }

    public class TryChoiceRenderer extends ChoiceRenderer<Object> implements Serializable {
        private static final long serialVersionUID = 1L;

        public TryChoiceRenderer() {
            super();
        }

        @Override
        public Object getDisplayValue(Object object) {
            Integer n = (Integer) object;

            if (n == -1) {
                return unlimitedMessage;
            }

            return object;
        }
    }

}
