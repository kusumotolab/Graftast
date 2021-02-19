package experimenter.analyzer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MoveAnalyzer {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: $java -jar MoveAnalyzer [path] [-option]");
            System.out.println("  -o: Show outline");
            System.out.println("  -a identifier size seed: Show detail about some(size) random move.");
        }

        File[] files = new File(args[0]).listFiles();
        if (files == null) {
            System.err.println("Cannot open the directory: " + args[0]);
            return;
        }
        List<MoveInfo> moveInfos = new LinkedList<>();
        List<MoveType> moveTypes = new LinkedList<>();
        for (File f: files) {
            if (f.getName().matches("[0-9]+")) {
                int commitNum = Integer.parseInt(f.getName().replace(".csv",""));
                List<String> results = Files.readAllLines(Paths.get(f.getPath()));
                for (int i = 0; i < results.size();) {
                    String src = results.get(i).split(" -> ")[0];
                    String dst = results.get(i).split(" from ")[1];
                    String dstOriginal = results.get(i).split(" from ")[0].split(" -> ")[1];
                    i += 4;
                    String identifier = results.get(i).split(" ")[0];
                    int size = 0;
                    StringBuilder builder = new StringBuilder();
                    while (!results.get(i + size).equals("to")) {
                        builder.append(results.get(i + size));
                        builder.append("\n");
                        size += 1;
                    }
                    String moveTo = results.get(i + size + 3) + results.get(i + size + 2); // to \n NodeType [xxxx,yyyy]
                    String moveFrom = results.get(i + size + 5);
                    moveInfos.add(new MoveInfo(src, dst, dstOriginal, commitNum, identifier, size, new String(builder), moveTo, moveFrom));
                    i += size + 7;
                }
            }
        }
        if (args.length >= 2 && args[1].equals("-o")) {
            for (MoveInfo m : moveInfos) {
                MoveType moveType = new MoveType(m);
                int index;
                if ((index = moveTypes.indexOf(moveType)) == -1) {
                    moveTypes.add(moveType);
                } else {
                    moveTypes.get(index).add(moveType);
                }
            }
            moveTypes.sort((x, y) -> y.getNum() - x.getNum());
            for (MoveType m : moveTypes) {
                System.out.println(m.getIdentifier());
                System.out.println("  num: " + m.getNum());
                System.out.println("  size: " + m.getSizeAverage());
            }
            int num = 0;
            double average = 0;
            for (MoveType m : moveTypes) {
                average =  (average * num + m.getSizeAverage() * m.getNum()) / (num + m.getNum());
                num += m.getNum();
            }

            System.out.println("Total: " + num);
            System.out.println("Average: " + average);
        } else if (args[1].equals("-a")) {
            if (args.length >= 3) {
                String identifier = args[2];
                List<MoveInfo> filteredMove = new LinkedList<>();
                List<String> commits = Files.readAllLines(Paths.get("C:\\Users\\a-fujimt\\Lab\\gumtreeExperiment\\tomcat\\commits.txt"));
                final Repository repository = new FileRepositoryBuilder()
                        .findGitDir(new File("C:\\Users\\a-fujimt\\Lab\\gumtreeExperiment\\tomcat\\tomcat"))
                        .build();
                final Git git = new Git(repository);
                Iterable<RevCommit> log = null;
                try {
                    log = git.log().call();
                } catch (GitAPIException e) {
                    e.printStackTrace();
                }
                List<RevCommit> revCommits = new ArrayList<>();
                log.forEach(revCommits::add);
                for (MoveInfo m: moveInfos) {
                    //if (identifier.equals(m.getIdentifier()) && m.getSize() >= 50)
                    //if (revCommits.get(m.getCommitNum()).getName().startsWith("41c2f61e2d"))
                    if (m.getSize() >= 5 && commits.contains(revCommits.get(m.getCommitNum()).getName()))
                        filteredMove.add(m);
                }
                int seed;
                int size;
                if (args.length >= 5) {
                    size = Integer.parseInt(args[3]);
                    seed = Integer.parseInt(args[4]);
                } else if (args.length >= 4) {
                    size = Integer.parseInt(args[3]);
                    seed = 0;
                } else {
                    size = 200000;
                    seed = 0;
                }
                Random random = new Random(seed);
                List<MoveInfo> selected = new LinkedList<>();
                Set<Integer> randomIndexSet = new HashSet<>();
                while (randomIndexSet.size() < size && randomIndexSet.size() < filteredMove.size())
                    randomIndexSet.add(random.nextInt(filteredMove.size()));
                randomIndexSet.forEach(i -> selected.add(filteredMove.get(i)));
                int i = 1;
                for (MoveInfo m: selected) {
                    System.out.println(i++);
                    System.out.println(m.toString());
                }
                System.out.println(getOutline(selected));
            } else {
                System.out.println("Usage: option | -a identifier size seed: Show detail about some(size) random move.");
            }
        } else if (args[1].equals("-b")) {
            long count = moveInfos.stream()
                    .filter(m -> isTarget(m.getIdentifier()))
                    //.filter(m -> !m.getIdentifier().equals("ImportDeclaration"))
                    //.filter(m -> !m.getIdentifier().equals("SimpleName"))
                    //.filter(m -> !m.getIdentifier().equals("SimpleType"))
                    .filter(m -> m.getSrcFileName().contains("Util") || m.getDstFileName().contains("Util") || m.getSrcFileName().contains("Helper") || m.getDstFileName().contains("Helper"))
                    .count();
            System.out.println(count);
//                    .forEach(m -> System.out.println(m.getSize()));
        }
    }

    private static boolean isTarget(String identifier) {
        if (identifier.equals("MethodDeclaration"))
            return true;
        else if (identifier.equals("WhileStatement"))
            return true;
        else if (identifier.equals("ForStatement"))
            return true;
        else if (identifier.equals("IfStatement"))
            return true;
        else if (identifier.equals("Block"))
            return true;
        return false;
    }

    private static String getOutline(List<MoveInfo> moveInfos) {
        StringBuilder builder = new StringBuilder();
        for (MoveInfo m: moveInfos) {
            builder.append(m.getSrcFileName());
            builder.append("\n");
            builder.append(m.getDstFileName());
            builder.append("\n");
            builder.append(m.getDstFileNameOriginal());
            builder.append("\n");
            builder.append(m.getCommitNum());
            builder.append("\n");
            builder.append(m.getSrcRangeStart()).append(",");
            builder.append(m.getSrcRangeEnd()).append(",");
            builder.append(m.getDstRangeStart()).append(",");
            builder.append(m.getDstRangeEnd()).append("\n");
        }
        return new String(builder);
    }
}