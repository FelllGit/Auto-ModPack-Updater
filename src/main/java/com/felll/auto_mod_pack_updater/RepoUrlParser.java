package com.felll.auto_mod_pack_updater;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RepoUrlParser {

    private static final Pattern GITHUB = Pattern.compile("https?://(?:www\\.)?github\\.com/([^/]+)/([^/]+?)(?:\\.git)?(?:/tree/([^/]+))?(?:/.*)?$");
    private static final Pattern GITHUB_RAW = Pattern.compile("https?://raw\\.githubusercontent\\.com/([^/]+)/([^/]+)/([^/]+)/?(.*)$");
    private static final Pattern GITLAB = Pattern.compile("https?://(?:www\\.)?gitlab\\.com/([^/]+)/([^/]+?)(?:\\.git)?(?:/-/tree/([^/]+))?(?:/.*)?$");
    private static final Pattern GITLAB_RAW = Pattern.compile("https?://(?:www\\.)?gitlab\\.com/([^/]+)/([^/]+)/-/raw/([^/]+)/?(.*)$");
    private static final Pattern GITEA = Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+?)(?:\\.git)?(?:/src/(?:branch|commit)/([^/]+))?(?:/.*)?$");
    private static final Pattern CODEBERG = Pattern.compile("https?://(?:www\\.)?codeberg\\.org/([^/]+)/([^/]+?)(?:\\.git)?(?:/src/([^/]+))?(?:/.*)?$");
    private static final Pattern SOURCEHUT = Pattern.compile("https?://(?:www\\.)?git\\.sr\\.ht/(?:~)?([^/]+)/([^/]+?)(?:\\.git)?(?:/tree/([^/]+))?(?:/.*)?$");
    private static final String DEFAULT_BRANCH = "main";

    private RepoUrlParser() {
    }

    public static record BaseUrlInfo(String baseUrl, String provider) {
    }

    public static record RepoInfo(String owner, String repo, String branch, String provider, String host) {
    }

    public static BaseUrlInfo parse(String url) {
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        BaseUrlInfo info = tryGitHub(trimmed);
        if (info != null) return info;
        info = tryGitLab(trimmed);
        if (info != null) return info;
        info = tryGitea(trimmed);
        if (info != null) return info;
        info = tryCodeberg(trimmed);
        if (info != null) return info;
        info = trySourcehut(trimmed);
        if (info != null) return info;
        return tryGeneric(trimmed);
    }

    public static RepoInfo parseForApi(String url) {
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return null;
        Matcher m = GITHUB.matcher(trimmed);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : DEFAULT_BRANCH;
            return new RepoInfo(m.group(1), m.group(2).replaceAll("\\.git$", ""), branch, "github", "github.com");
        }
        m = GITHUB_RAW.matcher(trimmed);
        if (m.matches()) return new RepoInfo(m.group(1), m.group(2), m.group(3), "github", "github.com");
        m = GITLAB.matcher(trimmed);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : DEFAULT_BRANCH;
            return new RepoInfo(m.group(1), m.group(2).replaceAll("\\.git$", ""), branch, "gitlab", "gitlab.com");
        }
        m = GITLAB_RAW.matcher(trimmed);
        if (m.matches()) return new RepoInfo(m.group(1), m.group(2), m.group(3), "gitlab", "gitlab.com");
        m = CODEBERG.matcher(trimmed);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : DEFAULT_BRANCH;
            return new RepoInfo(m.group(1), m.group(2).replaceAll("\\.git$", ""), branch, "codeberg", "codeberg.org");
        }
        m = GITEA.matcher(trimmed);
        if (m.matches()) {
            String host = m.group(1);
            if (host.contains("gitlab") || host.contains("github") || host.contains("codeberg")) return null;
            String branch = m.group(4) != null ? m.group(4) : DEFAULT_BRANCH;
            return new RepoInfo(m.group(2), m.group(3).replaceAll("\\.git$", ""), branch, "gitea", host);
        }
        return null;
    }

    private static BaseUrlInfo tryGitHub(String url) {
        Matcher m = GITHUB_RAW.matcher(url);
        if (m.matches()) {
            String path = m.group(4) != null && !m.group(4).isEmpty() ? m.group(4) : "";
            String base = String.format("https://raw.githubusercontent.com/%s/%s/%s/", m.group(1), m.group(2), m.group(3));
            return new BaseUrlInfo(base + path, "github");
        }
        m = GITHUB.matcher(url);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : DEFAULT_BRANCH;
            return new BaseUrlInfo(String.format("https://raw.githubusercontent.com/%s/%s/%s/", m.group(1), m.group(2), branch), "github");
        }
        return null;
    }

    private static BaseUrlInfo tryGitLab(String url) {
        Matcher m = GITLAB_RAW.matcher(url);
        if (m.matches()) {
            String path = m.group(4) != null && !m.group(4).isEmpty() ? m.group(4) : "";
            String base = String.format("https://gitlab.com/%s/%s/-/raw/%s/", m.group(1), m.group(2), m.group(3));
            return new BaseUrlInfo(base + path, "gitlab");
        }
        m = GITLAB.matcher(url);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : DEFAULT_BRANCH;
            return new BaseUrlInfo(String.format("https://gitlab.com/%s/%s/-/raw/%s/", m.group(1), m.group(2), branch), "gitlab");
        }
        return null;
    }

    private static BaseUrlInfo tryGitea(String url) {
        Matcher m = GITEA.matcher(url);
        if (m.matches()) {
            String host = m.group(1);
            if (host.contains("gitlab") || host.contains("github") || host.contains("codeberg")) {
                return null;
            }
            String owner = m.group(2);
            String repo = m.group(3).replaceAll("\\.git$", "");
            String branch = m.group(4) != null ? m.group(4) : DEFAULT_BRANCH;
            return new BaseUrlInfo(String.format("https://%s/%s/%s/raw/branch/%s/", host, owner, repo, branch), "gitea");
        }
        return null;
    }

    private static BaseUrlInfo tryCodeberg(String url) {
        Matcher m = CODEBERG.matcher(url);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : DEFAULT_BRANCH;
            return new BaseUrlInfo(String.format("https://codeberg.org/%s/%s/raw/branch/%s/", m.group(1), m.group(2), branch), "codeberg");
        }
        return null;
    }

    private static BaseUrlInfo trySourcehut(String url) {
        Matcher m = SOURCEHUT.matcher(url);
        if (m.matches()) {
            String branch = m.group(3) != null ? m.group(3) : "main";
            return new BaseUrlInfo(String.format("https://git.sr.ht/~%s/%s/blob/%s/", m.group(1), m.group(2), branch), "sourcehut");
        }
        return null;
    }

    private static BaseUrlInfo tryGeneric(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return null;
        }
        String base = url.endsWith("/") ? url : url + "/";
        return new BaseUrlInfo(base, "generic");
    }
}
