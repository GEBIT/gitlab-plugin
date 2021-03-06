package com.dabsquared.gitlabjenkins.util;

import java.util.Collections;
import java.util.List;

import com.dabsquared.gitlabjenkins.cause.GitLabWebHookCause;
import com.dabsquared.gitlabjenkins.workflow.GitLabBranchBuild;

import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.Cause.UpstreamCause;
import hudson.plugins.git.Branch;
import hudson.plugins.git.util.BuildData;
import hudson.plugins.git.util.MergeRecord;

/**
 * @author Robin Müller
 */
public class BuildUtil {
    public static Run<?, ?> getBuildByBranch(Job<?, ?> project, String branchName) {
        for (Run<?, ?> build : project.getBuilds()) {
            BuildData data = build.getAction(BuildData.class);
            MergeRecord merge = build.getAction(MergeRecord.class);
            if (hasLastBuild(data) && isNoMergeBuild(data, merge)) {    
                for (Branch branch : data.lastBuild.getRevision().getBranches()) {
                    if (branch.getName().endsWith("/" + branchName)) {
                        return build;
                    }
                }
            }
        }
        return null;
    }

    public static Run<?, ?> getBuildBySHA1WithoutMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            MergeRecord merge = build.getAction(MergeRecord.class);
            for(BuildData data : build.getActions(BuildData.class)) {
                if (hasLastBuild(data) && isNoMergeBuild(data, merge) && data.lastBuild.isFor(sha1)) {
                    return build;
                }
            }
        }
        return null;
    }

    public static Run<?, ?> getBuildBySHA1IncludingMergeBuilds(Job<?, ?> project, String sha1) {
        for (Run<?, ?> build : project.getBuilds()) {
            for(BuildData data : build.getActions(BuildData.class)) {
                if (data != null
                    && data.lastBuild != null
                    && data.lastBuild.getMarked() != null
                    && data.lastBuild.getMarked().getSha1String().equals(sha1)) {
                    return build;
                }
            }
        }
        return null;
    }

    private static boolean isNoMergeBuild(BuildData data, MergeRecord merge) {
        return merge == null || merge.getSha1().equals(data.lastBuild.getMarked().getSha1String());
    }

    private static boolean hasLastBuild(BuildData data) {
        return data != null && data.lastBuild != null && data.lastBuild.getRevision() != null;
    }

    public static GitLabWebHookCause findGitLabWebHookCause(Run<?, ?> build) {
    	return findGitLabWebHookCauseFromUpstreamCauses(build.getCauses());
    }

    private static GitLabWebHookCause findGitLabWebHookCauseFromUpstreamCauses(List<Cause> causes) {
        for (Cause cause : causes) {
        	if (cause instanceof GitLabWebHookCause) {
                return (GitLabWebHookCause) cause;
            } else if (cause instanceof UpstreamCause) {
                List<Cause> upCauses = ((UpstreamCause) cause).getUpstreamCauses();    // Non null, returns empty list when none are set
                for (Cause upCause : upCauses) {
                    if (upCause instanceof GitLabWebHookCause) {
                        return (GitLabWebHookCause) upCause;
                    }
                }
                GitLabWebHookCause gitLabCause = findGitLabWebHookCauseFromUpstreamCauses(upCauses);
                if (gitLabCause != null) {
                    return gitLabCause;
                }
            }
        }
        return null;
    }
}
