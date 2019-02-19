package com.wcg.bitbucket.merge.checks;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.setting.*;

import javax.annotation.Nonnull;

public class FormValidator implements RepositorySettingsValidator {

    @Override
    public void validate(@Nonnull Settings settings, @Nonnull SettingsValidationErrors errors, @Nonnull Repository repository) {
        if (settings.getString("projectName").isEmpty()) {
            errors.addFieldError("projectName", "Must provide a project name.");
        } else if (settings.getString("branchName").isEmpty()) {
            errors.addFieldError("branchName", "Must put branch name patterns.");
        }
    }
}
