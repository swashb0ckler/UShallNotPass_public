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
import java.util.Base64;
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

        URL url = App.class.getResource(PASSWORDS_FILE);

        System.out.println(ASCII.HEADLINE);
        System.out.println(ASCII.ENTERKEY);

        // Initialize empty password
        char[] originalPassword = null;

        File file = new File(url.getFile());


        if (!file.exists()) {
            try {
                Files.createDirectories(file.getParentFile().toPath());
                Files.createFile(file.toPath());
                System.out.println("Created passwords.txt");
            } catch (IOException e) {
                System.err.println("Failed to create passwords.txt: " + e.getMessage());
            }
        } else {
            System.out.println("passwords.txt already exists");
        }

        Scanner scanner = new Scanner(System.in);

        //Testing if there are more than one line
        int lineCounter = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
            String line;
            boolean firstLineHasPassed = false;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if(firstLineHasPassed){
                    //System.out.println(parts[0] + "===" + parts[1] + "===" + parts[2]);
                }
                firstLineHasPassed = true;
                lineCounter++;
            }

        } catch (IOException e) {
            System.err.println("Error reading from passwords file: " + e.getMessage());
        }

        //Checks if first login
        if(lineCounter < 1){

            //Encrypting password with real key
            System.out.println("Create a key for all future logins:");
            String enteredKey = scanner.nextLine();
            IvParameterSpec ivParameterSpec = Encryption.generateIv();
            SecretKey key = Encryption.createSecretKey(enteredKey, "fixedSalt");
            String encryptedPassword = Encryption.encrypt(enteredKey, key, ivParameterSpec);

            //Save encrypted password test
            encryptedPasswordTest = encryptedPassword;

            //Setting the original password
            originalPassword = enteredKey.toCharArray();

            //Writing to file
            try (FileWriter writer = new FileWriter(url.getFile(), true)) {
                writer.write("Test:" + encryptedPassword + "\n");
                System.out.println("Password added.");
            } catch (IOException e) {
                System.err.println("Error writing to passwords file: " + e.getMessage());
            }

        } else{
            System.out.println("\n" + "Enter the key you normally use:");
            String enteredKey = scanner.nextLine();

            String encryptedPassword = getEncryptedPassword(file);

            //Authentication by comparison with test from file
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

            if (command.equalsIgnoreCase("add")) {

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

                    SecretKey key = Encryption.createSecretKey(originalPasswordString, "fixedSalt");
                    IvParameterSpec ivParameterSpec = Encryption.generateIv();
                    String encryptedPassword = Encryption.encrypt(password, key, ivParameterSpec);

                    try (FileWriter writer = new FileWriter(url.getFile(), true)) {
                        writer.write(name + DELIMITER + username + DELIMITER + encryptedPassword + "\n");
                        System.out.println("Password added.");
                    } catch (IOException e) {
                        System.err.println("Error writing to passwords file: " + e.getMessage());
                    }
                }

            } else if (command.equalsIgnoreCase("get")) {
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

                            //Decryption
                            SecretKey key = Encryption.createSecretKey(originalPasswordString, "fixedSalt");
                            String plainText = Encryption.decrypt(parts[2], key);

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

            } else if (command.equalsIgnoreCase("delete")) {
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

            }else if (command.equalsIgnoreCase("quit")) {
                System.out.println(ASCII.OUTRO);
                System.out.println("See you later!");
                break;
            } else if (command.equalsIgnoreCase("help")) {
                System.out.println(ASCII.COMMANDS);
            }
            else if (command.equalsIgnoreCase("about")) {
                System.out.println(ASCII.ABOUT);
            }
            else if (command.equalsIgnoreCase("listAll")) {
                System.out.println("Listing all entries:");
                System.out.println("----------------------------------------------------");
                try (BufferedReader reader = new BufferedReader(new FileReader(url.getFile()))) {
                    String line;
                    boolean firstLineHasPassed = false;
                    int counter = 0;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(DELIMITER);
                        if(firstLineHasPassed){
                            System.out.println(parts[0] + "===" + parts[1] + "===" + parts[2]);
                        }
                        firstLineHasPassed = true;
                        counter++;
                    }
                    System.out.println("----------------------------------------------------");
                    if(counter <= 1){
                        System.out.println("...There are no entries");
                    }

                } catch (IOException e) {
                    System.err.println("Error reading from passwords file: " + e.getMessage());
                }
            }
            else if (command.equals("BURN EVERYTHING")) {
                if(file.exists()){
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
                } else{
                    System.out.println("Nothing to burn..");
                }

                //Inserting back the test password
                try (FileWriter writer = new FileWriter(url.getFile(), true)) {
                    writer.write(encryptedPasswordTest + "\n");
                } catch (IOException e) {
                    System.err.println("Error writing to passwords file: " + e.getMessage());
                }


            }
            else if (command.equalsIgnoreCase("change")){
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
                            SecretKey key = Encryption.createSecretKey(originalPasswordString, "fixedSalt");
                            IvParameterSpec ivParameterSpec = Encryption.generateIv();
                            String encryptedPassword = Encryption.encrypt(newPassword, key, ivParameterSpec);

                            arrList.add(parts[0] + "===" + parts[1] + "===" + encryptedPassword);

                            found = true;
                            break;

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


            }else {
                System.out.println("Invalid command. Try again.");
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




}

