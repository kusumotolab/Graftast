
public interface ASTLocation {

  public static LineNumberRange NONE = new LineNumberRange(0, -1);

  public SourcePath getSourcePath();

  /**
   * このLocationが指すノードがソースコード中でどの位置にあるか、行番号の範囲を返す。 範囲が求められない場合、(0, -1)のRangeを返す
   *
   * @return 行番号の範囲
   */
  public LineNumberRange inferLineNumbers();
}

import java.util.List;

public interface ASTLocations {

  /**
   * 指定された行にあるASTのノードを推定する。候補が複数ある場合、ノードが表すソースコードが広い順にListに格納したものを返す。
   * 例えば以下のプログラムで{@code a = -a}の行を指定した場合、IfStatement、Block、ExpressionStatementの順に格納される。
   * 
   * <pre>
   * {@code
   * if (a < 0) {
   *   a = -a;
   * }
   * }
   * </pre>
   * 
   * @param lineNumber 行番号
   * 
   * @return 指定された行にあるASTノードを表すLocationのList
   */
  public List<ASTLocation> infer(int lineNumber);

  public List<ASTLocation> getAll();
}

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import jp.kusumotolab.kgenprog.project.build.CompilationPackage;
import jp.kusumotolab.kgenprog.project.test.FullyQualifiedName;

public class BuildResults {

  public final boolean isBuildFailed;

  // TODO コンパイルできないときのエラー情報はほんとにこの型でいいか？
  public final DiagnosticCollector<JavaFileObject> diagnostics;

  // ビルド実行時のテキスト出力
  public final String buildProgressText;

  // ソースとクラスファイル間のマッピング
  private final Map<Path, Path> classToSourceMap;

  // ソースとFQN間のマッピング
  private final Map<Path, Set<FullyQualifiedName>> sourceToFQNMap;
  private final Map<FullyQualifiedName, Path> fqnToSourceMap;

  // 対応関係がうまく構築できたかの可否
  private boolean isMappingAvaiable;

  // ビルド元となったソースコード
  public final GeneratedSourceCode sourceCode;

  private CompilationPackage compilationPackage;

  /**
   * 
   * @param sourceCode ビルド元となったソースコード
   * @param compilationPackage バイトコード
   * @param diagnostics ビルド時の詳細情報
   * @param buildProgressText ビルド実行時のテキスト出力
   */
  public BuildResults(final GeneratedSourceCode sourceCode,
      final CompilationPackage compilationPackage,
      final DiagnosticCollector<JavaFileObject> diagnostics, final String buildProgressText) {
    this(sourceCode, false, compilationPackage, diagnostics, buildProgressText);
  }

  /**
   * コンストラクタ（後で書き換え TODO）
   * 
   * @param sourceCode ビルド元となったソースコード
   * @param isBuildFailed ビルドの成否
   * @param diagnostics ビルド時の詳細情報
   * @param buildProgressText ビルド実行時のテキスト出力
   */
  protected BuildResults(final GeneratedSourceCode sourceCode, final boolean isBuildFailed,
      final CompilationPackage compilationPackage,
      final DiagnosticCollector<JavaFileObject> diagnostics, final String buildProgressText) {
    this.sourceCode = sourceCode;
    this.isBuildFailed = isBuildFailed;
    this.compilationPackage = compilationPackage;
    this.diagnostics = diagnostics;
    this.buildProgressText = buildProgressText;
    this.classToSourceMap = new HashMap<>();
    this.fqnToSourceMap = new HashMap<>();
    this.sourceToFQNMap = new HashMap<>();
    this.isMappingAvaiable = true;
  }

  public CompilationPackage getCompilationPackage() {
    return compilationPackage;
  }

  /**
   * 引数絵与えたソースファイルに対応するFQNのPath（FQNのSet）を返す
   * 
   * @param pathToSource ソースファイルの Path
   * @return 引数で与えたソースファイルに対応する FQN の Set
   */
  public Set<FullyQualifiedName> getPathToFQNs(final Path pathToSource) {
    return this.sourceToFQNMap.get(pathToSource);
  }

  /**
   * ソースファイルと FQN 間のマッピングを追加する
   * 
   * @param source ソースファイルの Path
   * @param fqn FQN
   */
  public void addMapping(final Path source, final FullyQualifiedName fqn) {

    Set<FullyQualifiedName> fqns = this.sourceToFQNMap.get(source);
    if (null == fqns) {
      fqns = new HashSet<>();
      this.sourceToFQNMap.put(source, fqns);
    }
    fqns.add(fqn);

    // TODO すでに同じfqnな別のsourceが登録されているかチェックすべき
    // 登録されている場合はillegalStateExceptionを投げるべき？
    this.fqnToSourceMap.put(fqn, source);
  }

