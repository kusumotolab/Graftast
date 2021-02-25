EN  [JP](./README_JP.md)

# Graftast

Graftast is an extension tool of [GumTree](https://github.com/GumTreeDiff/gumtree). Comparing targets are expanded from single file to multiple files.

---

## Installation

### Download Binary

You can download zip [here](https://github.com/kusumotolab/Graftast/releases/latest).

### Build with Gradle

Clone this project.

```
$ git clone git@github.com:kusumotolab/Graftast.git
```

Create `gradle.properties` in project root with the following contents.

```
GITHUB_USER = XXXXXX
GUTHUB_TOKEN = YYYYYY
```

Add GitHub authentication in `gradle.properties` because GumTree is published in Github Package. If you want to know detail about GitHub authentication, see [this page](https://docs.github.com/en/packages/learn-github-packages/about-github-packages#about-scopes-and-permissions-for-package-registries).

Next, execute the following command.

```
$ cd Graftast
$ ./gradlew build
```

A Zip file will be generated under `build/distributions`.

---

## Usage

Unzip downloaded or builded file, and execute a file in bin folder.

```
Graftast (diff|webdiff) dir1 dir2 fileType
```

- Calculate differencing of files which are contained`dir1`,`dir2`directory, and whose extetnsion matches `fileType`.
- Example of `fileType`: `.java`, `.c`,  `.py`

### Options

- `diff`: Output differencing in a textual format.
- `webdiff`：Display differencing visually with web browser. But, this webdiff is not supported MergelyView that is implemented in original GumTree.

---

## API Usage

You can use a part of Graftast directly from the Java code. JDK11 is necessary.

### Construction of project AST

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
Tree srcTree = projectTrees.first;
Tree dstTree = projectTrees.second;
```

### Getting mappings

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
MappingStore mappingStore = graftastMain.getMappings(projectTrees);
```

or

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
Tree srcTree = projectTrees.first;
Tree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
```

### Calculating difference

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<Tree, Tree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
EditScript editScript = graftastMain.calcurateEditScript(projectTrees);
```

or

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
Tree srcTree = projectTrees.first;
Tree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
EditScriptGenerator editScriptGenerator = new ChawatheScriptGenerator();
EditScript editScript = editScriptGenerator.computeActions(mappingStore);
```

