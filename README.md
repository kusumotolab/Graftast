EN  [JP](https://github.com/kusumotolab/Graftast/README_JP.md)

# Graftast

Graftast is an extension tool of [GumTree](https://github.com/GumTreeDiff/gumtree). Comparing targets are expanded from single file to multiple files.

## Requirement

JDK8+

---

## Installation

You can download zip [here](https://github.com/kusumotolab/Graftast/releases/tag/v1.0).

---

## Usage

Unzip downloaded file, and execute a file in bin folder.

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

You can use a part of Graftast directly from the Java code. However, you needs installation of gumtree2.1.3. Referencing installation of gumtree2.1.3 is [here](https://github.com/GumTreeDiff/gumtree/wiki/Getting-Started).

### Construction of project AST

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
ITree srcTree = projectTrees.first;
ITree dstTree = projectTrees.second;
```

### Getting mappings

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
MappingStore mappingStore = graftastMain.getMappings(projectTrees);
```

or

```java
Run.initGenerators();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
ITree srcTree = projectTrees.first;
ITree dstTree = projectTrees.second;
MappingStore mappingStore = new ProjectMatcher().match(srcTree, dstTree);
```

### Calculating difference

```java
GraftastMain graftastMain = new GraftastMain();
String srcDir = "";
String dstDir = "";
Pair<ITree, ITree> projectTrees = new ProjectTreeGenerator(srcDir, dstDir, ".java");
EditScript editScript = graftastMain.calcurateEditScript(projectTrees);
```

Or

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