  /**
   * クラスファイルの Path から対応するソースファイルの Path を返す
   * 
   * @param pathToClass クラスファイルの Path
   * @return 対応するソースファイルの Path
   */
  public Path getPathToSource(final Path pathToClass) {
    return this.classToSourceMap.get(pathToClass);
  }

  /**
   * FQN から対応するソースファイルの Path を返す
   * 
   * @param fqn FQN
   * @return 対応するソースファイルの FQN
   */
  public Path getPathToSource(final FullyQualifiedName fqn) {
    return this.fqnToSourceMap.get(fqn);
  }

  /**
   * 「ソースファイルとクラスファイルの対応関係」および「ソースファイルとFQNの対応関係」の構築の成否を登録する
   * 
   * @param available true なら成功，false なら失敗
   */
  public void setMappingAvailable(final boolean available) {
    this.isMappingAvaiable = available;
  }

  /**
   * 「ソースファイルとクラスファイルの対応関係」および「ソースファイルとFQNの対応関係」の構築の成否を返す
   * 
   * @return true なら成功，false なら失敗
   */
  public boolean isMappingAvailable() {
    return this.isMappingAvaiable;
  }
}

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.ClassVisitor;

public class ClassParser extends ClassVisitor {

  String sourceFileName;
  List<String> fqnClassName;

  public ClassParser(final int opcode) {
    super(opcode);
    this.sourceFileName = "";
    this.fqnClassName = new ArrayList<>();
  }

  @Override
  public void visit(final int version, final int access, final String name, final String signature,
      final String superName, final String[] interfaces) {

    final int index = name.lastIndexOf('/');
    if (0 < index) {
      Arrays.stream(name.substring(0, index)
          .split("/"))
    }

    // fqnClassName に対する処理
    for (final String token : name.split("/")) {
      Arrays.stream(token.split("$"))
          .forEach(t -> this.fqnClassName.add(t));
    }
  }

  @Override
  public void visitSource(final String source, final String debug) {
    this.sourceFileName = source;
  }

  public String getPartialPath() {
        this.sourceFileName);
  }

  public String getFQN() {
    return String.join(".", this.fqnClassName);
  }
}

import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClassPath {

  private static Logger log = LoggerFactory.getLogger(ClassPath.class);

  public final Path path;

  public ClassPath(final Path path) {
    log.debug("enter ClassPath(Path=\"{}\")", path.toString());
    this.path = path;
  }

  @Override
  public boolean equals(Object o) {
    return this.toString()
        .equals(o.toString());
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return this.path.toString();
  }
}

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.kusumotolab.kgenprog.fl.Ochiai;
import jp.kusumotolab.kgenprog.project.test.FullyQualifiedName;

public class EmptyBuildResults extends BuildResults {

  private Logger log = LoggerFactory.getLogger(Ochiai.class);

  public static final EmptyBuildResults instance = new EmptyBuildResults();

  private EmptyBuildResults() {
    super(null, true, null, null, null);
  }

  @Override
  public Set<FullyQualifiedName> getPathToFQNs(final Path pathToSource) {
    return Collections.emptySet();
  }

  @Override
  public void addMapping(final Path source, final FullyQualifiedName fqn) {
    // do nothing
  }

  @Override
  public Path getPathToSource(final Path pathToClass) {
    log.error("getPathToSource(Path) is unavailable in EmptyBuildResults");
    return null;
  }

  @Override
  public Path getPathToSource(final FullyQualifiedName fqn) {
    log.error("getPathToSource(FullyQualifiedName) is unavailable in EmptyBuildResults");
    return super.getPathToSource(fqn);
  }

  @Override
  public void setMappingAvailable(boolean available) {
    // do nothing
  }

  @Override
  public boolean isMappingAvailable() {
    return false;
  }
}

// TODO: クラス名を再検討
public interface GeneratedAST<T extends SourcePath> {

  public String getSourceCode();

  public String getPrimaryClassName();

  public T getSourcePath();

  public ASTLocations createLocations();

  public String getMessageDigest();
}

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APR によって生成されたソースコード 複数ソースファイルの AST の集合を持つ
 */
public class GeneratedSourceCode {

  private static Logger log = LoggerFactory.getLogger(GeneratedSourceCode.class);
  private static final String DIGEST_ALGORITHM = "MD5";

