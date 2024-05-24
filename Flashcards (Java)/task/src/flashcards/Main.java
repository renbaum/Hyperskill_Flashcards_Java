package flashcards;

import java.io.*;
import java.util.*;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Stack;
import java.io.InputStream;

class CustomInputStream extends InputStream {
    private InputStream original;
    private Stack<String> inputStack;

    public CustomInputStream(InputStream original, Stack<String> inputStack) {
        this.original = original;
        this.inputStack = inputStack;
    }

    @Override
    public int read() throws IOException {
        return original.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = original.read(b, off, len);
        if (bytesRead > 0) {
            String input = new String(b, off, bytesRead);
            inputStack.push(input);
        }
        return bytesRead;
    }
}



class CustomPrintStream extends PrintStream {
    private PrintStream original;
    private Stack<String> outputStack;

    public CustomPrintStream(PrintStream original, Stack<String> outputStack){
        super(original);
        this.original = original;
        this.outputStack = outputStack;
    }

    @Override
    public void println(String x) {
        original.println(x); // Print to console
        outputStack.push(x); // Add to stack
    }

    @Override
    public void print(String s) {
        original.print(s); // Print to console
        outputStack.push(s); // Add to stack
    }

    @Override
    public void println(Object x) {
        original.println(x); // Print to console
        outputStack.push(x.toString()); // Add to stack
    }

    @Override
    public void print(Object obj) {
        original.print(obj); // Print to console
        outputStack.push(obj.toString()); // Add to stack
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        String formattedString = String.format(format, args);
        original.print(formattedString); // Print to console
        outputStack.push(formattedString); // Add to stack
        return this;
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        String formattedString = String.format(l, format, args);
        original.print(formattedString); // Print to console
        outputStack.push(formattedString); // Add to stack
        return this;
    }
}





class FlashCard{
    String term;
    String definition;
    int mistakes;

    public FlashCard(String term, String definition, int mistakes){
        this.term = term;
        this.definition = definition;
        this.mistakes = mistakes;
    }

    boolean checkAnswer(String answer){
        boolean result = answer.equals(definition);
        if(!result) mistakes++;
        return result;
    }

    void resetMistakes(){this.mistakes = 0;}

    void askTerm(){
        System.out.printf("Print the definition of \"%s\":\n", this.term);
    }

}

class CollectionFlashCards implements AutoCloseable{
    Map<String, FlashCard> flashcards;
    Scanner sc;

    CollectionFlashCards() {
        flashcards = new HashMap<>();
        sc = new Scanner(System.in);
    }

    private void addFlashcard(String term, String definition, int mistakes){
        flashcards.put(term, new FlashCard(term, definition, mistakes));
    }

    void addCard(){
        System.out.println("The card:");
        String term = sc.nextLine();
        if (flashcards.containsKey(term)) {
            System.out.printf("The card \"%s\" already exists.\n", term);
            return;
        }

        System.out.println("The definition of the card:");

        String definition = sc.nextLine();
        Collection<FlashCard> def = flashcards.values();
        for(FlashCard c : def){
            if (c.definition.equals(definition)) {
                System.out.printf("The definition \"%s\" already exists.\n", definition);
                return;
            }
        }

        addFlashcard(term, definition, 0);
        System.out.printf("The pair (\"%s\":\"%s\") has been added.\n", term, definition);
    }

    void removeCard(){
        System.out.println("Which card?");
        String term = sc.nextLine();
        FlashCard c = flashcards.remove(term);
        if (c != null) {
            System.out.println("The card has been removed.");
        } else {
            System.out.printf("Can't remove \"%s\": there is no such card.\n", term);
        }
    }

    FlashCard getRandomCard(){
        Set<String> set = flashcards.keySet();
        List<String> keyList = new ArrayList<>(set);

        Random random = new Random();
        String key = keyList.get(random.nextInt(keyList.size()));
        return flashcards.get(key);
    }

    void checkCards(){
        System.out.println("How many times to ask?");
        int num = Integer.parseInt(sc.nextLine());

        for(int i = 0; i < num; i++){
            FlashCard card = getRandomCard();
            card.askTerm();
            String answer = sc.nextLine();
            if(card.checkAnswer(answer)){
                System.out.println("Correct!");
            }else{
                Collection<FlashCard> cards = flashcards.values();
                boolean found = false;
                for(FlashCard c : cards){
                    if (c.definition.equals(answer)){
                        System.out.printf("Wrong. The right answer is \"%s\", but your definition is correct for \"%s\".\n",
                                card.definition, c.term);
                        found = true;
                        break;
                    }
                }
                if(!found) System.out.printf("Wrong. The right answer is \"%s\".\n", card.definition);
            }
        }
    }

