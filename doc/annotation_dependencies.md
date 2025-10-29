# Annotation Package External Dependencies (as of 2025-10-29)

## Depends on `modules/requel-app` (outside `com.rreganjr.requel.annotation`)

- `impl/AbstractAnnotation.java`
  - `import com.rreganjr.requel.utils.jaxb.DateAdapter`
  - `import com.rreganjr.requel.utils.jaxb.JAXBAnnotationGroupedByPatcher`
  - `import com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher`
  - `import com.rreganjr.requel.utils.jaxb.UnmarshallerListener`
- `impl/ArgumentImpl.java`
  - `import com.rreganjr.requel.utils.jaxb.DateAdapter`
  - `import com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher`
  - `import com.rreganjr.requel.utils.jaxb.UnmarshallerListener`
- `impl/IssueImpl.java`
  - `import com.rreganjr.requel.utils.jaxb.DateAdapter`
  - `import com.rreganjr.requel.utils.jaxb.UnmarshallerListener`
- `impl/PositionImpl.java`
  - `import com.rreganjr.requel.utils.jaxb.DateAdapter`
  - `import com.rreganjr.requel.utils.jaxb.JAXBCreatedEntityPatcher`
  - `import com.rreganjr.requel.utils.jaxb.UnmarshallerListener`

## Depends on `modules/dictionary-jpa`

- `impl/command/ResolveIssueWithAddWordToDictionaryPositionCommandImpl.java`
  - `import com.rreganjr.nlp.dictionary.command.DictionaryCommandFactory`
  - `import com.rreganjr.nlp.dictionary.command.EditDictionaryWordCommand`