  private final List<GeneratedAST<ProductSourcePath>> productAsts;
  private final List<GeneratedAST<TestSourcePath>> testAsts;
  private final Map<SourcePath, GeneratedAST<ProductSourcePath>> pathToAst;
  private final String messageDigest;

  /**
   * @param productAsts ProductソースコードのAST
   * @param testAsts TestソースコードのList
   */
  public GeneratedSourceCode(final List<GeneratedAST<ProductSourcePath>> productAsts,
      final List<GeneratedAST<TestSourcePath>> testAsts) {
    this.productAsts = productAsts;
    this.testAsts = testAsts;
    pathToAst = productAsts.stream()
        .collect(Collectors.toMap(GeneratedAST::getSourcePath, v -> v));
    this.messageDigest = createMessageDigest();
  }

  public List<GeneratedAST<ProductSourcePath>> getProductAsts() {
    log.debug("enter getProductAsts()");
    return productAsts;
  }

  public List<GeneratedAST<TestSourcePath>> getTestAsts() {
    return testAsts;
  }

  /**
   * 引数のソースコードに対応するASTを取得する
   */
  public GeneratedAST<ProductSourcePath> getProductAst(final ProductSourcePath path) {
    log.debug("enter getProductAst()");
    return pathToAst.get(path);
  }

  /**
   * ASTLocationが対応する行番号を推定する
   */
  public LineNumberRange inferLineNumbers(final ASTLocation location) {
    log.debug("enter inferLineNumbers(Location)");
    return location.inferLineNumbers();
  }

  public String getMessageDigest() {
    return messageDigest;
  }

  public boolean isGenerationSuccess() {
    return true;
  }

  public String getGenerationMessage() {
    return "";
  }

  private String createMessageDigest() {
    try {
      final MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);

      productAsts.stream()
          .sorted(Comparator.comparing(v -> v.getSourcePath()
              .toString()))
          .map(GeneratedAST::getMessageDigest)
          .map(String::getBytes)
          .forEach(digest::update);

      return Hex.encodeHexString(digest.digest());

    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}

import java.util.Collections;

public class GenerationFailedSourceCode extends GeneratedSourceCode {

  private final String generationMessage;

  public GenerationFailedSourceCode(final String generationMessage) {
    super(Collections.emptyList(), Collections.emptyList());
    this.generationMessage = generationMessage;
  }

  @Override
  public boolean isGenerationSuccess() {
    return false;
  }

  @Override
  public String getGenerationMessage() {
    return generationMessage;
  }
}

public class LineNumberRange {

  /** 開始行番号 (この行を含む) */
  public final int start;

  /** 終了行番号 (この行を含む) */
  public final int end;

  public LineNumberRange(final int start, final int end) {
    this.start = start;
    this.end = end;
  }

  public int getLength() {
    return this.end - start + 1;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + end;
    result = prime * result + start;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    LineNumberRange other = (LineNumberRange) obj;
    if (end != other.end) {
      return false;
    }
    if (start != other.start) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Range [start=" + start + ", end=" + end + "]";
  }
}

public class NoneOperation implements Operation {

  @Override
  public GeneratedSourceCode apply(GeneratedSourceCode generatedSourceCode, ASTLocation location) {
    return generatedSourceCode;
  }
}

public interface Operation {

  public GeneratedSourceCode apply(GeneratedSourceCode generatedSourceCode, ASTLocation location);
}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Patch {

  private static final Logger log = LoggerFactory.getLogger(Patch.class);

  private final List<String> diff;
  public final String fileName;
  private final List<String> originalSourceCodeLines;
  private final List<String> modifiedSourceCodeLines;

  public Patch(final List<String> diff, final String fileName,
      final List<String> originalSourceCodeLines, final List<String> modifiedSourceCodeLines) {
    this.diff = diff;
    this.fileName = fileName;
    this.originalSourceCodeLines = originalSourceCodeLines;
    this.modifiedSourceCodeLines = modifiedSourceCodeLines;
  }

  public List<String> getOriginalSourceCodeLines() {
    return originalSourceCodeLines;
  }

  public List<String> getModifiedSourceCodeLines() {
    return modifiedSourceCodeLines;
  }

  public String getDiff() {
    return String.join(System.lineSeparator(), diff);
  }

  public void write(final Path outDir) {
    try {
      Files.write(outDir.resolve(fileName + ".java"), modifiedSourceCodeLines);
      Files.write(outDir.resolve(fileName + ".patch"), diff);
    } catch (final IOException e) {
      log.error(e.getMessage(), e);
    }
  }
}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.eclipse.jface.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.algorithm.DiffException;
import jp.kusumotolab.kgenprog.ga.Variant;

public class PatchGenerator {

