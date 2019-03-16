# Overview

1. The plugin is called by Bitbucket server upon a pull request is viewed by a reviewer as pre-check condition
2. The plugin gets the branch name to be merged(this is the feature branch name for example) and the configured TeamCity project name for the repository
3. The plugin calls TeamCity REST API to get corresponding build tasks' statuses using the Project Name and Branch Name

## Successful conditions: 
    *	There has to be successful builds in TC for the feature/bugfix branch
    *	All build jobs must have state: finished and status: success
    *	There cannot be any build job in queue for that branch.
## Failure conditions:
    *	If any job has any other status than 'success'
    *	if any job has any other state than 'finished'
    *	If any job is in queue
    *	if any system error happens
    *	if the authentication fails from TeamCity
    *	if no corresponding project/branch name is found in TeamCity
Based on the condition, the plugin enable/disable the **Merge** button, if disabled, a short related message is shown.

# Build

use *mvn clean install* command to create artifacts

# Install

From BitBucket Administration -> Manage add-ons -> Upload add-on upload the jar file

# Configuration

1.  The plugin needs a TeamCity user credential (in Base64 encoded form) to be in bitbucket_home_dir/../app/WEB-INF/classes/custom_merge_plugin.properties file.
	**authentication_hash**=\<*paste Base64 Encoded hash value*\>". The home directory can be figured out by looking at: Administration -> Troubeshooting and support tools -> System Information -> "Home Directory"
	N.B. changing user hash in the properties file takes effect without any reload/reboot
2.  For each repository, go to Repository Settings -> Merge check and enable the plugin (Custom Merge Checks)
    The target TC project ID has to be configured while the plugin is enabled. This is a per-repository settings.
    Specify the **Branch name rules** as regular expressions. For example, to get only the **branchName** from "refs/heads/feature/branchName", specify "refs/heads/feature/(.\*)". If expectation is **feature/branchName**, specify "refs/heads/(feature/.\*)".
    Multiple branch rules can be specified separated by comma.
	