/*
 *   BACKLOG
 *
 *
 * //
 * Need to fix the shadowJar problem **CHECK
 * Need to create new password file after BURN EVERYTHING,change and so on **CHECK
 * Need to fix the decoder problem
 * Refactor
 *
 *
 * //
 *   0) Setup github and check how to make things private and then public later **CHECK
 *
 *   1) Encrypt password-entry to txt using final toy byte key **CHECK
 *   2) get receives the website, username and password from txt and decrypts password using the toy key **CHECK
 *
 *   3) if file doesn't exist (first time using) **CHECK
 *       - (Check to see password strength (=======))
 *       - bytes[] userInput = scanner.nextline().bytes
 *       - userInput (key) is used to encrypt the text CheckTo345290See!!IfKeyIsCorrect
 *       - Encrypted test goes to txt file
 *
 *   4) if file exists, the program asks for a key. if the key test pass, the user input is saved as key used for encryption. **CHECK
 *       - user input is  --> saved in bytearray **IMPORTANT
 *       - the test is received
 *       - input is encrypted and compared with the encrypted test from txt
 *       - if there is a match: store the user input as key (maybe in keyStore)
 *       - -ACCEPTED- else -DENIED-
 *
 *
 *   5) Change an entry **CHECK
 *      - name of entry is saved as siteName
 *      - if passwords.txt contains siteName -->
 *      - a new list is made
 *      - all entries from passwords.txt are inserted into a map/list
 *          - if entry elem == siteName
 *              prompt user with new username and new password
 *      - siteName===username===password is inserted
 *      - the rest of the entries are inserted
 *      - passwords.txt is wiped and list is copied into passwords.txt
 *      - -Username and Password changed-
 *
 *   6) Delete an entry **CHECK
 *      - (are you sure?) scanner.nextline()
 *      - name of entry is saved as siteName
 *      - if passwords.txt contains siteName -->
 *      - a new list is made
 *      - all entries from password.txt is inserted into list
 *          if entry elem == siteName then do not include
 *      - rest of entries are inserted
 *      - -Sitename + has been deleted
 *
 *
 *   7) List all entries **CHECK
 *      - make new list
 *      - read each line from passwords.txt and insert into list
 *      - for each elem in list
 *          - split elem in 3 parts
 *          - password = decrypt part 3 using secret key
 *          - sout(website: part[1], username: part[2], password)
 *
 *   8) Make it easy to send - package as a jar file
 *      - jar cfm MyProgram.jar Manifest.txt *.class --> creates jar
 *      - java -jar MyProgram.jar --> when friends need to run it
 *
 *
 *   9) Make github page with animations and so on.
 *      - Listing the good things: uses AES, bitstring so not vulnerable to memory dumps,
 *      - Disclaimer: While the secret key is hidden, the plaintext test makes plaintext attacks and brute force easier.
 *
 *
 *
 *   UShallNotPass is a simple encrypted password manager which turns your terminal into a vault for all of your usernames and passwords across the internet or perhaps for your wifi or netflix account.
 *  - A java command line tool which uses AES encryption to write entries to a txt file.
 *  - Simple and fun project to remember all of your passwords scattered across, well anywhere.
 *
 *  - Insert gif from ENTERKEY
 *
 *  - Getting started
 *      - Install java
 *      - run: ---------
 *      - Make sure your are in the directory you want to use
 *
 *  - more gifs
 *
 * */