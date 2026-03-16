package fr.adrienbrault.idea.symfony2plugin.httpClientRecorder;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class HttpClientRecorderUtil {
    public static final String RECORDER_ATTRIBUTE_CLASS = "\\Symfony\\HttpClientRecorderBundle\\PHPUnit\\Attribute\\UseRecord";
    public static final String RECORDER_EXTENSION_CLASS = "Symfony\\HttpClientRecorderBundle\\PHPUnit\\RecorderExtension";

    @Nullable
    public static VirtualFile getPhpUnitConfig(@NotNull Project project, @NotNull PsiElement context) {
        VirtualFile projectDir = ProjectUtil.getProjectDir(context);
        if (projectDir == null) return null;

        String[] configs = {"phpunit.xml", "phpunit.xml.dist"};
        for (String config : configs) {
            VirtualFile file = projectDir.findChild(config);
            if (file != null) {
                return file;
            }
        }

        // Fallback search in project
        for (String config : configs) {
            Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(project, config, GlobalSearchScope.projectScope(project));
            if (!files.isEmpty()) {
                return files.iterator().next();
            }
        }

        return null;
    }

    @NotNull
    public static String getDefaultDirectory(@NotNull Project project, @NotNull PsiElement context) {
        VirtualFile phpUnitConfig = getPhpUnitConfig(project, context);
        if (phpUnitConfig == null) {
            return "tests/fixtures/records/";
        }

        PsiFile psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(phpUnitConfig);
        if (psiFile instanceof XmlFile xmlFile) {
            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag != null) {
                // Search in <extensions>
                XmlTag extensions = rootTag.findFirstSubTag("extensions");
                if (extensions != null) {
                    for (XmlTag bootstrap : extensions.findSubTags("bootstrap")) {
                        if (RECORDER_EXTENSION_CLASS.equals(bootstrap.getAttributeValue("class"))) {
                            for (XmlTag parameter : bootstrap.findSubTags("parameter")) {
                                if ("defaultDirectory".equals(parameter.getAttributeValue("name"))) {
                                    String value = parameter.getAttributeValue("value");
                                    if (StringUtils.isNotBlank(value)) {
                                        return value;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return "tests/fixtures/records/";
    }

    @Nullable
    public static VirtualFile resolvePath(@NotNull Project project, @NotNull PsiElement context, @NotNull String path) {
        if (path.startsWith("@")) {
            String defaultDir = getDefaultDirectory(project, context);
            VirtualFile phpUnitConfig = getPhpUnitConfig(project, context);
            VirtualFile baseDir = phpUnitConfig != null ? phpUnitConfig.getParent() : ProjectUtil.getProjectDir(context);
            if (baseDir == null) return null;

            String subPath = path.substring(1);
            VirtualFile dir = baseDir.findFileByRelativePath(defaultDir);
            if (dir != null) {
                return dir.findFileByRelativePath(subPath);
            }
            return null;
        }

        if (path.startsWith("/")) {
             VirtualFile projectDir = ProjectUtil.getProjectDir(context);
             return projectDir != null ? projectDir.findFileByRelativePath(path) : null;
        }

        // Relative to current file
        PsiFile containingFile = context.getContainingFile();
        if (containingFile != null) {
            VirtualFile virtualFile = containingFile.getOriginalFile().getVirtualFile();
            if (virtualFile != null) {
                VirtualFile parent = virtualFile.getParent();
                if (parent != null) {
                    return parent.findFileByRelativePath(path);
                }
            }
        }

        return null;
    }
}
