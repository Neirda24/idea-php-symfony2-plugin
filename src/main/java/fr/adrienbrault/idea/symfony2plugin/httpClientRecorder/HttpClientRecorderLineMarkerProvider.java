package fr.adrienbrault.idea.symfony2plugin.httpClientRecorder;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.httpClientRecorder.stubs.indexes.HttpClientRecorderStubIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HttpClientRecorderLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty() || !Symfony2ProjectComponent.isEnabled(elements.get(0))) {
            return;
        }

        for (PsiElement element : elements) {
            if (element instanceof PsiFile psiFile) {
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if (virtualFile != null && "har".equalsIgnoreCase(virtualFile.getExtension())) {
                    LineMarkerInfo<?> lineMarkerInfo = collectSlowLineMarkers(psiFile);
                    if (lineMarkerInfo != null) {
                        result.add(lineMarkerInfo);
                    }
                }
            }
        }
    }

    @Nullable
    private LineMarkerInfo<?> collectSlowLineMarkers(@NotNull PsiFile harFile) {
        Project project = harFile.getProject();
        VirtualFile virtualFile = harFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Set<PsiElement> targets = new HashSet<>();

        // We need to find all #[UseRecord] attributes that resolve to this file
        // 1. Get all candidates from index
        // Since we don't know the exact string used in the attribute (could be relative, absolute, or @),
        // we might need to iterate over all keys in the index or do a more clever search.
        // However, the index keys are the strings in the attributes.

        FileBasedIndex.getInstance().processAllKeys(HttpClientRecorderStubIndex.KEY, key -> {
            VirtualFile resolved = HttpClientRecorderUtil.resolvePath(project, harFile, key);
            if (virtualFile.equals(resolved)) {
                FileBasedIndex.getInstance().getFilesWithKey(HttpClientRecorderStubIndex.KEY, Collections.singleton(key), file -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (psiFile != null) {
                        // Find the attribute in the file
                        // This is a bit slow but we are in collectSlowLineMarkers
                        psiFile.accept(new com.intellij.psi.PsiRecursiveElementWalkingVisitor() {
                            @Override
                            public void visitElement(@NotNull PsiElement element) {
                                if (element instanceof PhpAttribute attribute && HttpClientRecorderUtil.RECORDER_ATTRIBUTE_CLASS.equals(attribute.getFQN())) {
                                    String attributeRecord = fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "record");
                                    if (key.equals(attributeRecord)) {
                                        targets.add(attribute);
                                    }
                                }
                                super.visitElement(element);
                            }
                        });
                    }
                    return true;
                }, GlobalSearchScope.projectScope(project));
            }
            return true;
        }, project);

        if (targets.isEmpty()) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.HTTP_CLIENT_RECORDER_LINE_MARKER)
            .setTargets(targets)
            .setTooltipText("Navigate to UseRecord tests");

        return builder.createLineMarkerInfo(harFile);
    }
}
