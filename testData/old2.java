
public interface ASTLocation {

  public static LineNumberRange NONE = new LineNumberRange(0, -1);

  public SourcePath getSourcePath();

  /**
   * このLocationが指すノードがソースコード中でどの位置にあるか、行番号の範囲を返す。 範囲が求められない場合、(0, -1)のRangeを返す
   *
   * @return 行番号の範囲
   */
  public LineNumberRange inferLineNumbers();
  
  public GeneratedAST<?> getGeneratedAST();
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClassPath {

  public final Path path;

  public ClassPath(final Path path) {
    this.path = path;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final ClassPath that = (ClassPath) o;
    try {
      return Files.isSameFile(path, that.path);
    } catch (final IOException e) {
      return false;
    }
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

public abstract class FullyQualifiedName {

  final public String value;

  protected FullyQualifiedName(final String value) {
    // TODO check validation
    this.value = value;
  }

  public String getPackageName() {
    final int lastIndexOf = value.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return "";
    }
    return value.substring(0, lastIndexOf);
  }

  @Override
  public boolean equals(final Object o) {
    return this.toString()
        .equals(o.toString());
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public String toString() {
    return this.value;
  }
}

// TODO: クラス名を再検討
public interface GeneratedAST<T extends SourcePath> {

  public String getSourceCode();

  public FullyQualifiedName getPrimaryClassName();

  public T getSourcePath();

  public ASTLocations createLocations();

  public String getMessageDigest();

  public int getNumberOfLines();
}

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Hex;

/**
 * APR によって生成されたソースコード 複数ソースファイルの AST の集合を持つ
 */
public class GeneratedSourceCode {

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

  public List<GeneratedAST<?>> getAllAsts() {
    final List<GeneratedAST<?>> list = new ArrayList<>();
    list.addAll(productAsts);
    list.addAll(testAsts);
    return list;
  }

  public List<GeneratedAST<ProductSourcePath>> getProductAsts() {
    return productAsts;
  }

  public List<GeneratedAST<TestSourcePath>> getTestAsts() {
    return testAsts;
  }

  /**
   * 引数のソースコードに対応するASTを取得する
   */
  public GeneratedAST<ProductSourcePath> getProductAst(final ProductSourcePath path) {
    return pathToAst.get(path);
  }

  /**
   * ASTLocationが対応する行番号を推定する
   */
  public LineNumberRange inferLineNumbers(final ASTLocation location) {
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

  default String getName(){
    return "";
  }

  default String getTargetSnippet() {
    return "";
  }
}

import java.nio.file.Path;

public final class ProductSourcePath extends SourcePath {

  /**
   * pathをrootPathからの相対パスに変換してからProductSourcePathを生成します
   * 
   * @param rootPath プロジェクトルートディレクトリへのパス
   * @param path ソースコードへのパス（絶対パスもしくはカレントディレクトリからの相対パス）
   * @return ProductSourcePath
   */
  public static ProductSourcePath relativizeAndCreate(final Path rootPath, final Path path) {
    return new ProductSourcePath(rootPath, SourcePath.relativize(rootPath, path));
  }

  public ProductSourcePath(final Path rootPath, final Path path) {
    super(rootPath, path);
  }

  @Override
  public FullyQualifiedName createFullyQualifiedName(final String className) {
    return new TargetFullyQualifiedName(className);
  }
}

import java.nio.file.Path;
import jp.kusumotolab.kgenprog.project.factory.TargetProject;

public abstract class SourcePath {

  private final Path resolvedPath;

  /**
   * {@link TargetProject#rootPath} からの相対パス
   */
  public final Path path;

  /**
   * SourcePathを生成する
   *
   * @param rootPath プロジェクトルートへのパス {@link TargetProject#rootPath}
   * @param path ルートからの相対パス
   */
  protected SourcePath(final Path rootPath, final Path path) {
    this.resolvedPath = rootPath.resolve(path);
    this.path = path;
  }

  @Override
  public boolean equals(final Object o) {
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

  public Path getResolvedPath() {
    return resolvedPath;
  }

  public abstract FullyQualifiedName createFullyQualifiedName(String className);

  /**
   * 相対パスに変換する
   *
   * @param base 基準となるパス
   * @param target 対象パス
   * @return 相対パス
   */
  public static Path relativize(final Path base, final Path target) {
    return base.normalize()
        .toAbsolutePath()
        .relativize(target.normalize()
            .toAbsolutePath());
  }
}

public class TargetFullyQualifiedName extends FullyQualifiedName {

  public TargetFullyQualifiedName(final String value) {
    super(value);
  }

}

public class TestFullyQualifiedName extends FullyQualifiedName {

  public TestFullyQualifiedName(final String value) {
    super(value);
  }

}

import java.nio.file.Path;

public class TestSourcePath extends SourcePath {

  /**
   * pathをrootPathからの相対パスに変換してからTestSourcePathを生成します
   * 
   * @param rootPath プロジェクトルートディレクトリへのパス
   * @param path ソースコードへのパス（絶対パスもしくはカレントディレクトリからの相対パス）
   * @return TestSourcePath
   */
  public static TestSourcePath relativizeAndCreate(final Path rootPath, final Path path) {
    return new TestSourcePath(rootPath, SourcePath.relativize(rootPath, path));
  }

  public TestSourcePath(final Path rootPath, final Path path) {
    super(rootPath, path);
  }

  @Override
  public FullyQualifiedName createFullyQualifiedName(final String className) {
    return new TestFullyQualifiedName(className);
  }
}
