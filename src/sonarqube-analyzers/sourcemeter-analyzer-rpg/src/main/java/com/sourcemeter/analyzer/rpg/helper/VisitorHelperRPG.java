/**
 * Copyright (c) 2014-2015, FrontEndART Software Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by FrontEndART Software Ltd.
 * 4. Neither the name of FrontEndART Software Ltd. nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY FrontEndART Software Ltd. ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL FrontEndART Software Ltd. BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.sourcemeter.analyzer.rpg.helper;

import graphlib.Attribute;
import graphlib.AttributeComposite;
import graphlib.AttributeInt;
import graphlib.AttributeString;
import graphlib.Node;
import graphsupportlib.Metric.Position;
import com.sourcemeter.analyzer.base.batch.ProfileInitializer;
import com.sourcemeter.analyzer.base.helper.VisitorHelper;
import com.sourcemeter.analyzer.rpg.SourceMeterRPGMetricFinder;
import com.sourcemeter.analyzer.rpg.profile.SourceMeterRPGRuleRepository;

import java.io.File;
import java.util.List;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;

public class VisitorHelperRPG extends VisitorHelper {

    public static final String PROGRAM_TRESHOLD_VIOLATION_SUFFIX = "_warning_Program";
    public static final String PROCEDURE_TRESHOLD_VIOLATION_SUFFIX = "_warning_Procedure";
    public static final String SUBROUTINE_TRESHOLD_VIOLATION_SUFFIX = "_warning_Subroutine";

    private final FileSystem fileSystem;

    public VisitorHelperRPG(Project project, SensorContext sensorContext,
            ResourcePerspectives perspectives, Settings settings,
            FileSystem fileSystem) {

        super(project, sensorContext, perspectives, settings,
                new SourceMeterRPGMetricFinder());

        this.fileSystem = fileSystem;
    }

    @Override
    public void uploadWarnings(Attribute attribute, Node node, Position nodePosition) {
        if (nodePosition == null) {
            return;
        }

        AttributeComposite warningAttribute = (AttributeComposite) attribute;
        int lineId = 0;
        String warningText = "";
        String warningPath = "";

        List<Attribute> compAttributes = warningAttribute.getAttributes();
        for (Attribute a : compAttributes) {
            if ("Path".equals(a.getName())) {
                warningPath = ((AttributeString) a).getValue();
            } else if ("Line".equals(a.getName())) {
                lineId = ((AttributeInt) a).getValue();
            } else if ("WarningText".equals(a.getName())) {
                warningText = ((AttributeString) a).getValue();
            }
        }

        warningPath = nodePosition.path;

        Resource violationResource = org.sonar.api.resources.File.fromIOFile(
                new File(warningPath), this.project);

        if (violationResource == null) {
            return;
        }

        Issuable issuable = this.perspectives.as(Issuable.class,
                violationResource);
        if (issuable != null) {
            String tmpRuleKey = warningAttribute.getName();
            tmpRuleKey = getCorrectedRuleKey(tmpRuleKey);
            warningText = "SourceMeter: " + warningText;
            RuleKey ruleKey = RuleKey.of(SourceMeterRPGRuleRepository.getRepositoryKey(),
                    tmpRuleKey);
            Issue issue = issuable.newIssueBuilder().ruleKey(ruleKey)
                    .message(warningText).line(lineId).build();

            issuable.addIssue(issue);
        }
    }

    @Override
    public String getCorrectedRuleKey(String ruleKey) {
        if (ruleKey.contains(PROGRAM_TRESHOLD_VIOLATION_SUFFIX)) {
            // program treshold violation
            ruleKey = METRIC_PREFIX + ruleKey.replaceAll(PROGRAM_TRESHOLD_VIOLATION_SUFFIX, "");
        } else if (ruleKey.contains(PROCEDURE_TRESHOLD_VIOLATION_SUFFIX)) {
            // procedure treshold violation
            ruleKey = METRIC_PREFIX + ruleKey.replaceAll(PROCEDURE_TRESHOLD_VIOLATION_SUFFIX, "");
        } else if (ruleKey.contains(SUBROUTINE_TRESHOLD_VIOLATION_SUFFIX)) {
            // subroutine treshold violation
            ruleKey = METRIC_PREFIX + ruleKey.replaceAll(SUBROUTINE_TRESHOLD_VIOLATION_SUFFIX, "");
        } else if (ruleKey.contains(ProfileInitializer.ClONE_CLASS_TRESHOLD_VIOLATION_SUFFIX)) {
            // CloneClass treshold violation
            ruleKey = METRIC_PREFIX + ruleKey.replaceAll(ProfileInitializer.ClONE_CLASS_TRESHOLD_VIOLATION_SUFFIX, "");
        } else if (ruleKey.contains(ProfileInitializer.CLONE_INSTANCE_TRESHOLD_VIOLATION_SUFFIX)) {
            // CloneInstance treshold violation
            ruleKey = METRIC_PREFIX + ruleKey.replaceAll(ProfileInitializer.CLONE_INSTANCE_TRESHOLD_VIOLATION_SUFFIX, "");
        } else {
            String[] splittedKey = ruleKey.split("_");
            ruleKey = splittedKey[splittedKey.length - 1];
        }

        return ruleKey;
    }

    @Override
    public String getPathFromNode(Node node) {
        String path = null;
        Position position = graphsupportlib.Metric
                .getFirstPositionAttribute(node);

        if (null != position) {
            path = FileHelperRPG.getCorrectedFilePath(position.path, fileSystem);
        }

        return path;
    }
}
