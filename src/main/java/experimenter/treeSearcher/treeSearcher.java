package experimenter.treeSearcher;

import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

public class treeSearcher {

    private static int numOfIf, numOfWhile, numOfFor;

    public static void main(String[] args) {

        Path path = Paths.get(args[0]);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith("java")) {
                        JdtTreeGenerator jdtTreeGenerator = new JdtTreeGenerator();
                        try {
                            Tree tree = jdtTreeGenerator.generateFrom().string(new String(Files.readAllBytes(file))).getRoot();
                            Iterator<Tree> iterator = TreeUtils.breadthFirstIterator(tree);
                            while (iterator.hasNext()) {
                                Tree it = iterator.next();
                                switch (it.getType().name) {
                                    case "IfStatement":
                                        numOfIf += 1;
                                        break;
                                    case "WhileStatement":
                                        numOfWhile += 1;
                                        break;
                                    case "ForStatement":
                                        numOfFor += 1;
                                        break;
                                }
                            }
                        } catch(SyntaxException e) {
                            e.printStackTrace();
                            System.err.println(file.getFileName());
                        }

                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("IfStatement: " + numOfIf);
            System.out.println("WhileStatement: " + numOfWhile);
            System.out.println("ForStatement: " + numOfFor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
