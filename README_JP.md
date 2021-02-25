[EN](./README.md)  JP

# Graftast

[GumTree](https://github.com/GumTreeDiff/gumtree)の検出対象を複数のファイルに拡張します。

Graft(接木) ---複数のASTをまとめて1つのASTを構築する、という手法から"ASTの接木"→"Graft AST"の名前がつきました。

---

## インストール方法

### バイナリをダウンロード

Zipファイルを[こちら](https://github.com/kusumotolab/Graftast/releases/latest)からダウンロードしてください。

### Gradleでビルド

プロジェクトをクローンします。

```
$ git clone git@github.com:kusumotolab/Graftast.git
```

`gradle.properties` をプロジェクトルート直下に以下の内容で作成してください。

```
GITHUB_USER = XXXXXX
GUTHUB_TOKEN = YYYYYY
```

GumTreeはGitHub Packageで公開されているため`gradle.properties`にGitHubのアクセストークンを記載します。アクセストークンについての詳しい情報は[こちら](https://docs.github.com/ja/packages/learn-github-packages/about-github-packages#managing-packages)をご覧ください。

次に以下のコマンドを実行します。

```
$ cd Graftast
$ ./gradlew build
```

`build/distributions`以下にZipファイルが生成されるので解凍して利用してください。

---

## 使用方法

ダウンロードまたはビルドしたZipファイルを解凍し、binフォルダ内のファイルを実行します。

```
Graftast (diff|webdiff) dir1 dir2 fileType
```

- `dir1`と`dir2`以下に含まれるファイルで、拡張子が`fileType`に一致するものを抽出し差分を計算します。
- `fileType`の例：`.java`, `.c`,  `.py`

### オプション

- `diff`：差分をテキストで出力します。
- `webdiff`：差分をwebブラウザを使って視覚的に表示できます。GumTreeで実装されているClassicViewのみに対応しています。

---

## APIの使用方法

Graftastの機能の一部を使用することが可能です。JDK11以上が必要です。

### プロジェクト全体のASTの構築

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
Tree srcTree = projectTrees.first;
Tree dstTree = projectTrees.second;
```

### mappingの取得

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
MappingStore mappingStore = graftastMain.getMappings(projectTrees);
```

または

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
Tree srcTree = projectTrees.first;
Tree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
```

### 差分の計算

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
EditScript editScript = graftastMain.calcurateEditScript(projectTrees);
```

または

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
Tree srcTree = projectTrees.first;
Tree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
EditScriptGenerator editScriptGenerator = new ChawatheScriptGenerator();
EditScript editScript = editScriptGenerator.computeActions(mappingStore);
```