  private static final Logger log = LoggerFactory.getLogger(PatchGenerator.class);

  public Patches exec(final Variant modifiedVariant) {
    log.debug("enter exec(Variant)");

    final Patches patches = new Patches();
    final GeneratedSourceCode modifiedSourceCode = modifiedVariant.getGeneratedSourceCode();
    final List<GeneratedAST<ProductSourcePath>> modifiedAsts = modifiedSourceCode.getProductAsts();

    for (final GeneratedAST<ProductSourcePath> ast : modifiedAsts) {
      try {
        final Patch patch = makePatch(ast);
        final String diff = patch.getDiff();
        if (diff.isEmpty()) {
          continue;
        }
        patches.add(patch);
      } catch (final IOException | DiffException e) {
        log.error(e.getMessage());
        return new Patches();
      }
    }
    log.debug("exit exec(Variant)");
    return patches;
  }

  /***
   * patch オブジェクトの生成を行う
   *
   * @param ast
   * @return
   * @throws IOException
   * @throws DiffException
   */
  private Patch makePatch(final GeneratedAST<?> ast) throws IOException, DiffException {
    final Path originPath = ast.getSourcePath().path;

    final String modifiedSourceCodeText = ast.getSourceCode();
    final Document document = new Document(modifiedSourceCodeText);

    final String fileName = ast.getPrimaryClassName();
    final String delimiter = document.getDefaultLineDelimiter();
    final List<String> modifiedSourceCodeLines =
        Arrays.asList(modifiedSourceCodeText.split(delimiter));
    final List<String> originalSourceCodeLines = Files.readAllLines(originPath);
    final List<String> noBlankLineOriginalSourceCodeLines = removeEndDelimiter(originalSourceCodeLines);
    final List<String> diffLines =
        makeDiff(fileName, noBlankLineOriginalSourceCodeLines, modifiedSourceCodeLines);

    return new Patch(diffLines, fileName, originalSourceCodeLines, modifiedSourceCodeLines);
  }

  /***
   * UnifiedDiff 形式の diff を返す．
   *
   * @param fileName
   * @param originalSourceCodeLines
   * @param modifiedSourceCodeLines
   * @return
   */
  private List<String> makeDiff(final String fileName, final List<String> originalSourceCodeLines,
      final List<String> modifiedSourceCodeLines) throws DiffException {
    final com.github.difflib.patch.Patch<String> diff =
        DiffUtils.diff(originalSourceCodeLines, modifiedSourceCodeLines);
    return UnifiedDiffUtils.generateUnifiedDiff(fileName, fileName, originalSourceCodeLines, diff,
        3);
  }

  private List<String> removeEndDelimiter(final List<String> sourceCodeLines) {
    for (int index = sourceCodeLines.size() - 1; index >= 0; index--) {
      final String sourceCodeLine = sourceCodeLines.get(index);
      if (!sourceCodeLine.equals("")) {
        return sourceCodeLines.subList(0, index + 1);
      }
    }

    return Collections.emptyList();
  }
}

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Patches {

  private static Logger log = LoggerFactory.getLogger(Patches.class);

  private final List<Patch> patches = new ArrayList<>();

  public void add(final Patch patche) {
    this.patches.add(patche);
  }

  public Patch get(final int index) {
    return patches.get(index);
  }

  public void writeToFile(final Path outDir) {
    try {
      if (Files.notExists(outDir)) {
        Files.createDirectories(outDir);
      }
    } catch (final IOException e) {
      log.error(e.getMessage());
    }

    for (final Patch patch : patches) {
      patch.write(outDir);
    }
  }

  public void writeToLogger() {
    for (final Patch patch : patches) {
      log.info(System.lineSeparator() + patch.getDiff());
    }
  }
}

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchesStore {

  private static Logger log = LoggerFactory.getLogger(PatchesStore.class);

  private final List<Patches> patchesList = new ArrayList<>();

  public void add(final Patches patch) {
    patchesList.add(patch);
  }

  public void writeToFile(final Path outDir) {
    log.debug("enter writeToFile(String)");
    final String timeStamp = getTimeStamp();
    final Path outDirInthisExecution = outDir.resolve(timeStamp);

    for (final Patches patches : patchesList) {
      final String variantId = makeVariantId(patches);
      final Path variantDir = outDirInthisExecution.resolve(variantId);
      patches.writeToFile(variantDir);
    }
  }

  public void writeToLogger() {
    log.debug("enter writeToLogger()");

    for (final Patches patches : patchesList) {
      patches.writeToLogger();
    }
  }

  private String makeVariantId(final Patches patches) {
    return "variant" + (patchesList.indexOf(patches) + 1);
  }

  private String getTimeStamp() {
    final Date date = new Date();
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    return sdf.format(date);
  }
}

import java.nio.file.Path;

public final class ProductSourcePath extends SourcePath {

