package name.falgout.jeffrey.proto.fieldmask.usage.processor;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.protobuf.Message;
import com.google.protobuf.util.FieldMaskUtil;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import name.falgout.jeffrey.proto.ProtoDescriptor;
import name.falgout.jeffrey.proto.fieldmask.usage.RequiresFields;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class RequiresFieldsValidator extends BasicAnnotationProcessor {
  public RequiresFieldsValidator() {}

  @Override
  protected Iterable<? extends ProcessingStep> initSteps() {
    return ImmutableList.of(new RequiresFieldsProcessingStep(processingEnv));
  }

  private static class RequiresFieldsProcessingStep implements ProcessingStep {
    private final ProcessingEnvironment processingEnv;

    private final TypeMirror messageType;

    RequiresFieldsProcessingStep(ProcessingEnvironment processingEnv) {
      this.processingEnv = processingEnv;

      messageType =
          processingEnv.getElementUtils().getTypeElement(Message.class.getName()).asType();
    }

    @Override
    public Set<? extends Class<? extends Annotation>> annotations() {
      return ImmutableSet.of(RequiresFields.class);
    }

    @Override
    public Set<? extends Element> process(
        SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
      return Stream.of(
          process(elementsByAnnotation, RequiresFields.class, this::validateRequiresFields))
          .flatMap(Collection::stream)
          .collect(toImmutableSet());
    }

    private ImmutableSet<Element> process(
        SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation,
        Class<? extends Annotation> annotationType,
        UnconditionalElementProcessor processor) {
      return process(elementsByAnnotation, annotationType, element -> {
        processor.process(element);
        return true;
      });
    }

    private ImmutableSet<Element> process(
        SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation,
        Class<? extends Annotation> annotationType,
        ElementProcessor processor) {
      ImmutableSet.Builder<Element> deferredElements = ImmutableSet.builder();

      for (Element element : elementsByAnnotation.get(annotationType)) {
        try {
          boolean processed = processor.process(element);
          if (!processed) {
            deferredElements.add(element);
          }
        } catch (Exception e) {
          Exception e2 = new Exception(
              String.format("Error while processing @%s", annotationType.getSimpleName()), e);
          AnnotationMirror mirror = MoreElements.getAnnotationMirror(element, annotationType).get();
          processingEnv.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              Throwables.getStackTraceAsString(e2),
              element,
              mirror);
        }
      }

      return deferredElements.build();
    }

    private void validateRequiresFields(Element element) throws ClassNotFoundException {
      RequiresFields requiresFields = element.getAnnotation(RequiresFields.class);
      AnnotationMirror annotationMirror =
          MoreElements.getAnnotationMirror(element, RequiresFields.class).get();

      if (requiresFields.value().length == 0) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            "@RequiresFields cannot be empty.",
            element,
            annotationMirror);
        return;
      }

      TypeMirror type = element.asType();

      if (!processingEnv.getTypeUtils().isSubtype(type, messageType)) {
        processingEnv.getMessager().printMessage(
            Diagnostic.Kind.ERROR,
            "@RequiresFields must be applied to subclasses of " + Message.class.getName(),
            element,
            annotationMirror);
        return;
      }

      ProtoDescriptor<?> descriptor = getDescriptor(type);
      for (String path : requiresFields.value()) {
        if (!FieldMaskUtil.isValid(descriptor.getDescriptorForType(), path)) {
          AnnotationValue value =
              AnnotationMirrors.getAnnotationValue(annotationMirror, "value");
          processingEnv.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              String.format(
                  "Invalid field path \"%s\" for type %s",
                  path,
                  descriptor.getDescriptorForType().getFullName()),
              element,
              annotationMirror,
              value);
          return;
        }
      }
    }

    private ProtoDescriptor<?> getDescriptor(TypeMirror type) throws ClassNotFoundException {
      Preconditions.checkArgument(processingEnv.getTypeUtils().isSubtype(type, messageType));

      TypeElement typeElement = MoreElements.asType(((DeclaredType) type).asElement());
      Name binaryName = processingEnv.getElementUtils().getBinaryName(typeElement);
      @SuppressWarnings("unchecked")
      Class<? extends Message> clazz =
          (Class<? extends Message>) Class.forName(binaryName.toString());

      return ProtoDescriptor.create(clazz);
    }
  }

  @FunctionalInterface
  private interface UnconditionalElementProcessor {
    void process(Element element) throws Exception;
  }

  @FunctionalInterface
  private interface ElementProcessor {
    boolean process(Element element) throws Exception;
  }
}
