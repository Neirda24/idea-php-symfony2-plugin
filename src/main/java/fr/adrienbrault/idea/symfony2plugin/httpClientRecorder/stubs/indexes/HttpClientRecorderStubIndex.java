package fr.adrienbrault.idea.symfony2plugin.httpClientRecorder.stubs.indexes;

import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.httpClientRecorder.HttpClientRecorderUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ObjectStreamDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HttpClientRecorderStubIndex extends FileBasedIndexExtension<String, HttpClientRecorderStubIndex.UseRecordValue> {

    public static final ID<String, UseRecordValue> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.http_client_recorder_use_record");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final ObjectStreamDataExternalizer<UseRecordValue> EXTERNALIZER = new ObjectStreamDataExternalizer<>();

    @NotNull
    @Override
    public ID<String, UseRecordValue> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, UseRecordValue, FileContent> getIndexer() {
        return inputData -> {
            Map<String, UseRecordValue> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if (!(psiFile instanceof PhpFile) || !Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            for (PhpAttribute attribute : ((PhpFile) psiFile).getAttributes()) {
                if (HttpClientRecorderUtil.RECORDER_ATTRIBUTE_CLASS.equals(attribute.getFQN())) {
                    processAttribute(attribute, map);
                }
            }

            for (PhpClass phpClass : ((PhpFile) psiFile).getTopLevelClasses()) {
                for (PhpAttribute attribute : phpClass.getAttributes()) {
                    if (HttpClientRecorderUtil.RECORDER_ATTRIBUTE_CLASS.equals(attribute.getFQN())) {
                        processAttribute(attribute, map);
                    }
                }
                for (Method method : phpClass.getOwnMethods()) {
                    for (PhpAttribute attribute : method.getAttributes()) {
                        if (HttpClientRecorderUtil.RECORDER_ATTRIBUTE_CLASS.equals(attribute.getFQN())) {
                            processAttribute(attribute, map);
                        }
                    }
                }
            }

            return map;
        };
    }

    private void processAttribute(@NotNull PhpAttribute attribute, @NotNull Map<String, UseRecordValue> map) {
        String record = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(attribute, "record");
        if (record != null && !record.isBlank()) {
            map.put(record, new UseRecordValue(record));
        }
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<UseRecordValue> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return file -> file.getFileType() == com.jetbrains.php.lang.PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public static class UseRecordValue implements Serializable {
        private final String record;

        public UseRecordValue(String record) {
            this.record = record;
        }

        public String getRecord() {
            return record;
        }
    }
}