    void showStats() {
        // sort by mistakes
        List<String> mostErrors = new ArrayList<>();
        int errors = 0;

        List<FlashCard> cards = new ArrayList<>(flashcards.values());
        // Collections.sort(cards, Comparator.comparingInt(card -> card.mistakes));
        // Collections.reverse(cards);
        // get the highest error
        for(FlashCard c : cards){
            errors = Math.max(c.mistakes, errors);
        }
        for(FlashCard c : cards){
            if(c.mistakes >= errors){
                mostErrors.add("\"" + c.term + "\"");
                errors = c.mistakes;
            }
        }
        if(errors == 0){
            System.out.println("There are no cards with errors.");
        }else {
            String terms = String.join(", ", mostErrors);
            if(mostErrors.size() == 1){
                System.out.printf("The hardest card is %s. You have %d errors answering it\n", terms, errors);
            }else{
                System.out.printf("The hardest cards are %s. You have %d errors answering them\n", terms, errors);
            }
        }
    }

    void resetStats(){
        flashcards.values().forEach(FlashCard::resetMistakes);
        System.out.println("Card statistics have been reset.");
    }

    void exportToFile(String fileName){
        if(fileName == null) {
            System.out.println("File name:");
            fileName = sc.nextLine();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            for (FlashCard card : flashcards.values()) {
                writer.println(card.term + "|" + card.definition+ "|" + card.mistakes);
            }
            System.out.println(flashcards.size() + " cards have been saved.");
        } catch (IOException e) {
            System.out.printf("An error occurred while saving the file: %s\n", e.getMessage());
        }
    }

    void importFromFile(String fileName) {
        if(fileName == null) {
            System.out.println("File name:");
            fileName = sc.nextLine();
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                addFlashcard(parts[0], parts[1], Integer.parseInt(parts[2]));
                count++;
            }
            System.out.println(count + " cards have been loaded.");
        } catch (IOException e) {
            System.out.println("File not found");
        }
    }

    @Override
    public void close() {
        sc.close();
    }
}

class Log{
    private Stack<String> stack;

    public Log(Stack<String> stack){
        this.stack = stack;
    }

    private String askFileName(){
        System.out.println("File name:");
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }

    public void writeLog(){
        String filename = askFileName();
        // Write the Stack contents to the file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for(String element : stack) {
                bw.write(element);
                bw.newLine(); // to write each element on a new line
            }
            System.out.println("The log has been saved.");
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
    }
}

class CommandLineParser {
    private String importFilePath;
    private String exportFilePath;

    public CommandLineParser(String[] args) {
        parseArguments(args);
    }

    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-import":
                    if (i + 1 < args.length) {
                        importFilePath = args[++i];
                    } else {
                        System.err.println("Error: Missing argument for -import");
                    }
                    break;
                case "-export":
                    if (i + 1 < args.length) {
                        exportFilePath = args[++i];
                    } else {
                        System.err.println("Error: Missing argument for -export");
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
                    break;
            }
        }
    }

    public String getImportFilePath() {
        return importFilePath;
    }

    public String getExportFilePath() {
        return exportFilePath;
    }
}

public class Main {
    public static void main(String[] args) {
        Stack<String> ioStack = new Stack<>();
        CustomInputStream customInputStream = new CustomInputStream(System.in, ioStack);
        CustomPrintStream customPrintStream = new CustomPrintStream(System.out, ioStack);
        // Redirect System.in and System.out
        System.setIn(customInputStream);
        System.setOut(customPrintStream);

        CollectionFlashCards c = new CollectionFlashCards();
        Scanner sc = new Scanner(System.in);
        boolean exit = false;

        CommandLineParser parser = new CommandLineParser(args);
        if (parser.getImportFilePath() != null) {
            c.importFromFile(parser.getImportFilePath());
        }

        do {
            System.out.println("\nInput the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):");
            String answer = sc.nextLine();
            switch (answer) {
                case "add":
                    c.addCard();
                    break;
                case "remove":
                    c.removeCard();
                    break;
                case "import":
                    c.importFromFile(null);
                    break;
                case "export":
                    c.exportToFile(null);
                    break;
                case "ask":
                    c.checkCards();
                    break;
                case "log":
                    Log logger = new Log(ioStack);
                    logger.writeLog();
                    break;
                case "hardest card":
                    c.showStats();
                    break;
                case "reset stats":
                    c.resetStats();
                    break;
                case "exit":
                    exit = true;
                    System.out.println("Bye bye!");
                    break;
            }
        } while (!exit);

        if (parser.getExportFilePath() != null) {
            c.exportToFile(parser.getExportFilePath());
        }

        sc.close();
    }
}
