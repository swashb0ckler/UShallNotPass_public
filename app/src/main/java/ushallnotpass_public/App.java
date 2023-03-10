package ushallnotpass_public;

import ushallnotpass_public.encryption.Encryption;
import ushallnotpass_public.visuals.ASCII;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class App {

    private static final String PASSWORDS_FILE = "/passwords.txt";
    private static final String DELIMITER = "===";
    private static String encryptedPasswordTest = "not initialized";

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException,
            NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, FileNotFoundException, InterruptedException {

        //Headline
        System.out.println(ASCII.HEADLINE);

        // Initialize empty password
        char[] originalPassword = null;

        //Get password file
        URL url = App.class.getResource(PASSWORDS_FILE);
        File file = new File(url.getFile());

        //If file doesn't exist a custom folder is created
        if (!file.exists()) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Files.createFile(file.toPath());
            } catch (IOException e) {
                System.err.println("Failed to create passwords.txt: " + e.getMessage());
            }
        }

        //Creates scanner
        Scanner scanner = new Scanner(System.in);

        //If first login:
        if(App.checkIfFileIsEmpty(url,true)){

            //Intro Text
            System.out.println(ASCII.FIRSTKEY);
            //Encrypting first password
            System.out.println("Create a key for all future logins:");
            String enteredKey = scanner.nextLine();
            IvParameterSpec ivParameterSpec = Encryption.generateIv();
            SecretKey key = Encryption.createSecretKey(enteredKey, "fixedSalt");
            String encryptedPassword = Encryption.encrypt(enteredKey, key, ivParameterSpec);

            //Save encrypted password test
            encryptedPasswordTest = encryptedPassword;

            //Setting first password
            originalPassword = enteredKey.toCharArray();

            //Writing to first encryptedPassword to txt for next login authentication
            try (FileWriter writer = new FileWriter(url.getFile(), true)) {
                writer.write("Test:" + encryptedPassword + "\n");
                System.out.println("Password added.");
            } catch (IOException e) {
                System.err.println("Error writing to passwords file: " + e.getMessage());
            }

        } else{

            //Intro for existing user
            System.out.println(ASCII.ENTERKEY);

            System.out.println("\n" + "Enter your original key:");
            String enteredKey = scanner.nextLine();

            //Authentication by comparison with test from file
            String encryptedPassword = getEncryptedPassword(file);
            SecretKey key = Encryption.createSecretKey(enteredKey, "fixedSalt");
            String passwordFromFile = Encryption.decrypt(encryptedPassword, key);

            if(passwordFromFile.equals(enteredKey)){
                System.out.println("-ACCEPTED-");
                originalPassword = passwordFromFile.toCharArray();

            } else{
                System.out.println("-DENIED-");
                return;
            }
        }

        while (true) {

            System.out.println("Alright, what do you want to do? (common commands: add, get, quit, help)");

            String command = scanner.nextLine();

            App.commandHandler(command,scanner, originalPassword, url, file);

            if (command.equalsIgnoreCase("quit")){
                break;
            }

        }
        scanner.close();
    }

    public static String getEncryptedPassword(File file) throws FileNotFoundException {
        Scanner fileScanner = new Scanner(file);
        String firstLine = fileScanner.nextLine();
        //Removing 'Test:' from the string
        String pattern = "^Test:\\s*";
        return firstLine.replaceFirst(pattern, "");
    }

    public static void commandHandler(String command, Scanner scanner, char[] originalPassword, URL url, File file) {

        if (command.equalsIgnoreCase("add")) {
            handleAdd(scanner, originalPassword, url, file);
        }
        else if(command.equalsIgnoreCase("get")) {
            handleGet(scanner, originalPassword, url, file);
        }
        else if (command.equalsIgnoreCase("delete")) {
            handleDelete(scanner, originalPassword, url, file);
        }
        else if (command.equalsIgnoreCase("help")) {
            handleHelp();
        }
        else if (command.equalsIgnoreCase("about")) {
            handleAbout();
        }
        else if (command.equalsIgnoreCase("listAll")) {
            handleListAll(scanner, originalPassword, url, file);
        }
        else if (command.equalsIgnoreCase("change")) {
            handleChange(scanner, originalPassword, url, file);
        }
        else if (command.equals("BURN EVERYTHING")) {
            handleBurnEverything(scanner, originalPassword, url, file);
        }
        else if (command.equalsIgnoreCase("quit")){
            handleQuit();
        }
        else{
            System.out.println("Invalid command");
        }

    }

    public static void handleAdd(Scanner scanner, char[] originalPassword, URL url, File file) {

        System.out.println(ASCII.INFORMATION);
        System.out.println("Enter the name of the website you want to add:");
        String name = scanner.nextLine();

        //Checking if name already exists
        boolean websiteAlreadyExists = false;
        try {
            Scanner fileScanner = new Scanner(file);
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                if(line.contains(name)) {
                    System.out.println("Website already exists");
                    websiteAlreadyExists = true;
                    break;
                }
            }
        } catch(FileNotFoundException e) {
            System.out.println("No such file");
        }

        if(!websiteAlreadyExists){

            System.out.println("Enter a username:");
            String username = scanner.nextLine();

            System.out.println("Enter a password:");
            String password = scanner.nextLine(); // Needs to be in bytes for added security

            //Receive the login password
            String originalPasswordString = new String(originalPassword);

            try {
                SecretKey key = Encryption.createSecretKey(originalPasswordString, "fixedSalt");
                IvParameterSpec ivParameterSpec = Encryption.generateIv();
                String encryptedPassword = Encryption.encrypt(password, key, ivParameterSpec);

                try (FileWriter writer = new FileWriter(url.getFile(), true)) {
                    writer.write(name + DELIMITER + username + DELIMITER + encryptedPassword + "\n");
                    System.out.println("Password added.");
                } catch (IOException e) {
                    System.err.println("Error writing to passwords file: " + e.getMessage());
                }

            } catch(Exception e) {
                System.err.println("Error writing to passwords file: " + e.getMessage());
            }
        }
    }

    public static void handleGet(Scanner scanner, char[] originalPassword, URL url, File file){
        System.out.println(ASCII.GETPWD);
        System.out.println("Enter a website or app name:");
        String name = scanner.nextLine();

        try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts[0].equals(name)) {
                    //Receive the login password
                    String originalPasswordString = new String(originalPassword);
                    String plainText = "";

                    //Decryption
                    try {
                        SecretKey key = Encryption.createSecretKey(originalPasswordString, "fixedSalt");
                        plainText = Encryption.decrypt(parts[2], key);
                    } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                            InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
                        e.printStackTrace();
                    }
                    System.out.println("                                    ***************************");
                    System.out.println("                                    * Username: " + parts[1]);
                    System.out.println("                                    * Password: " + plainText);
                    System.out.println("                                    ***************************");
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Password not found.");
            }
        } catch (IOException e) {
            System.err.println("Error reading from passwords file: " + e.getMessage());
        }
    }


    public static void handleDelete(Scanner scanner, char[] originalPassword, URL url, File file){
        ArrayList<String> arrList = new ArrayList<>();
        System.out.println("Enter a website or app name to delete:");
        String websiteName = scanner.nextLine();

        try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                boolean lineToDelete = false;
                String[] parts = line.split(DELIMITER);
                if (parts[0].equals(websiteName)) {
                    lineToDelete = true;
                    found = true;

                } else{
                    if(!lineToDelete){
                        arrList.add(line);
                    }
                }
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(url.getFile()));
            for (String l : arrList) {
                writer.write(l);
                writer.newLine();
            }
            writer.close();

            if (!found) {
                System.out.println("Website/App not found.");
            }
        } catch (IOException e) {
            System.err.println("Error reading from passwords file: " + e.getMessage());
        }
    }

    public static void handleHelp(){
        System.out.println(ASCII.COMMANDS);
    }

    public static void handleAbout(){
        System.out.println(ASCII.ABOUT);
    }

    public static void handleListAll(Scanner scanner, char[] originalPassword, URL url, File file){
        System.out.println("\n" + "Listing all entries:");
        System.out.println("----------------------------------------------------");
        try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
            String line;
            boolean firstLineHasPassed = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if(firstLineHasPassed){
                    System.out.println(parts[0] + "===" + parts[1] + "===" + parts[2]);
                }
                firstLineHasPassed = true;
            }

            System.out.println("----------------------------------------------------");

        } catch (IOException e) {
            System.err.println("Error reading from passwords file: " + e.getMessage());
        }
    }

    public static void handleChange(Scanner scanner, char[] originalPassword, URL url, File file){
        ArrayList<String> arrList = new ArrayList<>();

        System.out.println("Enter a website or app name to change:");
        String websiteName = scanner.nextLine();
        try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
            String line;
            boolean found = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts[0].equals(websiteName)) {

                    //Receive the login password
                    String originalPasswordString = new String(originalPassword);

                    //Enter new password
                    System.out.println("Enter new password");
                    String newPassword = scanner.nextLine();

                    //Encryption
                    try{
                        SecretKey key = Encryption.createSecretKey(originalPasswordString, "fixedSalt");
                        IvParameterSpec ivParameterSpec = Encryption.generateIv();
                        String encryptedPassword = Encryption.encrypt(newPassword, key, ivParameterSpec);

                        arrList.add(parts[0] + "===" + parts[1] + "===" + encryptedPassword);

                    } catch(InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                            InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
                        e.printStackTrace();
                    }

                    found = true;

                } else{
                    arrList.add(line);

                }
            }
            reader.close();

            BufferedWriter writer = new BufferedWriter(new FileWriter(url.getFile()));
            for (String l : arrList) {
                writer.write(l);
                writer.newLine();
            }
            writer.close();
            if (!found) {
                System.out.println("Website/App not found.");
            }
        } catch (IOException e) {
            System.err.println("Error reading from passwords file: " + e.getMessage());
        }


    }

    public static void handleBurnEverything(Scanner scanner, char[] originalPassword, URL url, File file){
        //lineCounter >= 1
        if(App.checkIfFileIsEmpty(url, false)){
            try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //Setting first line to test
                    encryptedPasswordTest = line;
                    break;
                }

            } catch (IOException e) {
                System.err.println("Error reading from passwords file: " + e.getMessage());
            }
            //Checks if file is deleted
            boolean fileDeleted = file.delete();
            if(fileDeleted){
                System.out.println(ASCII.BURNING);
            }

            //Inserting back the test password
            try (FileWriter writer = new FileWriter(url.getFile(), true)) {
                writer.write(encryptedPasswordTest + "\n");
            } catch (IOException e) {
                System.err.println("Error writing to passwords file: " + e.getMessage());
            }

        } else{
            System.out.println("Nothing to burn..");
        }
    }

    public static void handleQuit(){
        System.out.println(ASCII.OUTRO);
        System.out.println("See you later!");
    }

    public static boolean checkIfFileIsEmpty(URL url, boolean includingTest){
        int lineCounter = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
            while (reader.readLine() != null) {
                lineCounter++;
            }

        } catch (IOException e) {
            System.err.println("Error reading from passwords file: " + e.getMessage());
        }

        //For first login
        if (includingTest && lineCounter < 1){
            return true;
        }
        //For BURN EVERYTHING
        else if (!includingTest && lineCounter > 1){
            return true;
        }
        return false;
    }


}
