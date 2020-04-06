[EN](https://github.com/kusumotolab/Graftast/blob/master/README.md)  JP

# Graftast

[GumTree](https://github.com/GumTreeDiff/gumtree)の検出対象を複数のファイルに拡張します。

Graft(接木) ---複数のASTをまとめて1つのASTを構築する、という手法から"ASTの接木"→"Graft AST"の名前がつきました。

## 動作条件

JDK8+

---

## インストール方法

Zipファイルを[こちら](https://github.com/kusumotolab/Graftast/releases/tag/v1.0)からダウンロードしてください。

---

## 使用方法

ダウンロードしたZipファイルを解凍し、binフォルダ内のファイルを実行します。

```
Graftast (diff|webdiff) dir1 dir2 fileType
```

- `dir1`と`dir2`以下に含まれるファイルで、拡張子が`fileType`に一致するものを抽出し差分を計算します。
- `fileType`の例：`.java`, `.c`,  `.py`

### オプション

- `diff`：差分をテキストで出力します。
- `webdiff`：差分をwebブラウザを使って視覚的に表示できます。GumTreeで実装されているMergelyViewはサポートしていません。

---

## APIの使用方法

Graftastの機能の一部を使用することが可能です。ただし、依存ライブラリとしてgumtree2.1.3が必要です。gumtree2.1.3のインストールは[こちら](https://github.com/GumTreeDiff/gumtree/wiki/Getting-Started)を参照してください。

### プロジェクト全体のASTの構築

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
ITree srcTree = projectTrees.first;
ITree dstTree = projectTrees.second;
```

### mappingの取得

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
MappingStore mappingStore = graftastMain.getMappings(projectTrees);
```

または

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
ITree srcTree = projectTrees.first;
ITree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
```

### 差分の計算

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
EditScript editScript = graftastMain.calcurateEditScript(projectTrees);
```

または

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
ITree srcTree = projectTrees.first;
ITree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
EditScriptGenerator editScriptGenerator = new ChawatheScriptGenerator();
EditScript editScript = editScriptGenerator.computeActions(mappingStore);
```