  public ProductSourcePath(final Path path) {
    super(path);
  }
}

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.kusumotolab.kgenprog.project.build.CompilationPackage;
import jp.kusumotolab.kgenprog.project.build.CompilationUnit;
import jp.kusumotolab.kgenprog.project.build.InMemoryClassManager;
import jp.kusumotolab.kgenprog.project.build.JavaSourceFromString;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;
import jp.kusumotolab.kgenprog.project.test.TargetFullyQualifiedName;

public class ProjectBuilder {

  private static Logger log = LoggerFactory.getLogger(ProjectBuilder.class);

  private final TargetProject targetProject;

  public ProjectBuilder(final TargetProject targetProject) {
    this.targetProject = targetProject;
  }

  /**
   * @param generatedSourceCode null でなければ与えられた generatedSourceCode からビルド．null の場合は，初期ソースコードからビルド
   * @param workPath バイトコード出力ディレクトリ
   * @return ビルドに関するさまざまな情報
   */
  public BuildResults build(final GeneratedSourceCode generatedSourceCode) {
    log.debug("enter build(GeneratedSourceCode)");

    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    final StandardJavaFileManager standardFileManager =
        compiler.getStandardFileManager(null, null, null);
    final InMemoryClassManager inMemoryFileManager = new InMemoryClassManager(standardFileManager);

    // コンパイルの引数を生成
    final List<String> compilationOptions = new ArrayList<>();
    compilationOptions.add("-encoding");
    compilationOptions.add("UTF-8");
    compilationOptions.add("-classpath");
    compilationOptions.add(String.join(File.pathSeparator, this.targetProject.getClassPaths()
        .stream()
        .map(cp -> cp.path.toString())
        .collect(Collectors.toList())));
    compilationOptions.add("-verbose");
    final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    // コンパイル対象の JavaFileObject を生成
    final Iterable<? extends JavaFileObject> javaFileObjects =
        generateAllJavaFileObjects(generatedSourceCode.getProductAsts(), standardFileManager);

    // コンパイルの進捗状況を得るためのWriterを生成
    final StringWriter buildProgressWriter = new StringWriter();

    // コンパイルのタスクを生成
    final CompilationTask task = compiler.getTask(buildProgressWriter, inMemoryFileManager,
        diagnostics, compilationOptions, null, javaFileObjects);

    try {
      inMemoryFileManager.close();
    } catch (final IOException e) {
      e.printStackTrace();
    }

    // コンパイルを実行
    final boolean isBuildFailed = !task.call();
    if (isBuildFailed) {
      log.debug("exit build(GeneratedSourceCode, Path) -- build failed.");
      return EmptyBuildResults.instance;
    }

    final String buildProgressText = buildProgressWriter.toString();
    final List<CompilationUnit> compilationUnits = inMemoryFileManager.getAllClasses();
    final CompilationPackage compilationPackage = new CompilationPackage(compilationUnits);
    final BuildResults buildResults =
        new BuildResults(generatedSourceCode, compilationPackage, diagnostics, buildProgressText);

    // TODO: https://github.com/kusumotolab/kGenProg/pull/154
    // final Set<String> updatedFiles = getUpdatedFiles(verboseLines);

    final List<SourcePath> allSourcePaths = new ArrayList<>();
    allSourcePaths.addAll(this.targetProject.getProductSourcePaths());
    allSourcePaths.addAll(this.targetProject.getTestSourcePaths());

    for (final CompilationUnit compilationUnit : compilationUnits) {

      // TODO: https://github.com/kusumotolab/kGenProg/pull/154
      // 更新されたファイルの中に classFile が含まれていない場合は削除．この機能はとりあえず無しで問題ない
      // if (!updatedFiles.isEmpty() && !updatedFiles.contains(classFile.getAbsolutePath())) {
      // if (!classFile.delete()) {
      // throw new RuntimeException();
      // }
      // continue;
      // }

      // クラスファイルのパース
      final ClassParser parser = this.parse(compilationUnit);

      // 対応関係の構築
      final String partialPath = parser.getPartialPath();
      final TargetFullyQualifiedName fqn = new TargetFullyQualifiedName(parser.getFQN());
      SourcePath correspondingSourceFile = null;
      for (final SourcePath sourcePath : allSourcePaths) {
        if (sourcePath.path.endsWith(partialPath)) {
          correspondingSourceFile = sourcePath;
          break;
        }
      }
      if (null != correspondingSourceFile) {
        buildResults.addMapping(correspondingSourceFile.path, fqn);
      } else {
        buildResults.setMappingAvailable(false);
      }
    }
    log.debug("exit build(GeneratedSourceCode, Path) -- build succeeded.");
    return buildResults;
  }

