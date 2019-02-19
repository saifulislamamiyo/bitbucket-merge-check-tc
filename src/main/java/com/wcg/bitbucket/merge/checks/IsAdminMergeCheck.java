package com.wcg.bitbucket.merge.checks;

import com.atlassian.bitbucket.hook.repository.*;
import com.atlassian.bitbucket.i18n.I18nService;
import com.atlassian.bitbucket.permission.PermissionService;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.cache.CacheManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.util.ClassLoaderUtils;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.WrapperMain;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("isAdminMergeCheck")
public class IsAdminMergeCheck implements RepositoryMergeCheck {

    private static final Logger log = LoggerFactory.getLogger(IsAdminMergeCheck.class);

    private final I18nService i18nService;
    private final PermissionService permissionService;

    @Autowired
    public IsAdminMergeCheck(@ComponentImport I18nService i18nService,
            @ComponentImport PermissionService permissionService, @ComponentImport CacheManager cacheManager) {
        this.i18nService = i18nService;
        this.permissionService = permissionService;
    }

    public String ReadProps() {
        String retProp = null;
        try {
            InputStream iStream = ClassLoaderUtils.getResourceAsStream("custom_merge_plugin.properties", this.getClass());
            Properties p = new Properties();
            p.load(iStream);
            retProp = p.getProperty("authentication_hash").trim();
        } catch (IOException ex) {
            retProp = "Exception";
            ex.printStackTrace();
        } finally {
            return retProp;
        }
    }

    public static void print(String toPrint) {
        ZonedDateTime zdt = ZonedDateTime.now();
        System.out.println(zdt.toLocalTime() + " : " + toPrint);
        log.debug(zdt.toLocalTime() + " : " + toPrint);
    }

    public String getBranchName(String fullBranchName, String regex) {
        String bName = "";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(fullBranchName);
        if (m.find()) {
            try {
                bName = m.group(1);
            } catch (Exception e) {
                return "";
            }
        }
        return bName;
    }

    @Nonnull
    @Override
    public RepositoryHookResult preUpdate(PreRepositoryHookContext context, PullRequestMergeHookRequest request) {

        // changes for GBLDEVOPS-771
        Set<PullRequestParticipant> reviewers = request.getPullRequest().getReviewers();
        for (PullRequestParticipant reviewer : reviewers) {
//            System.out.println("Reviewer name: " + reviewer.getUser().getDisplayName());
//            System.out.println("Reviewer name: " + reviewer.getStatus());
            if (reviewer.getStatus() == PullRequestParticipantStatus.NEEDS_WORK) {
                return RepositoryHookResult.rejected("The PR needs work.", "The PR needs work.");
            }
        }
        Repository repository = request.getPullRequest().getToRef().getRepository();
        String fullBranchName = request.getPullRequest().getFromRef().getId();
        print("PR request from branch: " + fullBranchName);
        String[] branchRules = context.getSettings().getString("branchName").split(",");
//        String tmpRef[] = request.getPullRequest().getFromRef().getId().split("/");
        String finalBranchName = "";

        for (String regEx : branchRules) {
            finalBranchName = getBranchName(fullBranchName, regEx);
            if (!finalBranchName.isEmpty()) {
                break;
            }
        }
        //check if the final branch name is still empty, this can happen if no regex has matched
        if (finalBranchName.isEmpty()) {
            // if branch is not matched, we reject the PR
            return RepositoryHookResult.rejected(fullBranchName + " is not found in configuration.", fullBranchName + " is not found in configuration.");
        }

        print("Branch name for TC: " + finalBranchName);
        String projectName = context.getSettings().getString("projectName").trim();//context.getMergeRequest().getPullRequest().getFromRef().getRepository().getProject().getName();

        String summaryMsg = null;
        String detailedMsg = null;
//        String uName = context.getSettings().getString("tcUserName").trim();
//        String pass = context.getSettings().getString("tcPassword").trim();
        String encoding = ReadProps(); // read the authentication has from properties file of the location of <BB installation directory>/app/WEB-INF/classes
        //check if reading the property had any exceptions
        if (encoding.equalsIgnoreCase("Exception")) {
            return RepositoryHookResult.rejected("Error reading properties file, contact DevOPs", "Error reading properties file, contact DevOPs");
        }
        try {
//            encoding = Base64.getEncoder().encodeToString((uName + ":" + pass).getBytes("UTF-8"));
            WrapperMain wrapperClass = new WrapperMain(projectName, finalBranchName, encoding);
            String retResults = wrapperClass.shouldMergeBranch();
            log.info(retResults);
            if (!retResults.equalsIgnoreCase("OK")) {
                if (retResults.equals("401")) {
                    summaryMsg = "TeamCity user authentication failed.";
                    retResults = summaryMsg;
                } else {
                    summaryMsg = "TeamCity builds are not successful.";
                }
//                context.getMergeRequest().veto(summaryMsg, retResults);
                return RepositoryHookResult.rejected(summaryMsg, retResults);
            }
        } catch (Exception ex) {
            summaryMsg = i18nService.getText("wcg.plugin.merge.check.notrepoadmin.summary",
                    "A system error ocurred");
            detailedMsg = i18nService.getText("wcg.plugin.merge.check.notrepoadmin.detailed",
                    "A system error ocurred, contact DevOps.");
//            context.getMergeRequest().veto(summaryMsg, detailedMsg);
            return RepositoryHookResult.rejected(summaryMsg, detailedMsg);
        }
        return RepositoryHookResult.accepted();
    }
}
