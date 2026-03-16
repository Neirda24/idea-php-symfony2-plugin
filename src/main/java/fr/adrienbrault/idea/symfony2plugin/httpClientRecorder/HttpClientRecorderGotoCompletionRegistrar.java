package fr.adrienbrault.idea.symfony2plugin.httpClientRecorder;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.patterns.PatternCondition;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class HttpClientRecorderGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // #[UseRecord("...")]
        registrar.register(
            PlatformPatterns.psiElement().withParent(
                PhpElementsUtil.getAttributeNamedArgumentStringLiteralPattern(HttpClientRecorderUtil.RECORDER_ATTRIBUTE_CLASS, "record")
            ).withLanguage(PhpLanguage.INSTANCE),
            HttpClientRecorderGotoCompletionProvider::new
        );

        // #[UseRecord("...")] - default / first argument
        registrar.register(
            PlatformPatterns.psiElement().withParent(
                PlatformPatterns.psiElement(StringLiteralExpression.class)
                    .withParent(PlatformPatterns.psiElement(ParameterList.class)
                        .withParent(PlatformPatterns.psiElement(PhpAttribute.class)
                            .with(new AttributeInstancePatternCondition(HttpClientRecorderUtil.RECORDER_ATTRIBUTE_CLASS))
                        )
                    )
            ).withLanguage(PhpLanguage.INSTANCE),
            HttpClientRecorderGotoCompletionProvider::new
        );
    }

    private static class AttributeInstancePatternCondition extends PatternCondition<PhpAttribute> {
        private final String fqn;

        public AttributeInstancePatternCondition(String fqn) {
            super("AttributeInstancePatternCondition");
            this.fqn = fqn;
        }

        @Override
        public boolean accepts(@NotNull PhpAttribute phpAttribute, ProcessingContext processingContext) {
            return PhpElementsUtil.isEqualClassName(phpAttribute, fqn);
        }
    }

    private static class HttpClientRecorderGotoCompletionProvider extends GotoCompletionProvider {
        public HttpClientRecorderGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            PsiElement parent = getElement().getParent();
            if (!(parent instanceof StringLiteralExpression literal)) {
                return Collections.emptyList();
            }

            String contents = literal.getContents();
            Project project = getProject();
            Collection<LookupElement> lookupElements = new ArrayList<>();

            if (contents.startsWith("@")) {
                String defaultDir = HttpClientRecorderUtil.getDefaultDirectory(project, getElement());
                VirtualFile phpUnitConfig = HttpClientRecorderUtil.getPhpUnitConfig(project, getElement());
                VirtualFile baseDir = phpUnitConfig != null ? phpUnitConfig.getParent() : ProjectUtil.getProjectDir(project);

                if (baseDir != null) {
                    VirtualFile recordDir = baseDir.findFileByRelativePath(defaultDir);
                    if (recordDir != null) {
                        addFileLookupElements(lookupElements, recordDir, "@");
                    }
                }
            } else if (contents.startsWith("/")) {
                VirtualFile projectDir = ProjectUtil.getProjectDir(project);
                if (projectDir != null) {
                    addFileLookupElements(lookupElements, projectDir, "/");
                }
            } else {
                // Relative to current file
                PsiFile containingFile = getElement().getContainingFile();
                if (containingFile != null) {
                    VirtualFile virtualFile = containingFile.getOriginalFile().getVirtualFile();
                    if (virtualFile != null && virtualFile.getParent() != null) {
                        addFileLookupElements(lookupElements, virtualFile.getParent(), "");
                    }
                }
            }

            return lookupElements;
        }

        private void addFileLookupElements(@NotNull Collection<LookupElement> lookupElements, @NotNull VirtualFile rootDir, @NotNull String prefix) {
            VfsUtil.visitChildrenRecursively(rootDir, new VirtualFileVisitor<Void>() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    if (!file.isDirectory() && "har".equalsIgnoreCase(file.getExtension())) {
                        String relativePath = VfsUtil.getRelativePath(file, rootDir, '/');
                        if (relativePath != null) {
                            lookupElements.add(LookupElementBuilder.create(prefix + relativePath)
                                .withIcon(file.getFileType().getIcon())
                                .withTypeText(file.getExtension(), true));
                        }
                    }
                    return true;
                }
            });
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            PsiElement parent = psiElement.getParent();
            if (!(parent instanceof StringLiteralExpression literal)) {
                return Collections.emptyList();
            }

            String contents = literal.getContents();
            if (StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            VirtualFile virtualFile = HttpClientRecorderUtil.resolvePath(getProject(), psiElement, contents);
            if (virtualFile != null) {
                PsiFile file = PsiManager.getInstance(getProject()).findFile(virtualFile);
                if (file != null) {
                    return Collections.singletonList(file);
                }
            }

            return Collections.emptyList();
        }
    }
}