  private <T extends SourcePath> Iterable<? extends JavaFileObject> generateAllJavaFileObjects(
      final List<GeneratedAST<T>> list, final StandardJavaFileManager fileManager) {

    final Iterable<? extends JavaFileObject> targetIterator =
        generateJavaFileObjectsFromGeneratedAst(list);
    final Iterable<? extends JavaFileObject> testIterator =
        generateJavaFileObjectsFromSourceFile(this.targetProject.getTestSourcePaths(), fileManager);

    return Stream.concat( //
        StreamSupport.stream(targetIterator.spliterator(), false), //
        StreamSupport.stream(testIterator.spliterator(), false))
        .collect(Collectors.toSet());
  }

  /**
   * GeneratedAST の List からJavaFileObject を生成するメソッド
   * 
   * @param asts
   * @return
   */
  private <T extends SourcePath> Iterable<? extends JavaFileObject> generateJavaFileObjectsFromGeneratedAst(
      final List<GeneratedAST<T>> asts) {
    return asts.stream()
        .map(ast -> new JavaSourceFromString(ast.getPrimaryClassName(), ast.getSourceCode()))
        .collect(Collectors.toSet());
  }

  /**
   * ソースファイルから JavaFileObject を生成するメソッド
   * 
   * @param paths
   * @param fileManager
   * @return
   */
  private Iterable<? extends JavaFileObject> generateJavaFileObjectsFromSourceFile(
      final List<? extends SourcePath> paths, final StandardJavaFileManager fileManager) {
    final Set<String> sourceFileNames = paths.stream()
        .map(f -> f.path.toString())
        .collect(Collectors.toSet());
    return fileManager.getJavaFileObjectsFromStrings(sourceFileNames);
  }

  private ClassParser parse(final CompilationUnit compilationUnit) {
    log.debug("enter parse(CompilationUnit)");
    final ClassReader reader = new ClassReader(compilationUnit.getBytecode());
    final ClassParser parser = new ClassParser(Opcodes.ASM6);
    reader.accept(parser, ClassReader.SKIP_CODE);
    log.debug("exit parse(File)");
    return parser;
  }

  // TODO: https://github.com/kusumotolab/kGenProg/pull/154
  @SuppressWarnings("unused")
  private Set<String> getUpdatedFiles(final List<String> lines) {
    final String prefixWindowsOracle = "[RegularFileObject[";
    final String prefixMacOracle = "[DirectoryFileObject[";
    final Set<String> updatedFiles = new HashSet<>();
    for (final String line : lines) {

      // for OracleJDK in Mac environment
      if (line.startsWith(prefixMacOracle)) {
        final int startIndex = prefixMacOracle.length();
        final int endIndex = line.indexOf(']');
        final String updatedFile = line.substring(startIndex, endIndex)
            .replace(":", File.separator);
        updatedFiles.add(updatedFile);
      }

      // for OracleJDK in Windows environment
      else if (line.startsWith(prefixWindowsOracle)) {
        final int startIndex = prefixWindowsOracle.length();
        final int endIndex = line.indexOf(']');
        final String updatedFile = line.substring(startIndex, endIndex);
        updatedFiles.add(updatedFile);
      }
    }
    return updatedFiles;
  }
}

import java.nio.file.Path;

public abstract class SourcePath {

  public final Path path;

  protected SourcePath(final Path path) {
    this.path = path;
  }

  @Override
  public boolean equals(Object o) {
    return this.toString()
        .equals(o.toString());
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return this.path.toString();
  }
}

import java.nio.file.Path;

public class TestSourcePath extends SourcePath {

  public TestSourcePath(final Path path) {
    super(path);
  }
}
