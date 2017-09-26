/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gitstackexpander;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 *
 * @author shannah
 */
public class GitStackExpander {

    public static String gitPath = "git";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // TODO code application logic here
        
        GitStackExpander exp = new GitStackExpander();
        
        System.out.println(exp.findProjectWithRevisionId(null, args[0]));
        
        System.out.println(exp.findFile(exp.getWorkspace(), "LayeredLayoutEditorKit.java", "com.codename1.guibuilder.layeredlayout.LayeredLayoutEditorKit$AbstractWidgetEditor.getUnfilledConstraints"));
        System.out.println(exp.getRangeAsString(exp.getWorkspace(), "LayeredLayoutEditorKit.java", "com.codename1.guibuilder.layeredlayout.LayeredLayoutEditorKit$AbstractWidgetEditor.getUnfilledConstraints", 9508, 9511));
        System.out.println(exp.getRangeAsString(exp.getWorkspace(), "at com.codename1.guibuilder.layeredlayout.LayeredLayoutEditorKit$AbstractWidgetEditor.getUnfilledConstraints(LayeredLayoutEditorKit.java:9508)", 3, 3));
        EventQueue.invokeLater(()->{
            App app = exp.newApp();
            app.pack();
            app.setVisible(true);
        });
    }
    
    private File getWorkspace() {
        if (true) {
            return new File(".");
        }
        File userHome = new File(System.getProperty("user.home"));
        File workspace = new File(userHome, ".gitstackexpander");
        if (!workspace.exists()) {
            workspace.mkdir();
        }
        if (!workspace.isDirectory()) {
            throw new RuntimeException(workspace + " exists but is not a directory.  Please delete this file.");
        }
        return workspace;
    }
    
    private String md5(String str) {
        try {
            byte[] bytesOfMessage = str.getBytes("UTF-8");
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            return new String(thedigest, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    private File getWorkspace(String githubUrl) {
        File workspace = getWorkspace();
        File projectDir = new File(workspace, md5(githubUrl));
        if (!projectDir.exists()) {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(workspace);
            pb.command(gitPath, "clone", githubUrl, projectDir.getName());
            pb.inheritIO();
            try {
                Process p = pb.start();
                int code = p.waitFor();
                if (code != 0) {
                    throw new RuntimeException("Failed to clone project using git.  Error code "+code);
                }
            } catch (Exception ex) {
                Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }
        }
        return projectDir;
    }
    
    private boolean updateProject(String githubUrl) throws IOException, InterruptedException {
        File projectDir = getWorkspace(githubUrl);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(projectDir);
        pb.command(gitPath, "fetch", "origin");
        pb.inheritIO();
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            return false;
        }
        return true;
    }
    
    private boolean updateProject(File project) throws IOException, InterruptedException {
        File projectDir = project;
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(projectDir);
        pb.command(gitPath, "fetch", "origin");
        pb.inheritIO();
        Process p = pb.start();
        int code = p.waitFor();
        if (code != 0) {
            return false;
        }
        return true;
    }
    
    private void checkoutRevision(File project, String tagName, String revision) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(project);
        pb.command(gitPath, "checkout", revision);
        Process p = pb.start();
        if (p.waitFor() != 0) {
            throw new IOException("Failed to check out revision "+revision+" in project "+project);
        }
        
    }
    
    private Date getDateForRevision(String githubUrl, String revisionId, String tagName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(getWorkspace(githubUrl));
        String revNum = tagName == null ? revisionId : tagName+"^"+revisionId;
        pb.command(gitPath, "show", "-s", "--format=%ct", revNum);
        Process p = pb.start();
        try (InputStream is = p.getInputStream()) {
            Scanner scanner = new Scanner(is);
            String result = scanner.nextLine();
            Date dt = new Date(Long.parseLong(result) * 1000l);
            return dt;
            
        }
    }
    
    private Date getDateForRevision(File project, String revisionId, String tagName) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(project);
        String revNum = tagName == null ? revisionId : tagName+"^"+revisionId;
        pb.command(gitPath, "show", "-s", "--format=%ct", revNum);
        Process p = pb.start();
        try (InputStream is = p.getInputStream()) {
            Scanner scanner = new Scanner(is);
            String result = scanner.nextLine();
            Date dt = new Date(Long.parseLong(result) * 1000l);
            return dt;
            
        }
    }
    
    private String getRevisionForDate(String githubUrl, String tagName, Date dt) throws IOException, InterruptedException {
        if (tagName == null) {
            tagName = "master";
        }
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(getWorkspace(githubUrl));
        pb.command(gitPath, "checkout", tagName);
        Process p = pb.start();
        if (p.waitFor() != 0) {
            throw new IOException("Failed to check out tag "+tagName);
        }
        
        pb = new ProcessBuilder();
        pb.directory(getWorkspace(githubUrl));
        pb.command(gitPath, "log", "--pretty=format:%H %cd", "--date=raw");
        p = pb.start();
        try (InputStream is = p.getInputStream()) {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String revId = line.substring(0, line.indexOf(" "));
                String ts = line.substring(line.indexOf(" ")+1);
                ts = ts.substring(0, ts.indexOf(" "));
                Date tds = new Date(Long.parseLong(ts)*1000l);
                if (tds.getTime() <= dt.getTime()) {
                    return revId;
                }
            }
        }
        return null;
        
    }
    
    private String getRevisionForDate(File project, String tagName, Date dt) throws IOException, InterruptedException {
        if (tagName == null) {
            tagName = "master";
        }
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(project);
        pb.command(gitPath, "checkout", tagName);
        Process p = pb.start();
        if (p.waitFor() != 0) {
            throw new IOException("Failed to check out tag "+tagName);
        }
        
        pb = new ProcessBuilder();
        pb.directory(project);
        pb.command(gitPath, "log", "--pretty=format:%H %cd", "--date=raw");
        p = pb.start();
        try (InputStream is = p.getInputStream()) {
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String revId = line.substring(0, line.indexOf(" "));
                String ts = line.substring(line.indexOf(" ")+1);
                ts = ts.substring(0, ts.indexOf(" "));
                Date tds = new Date(Long.parseLong(ts)*1000l);
                if (tds.getTime() <= dt.getTime()) {
                    return revId;
                }
            }
        }
        return null;
        
    }
    
    private File findProjectWithRevisionId(String tagName, String commit) throws IOException, InterruptedException {
        if (tagName == null) tagName = "master";
        File workspace = getWorkspace();
        for (File project : workspace.listFiles()) {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(project);
            pb.command(gitPath, "fetch", "origin");
            pb.inheritIO();
            Process p = pb.start();
            
            if (p.waitFor() != 0) {
                throw new IOException("Failed to fetch origin for project "+project);
            }
            pb = new ProcessBuilder();
            pb.directory(project);
            pb.command(gitPath, "checkout", tagName);
            p = pb.start();
            if (p.waitFor() != 0) {
                throw new IOException("Failed to checkout "+tagName+ " of project "+project);
            }
            
            pb = new ProcessBuilder();
            pb.directory(project);
            pb.command(gitPath, "log", "--pretty=format:%H %h");
            p = pb.start();
            
            try (InputStream is = p.getInputStream()) {
                Scanner scanner = new Scanner(is);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String longId = line.substring(0, line.indexOf(" "));
                    String shortId = line.substring(line.indexOf(" ")+1);
                    if (commit.equals(longId) || commit.equals(shortId)) {
                        return project;
                    }
                }
            }
            
        }
        
        return null;
        
    }
    
    private Set<File> findMatchingFiles(Set<File> matches, File root, String fileName) {
        if (root.getName().equals(fileName)) {
            matches.add(root);
        }
        if (root.isDirectory()) {
            for (File child : root.listFiles()) {
                findMatchingFiles(matches, child, fileName);
            }
        }
        return matches;
    }
    
    private boolean containsClass(File file, String className) {
        String fileClassName = file.getName();
        int dotPos = fileClassName.indexOf(".");
        if (dotPos > -1) {
            fileClassName = fileClassName.substring(0, dotPos);
        }
        StringTokenizer strtok = new StringTokenizer(className, ".$");
        List<String> parts = new ArrayList<String>();
        int pos = -1;
        while (strtok.hasMoreElements()) {
            String tok = strtok.nextToken();
            parts.add(tok);
            if (tok.equals(fileClassName)) {
                pos = parts.size()-1;
            }
        }
        if (pos < 0) {
            return false;
        }
        File tmpF = file.getParentFile();
        pos--;
        while (pos >= 0) {
            if (tmpF == null || !tmpF.getName().equals(parts.get(pos))) {
                return false;
            }
            pos--;
            tmpF = tmpF.getParentFile();
        }
        return true;
        
    }
    
    private Set<File> filterOnClassName(Set<File> input, Set<File> output, String className) {
        for (File f : input) {
            if (containsClass(f, className)) {
                output.add(f);
            }
        }
        return output;
    }
    
    private Set<File> findFile(File root, String fileName, String className) {
        Set<File> matches = findMatchingFiles(new HashSet<File>(), root, fileName);
        return filterOnClassName(matches, new HashSet<File>(), className);
    }
    
    private class Line {
        String content;
        int lineNum;
        
        Line(String content, int lineNum) {
            this.content = content;
            this.lineNum = lineNum;
        }

        @Override
        public String toString() {
            return lineNum+": "+content;
        }
        
        
    }
    
    private List<Line> getRangeAsString(File root, String fileName, String className, int startLine, int endLine) throws FileNotFoundException, IOException {
        Set<File> matches = findFile(root, fileName, className);
        if (matches.isEmpty()) {
            throw new FileNotFoundException("The file "+fileName+" with class "+className+" was not found.");
        }
        File f = matches.iterator().next();
        int lineNum = 1;
        List<Line> out = new ArrayList<Line>();
        try (FileInputStream fis = new FileInputStream(f)) {
            Scanner s = new Scanner(fis);
            while (s.hasNextLine()) {
                String line = s.nextLine();
                if (lineNum >= startLine && lineNum < endLine) {
                    out.add(new Line(line, lineNum));
                }
                lineNum++;
                if (lineNum >= endLine) {
                    break;
                }
                
            }
        }
        return out;
    }
    
    private List<Line> getRangeAsString(File root, String fileName, String className, int lineNum, int before, int after) throws IOException {
        return getRangeAsString(root, fileName, className, lineNum-before, lineNum+after+1);
    }
    
    
    private class StackFrame {
        int lineNum;
        String fileName;
        String className;
        
        StackFrame(String line) {
            line = line.trim();
            int spacePos = line.indexOf(" ");
            className = line.substring(spacePos+1);
            int parenPos = className.indexOf("(");
            className = className.substring(0, parenPos);
            
            parenPos = line.indexOf("(");
            fileName = line.substring(parenPos+1);
            fileName = fileName.substring(0, fileName.indexOf(")"));
            lineNum = Integer.parseInt(fileName.substring(fileName.indexOf(":")+1));
            fileName = fileName.substring(0, fileName.indexOf(":"));
            
        }
    }
    
    private List<Line> getRangeAsString(File root, String stackFrame, int before, int after) {
        try {
            StackFrame sf = new StackFrame(stackFrame);
            return getRangeAsString(root, sf.fileName, sf.className, sf.lineNum, before, after);
        } catch (Throwable t) {
            Line l = new Line(t.getMessage(), -1);
            List<Line> out = new ArrayList<Line>();
            out.add(l);
            return out;
            
        }
    }
    
    private void setProjectsToRevision(String tagName, String revision, File... projects) throws IOException, InterruptedException {
        File paceProject = findProjectWithRevisionId(tagName, revision);
        if (paceProject == null) {
            throw new FileNotFoundException("Could not find project in workspace with given revision: "+revision);
        }
        
        Date dt = getDateForRevision(paceProject, revision, tagName);
        if (dt == null) {
            throw new FileNotFoundException("Failed to get date for revision "+revision);
        }
        for (File project : projects) {
            try {
                String rev = getRevisionForDate(project, tagName, dt);
                if (rev == null) {
                    throw new FileNotFoundException("Project "+project+" has no revision corresponding to "+revision);
                }
                //updateProject(project);
                checkoutRevision(project, null, rev);
            } catch (Throwable t){}
            
        }
    }
    
    private App newApp() {
        return new App();
    }
    
    private class App extends JFrame {
        SwingWorker currWorker;
        JTextField commit, before, after;
        JTextArea input, output;
        JSplitPane splitPane;
        JProgressBar progress;
        App() {
            try {
                setTitle(getWorkspace().getCanonicalFile().getName());
            } catch (Throwable t){}
            getRootPane().putClientProperty("Window.documentFile", getWorkspace());
            getContentPane().setLayout(new BorderLayout());
            
            progress = new JProgressBar();
            progress.setIndeterminate(true);
            
            progress.setVisible(false);
            
            input = new JTextArea();
            input.setColumns(50);
            input.setRows(50);
            
            output = new JTextArea();
            output.setColumns(50);
            output.setRows(50);
            output.setEditable(false);
            
            JPanel left = new JPanel();
            left.setLayout(new BorderLayout());
            left.add(BorderLayout.NORTH, new JLabel("Paste StackTrace Below:"));
            left.add(BorderLayout.CENTER, input);
            
            JPanel right = new JPanel();
            right.setLayout(new BorderLayout());
            right.add(BorderLayout.NORTH, new JLabel("Output"));
            right.add(BorderLayout.CENTER, output);
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(left), new JScrollPane(right));
            
            JButton convert = new JButton("Convert");
            convert.addActionListener(e->{
                final String inputString = input.getText();
                final String commitHash = commit.getText().equals("") ? null: commit.getText();
                int ibefore = 0;
                int iafter = 0;
                try {
                    ibefore = Integer.parseInt(before.getText());
                } catch (Throwable t){}
                
                try {
                    iafter = Integer.parseInt(after.getText());
                } catch (Throwable t){}
                final int fibefore = ibefore;
                final int fiafter = iafter;
                output.setText("");
                progress.setValue(0);
                
                progress.setVisible(true);
                SwingWorker worker = new SwingWorker<String,String>() {

                    @Override
                    protected String doInBackground() throws Exception {
                        if (commitHash != null) {
                            setProjectsToRevision(null, commitHash, getWorkspace().listFiles());
                        }
                        
                        StringBuilder sb = new StringBuilder();
                        Scanner s = new Scanner(inputString);
                        while (s.hasNextLine()) {
                            
                            String line = s.nextLine();
                            if (line.contains("Codename One revisions:")) {
                                final String newHash = line.substring(line.lastIndexOf(" ")).trim();
                                if (newHash.length() == 40) {
                                    setProjectsToRevision(null, newHash, getWorkspace().listFiles());
                                    EventQueue.invokeLater(()->{
                                        commit.setText(newHash);
                                    });
                                }
                            }
                            sb.append(line).append("\n");
                            if (line.trim().startsWith("at ")) {
                                List<Line> contents = getRangeAsString(getWorkspace(), line.trim(), fibefore, fiafter);
                                for (Line l : contents) {
                                    sb.append("  > ").append(l.toString()).append("\n");
                                }
                            }
                        }
                           
                        return sb.toString();
                    }

                    
                    @Override
                    protected void done() {
                        try {
                            output.setText(get());
                            progress.setValue(100);
                            progress.setVisible(false);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ExecutionException ex) {
                            Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    
                    
                };
                if (currWorker != null) {
                    if (!currWorker.isDone()) {
                        currWorker.cancel(true);
                    }
                }
                currWorker = worker;
                worker.execute();
            });
            
            JButton help = new JButton("Help");
            help.addActionListener(e->{
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/shannah/git-stack-expander/wiki/Usage-Instructions"));
                } catch (IOException ex) {
                    Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
                } catch (URISyntaxException ex) {
                    Logger.getLogger(GitStackExpander.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            
            commit = new JTextField(30);
            before = new JTextField(3);
            before.setToolTipText("Enter the number of lines before each stack frame to print. Default 0");
            after = new JTextField(3);
            after.setToolTipText("Enter the number of lines after each stack frame to print.  Default 0");
            
            JPanel north = new JPanel(new FlowLayout());
            north.add(new JLabel("Commit:"));
            north.add(commit);
            north.add(new JLabel("-B:"));
            north.add(before);
            north.add(new JLabel("-A:"));
            north.add(after);
            
            
            
            getContentPane().add(BorderLayout.NORTH, north);
            getContentPane().add(BorderLayout.CENTER, splitPane);
            
            JPanel south = new JPanel(new FlowLayout());
            south.add(convert);
            south.add(help);
            south.add(progress);
            getContentPane().add(BorderLayout.SOUTH, south);
            
            
        }
    }
    
}
