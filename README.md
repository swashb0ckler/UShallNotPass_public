# UShallNotPass

<p align="center">
    <img width="450px" src="https://media3.giphy.com/media/SlhWQJAX3rCBqgizHR/giphy.gif"/>
</p>


Do you struggle to remember all of your usernames and passwords?
With UShallNotPass you can turn your command-line into a vault for all of your login information, so you don't have to spend time trying to remember your netflix username and password ever again.

 
## How it Works
UShallNotPass uses AES-CBC encryption and writes the ciphertext to a txt file. The original password is used for future authentication and to encrypt all new passwords. 

**Features include**

* Common commands such as getting, deleting and changing passwords
* Listing all websites, usernames and encrypted passwords
* A 'BURN EVERYTHING' command. 

<p align="center">
    <img width="800px" src="https://www.linkpicture.com/q/USHALLNOTpassScreen_1.png"/>
</p>


## Try It out 

Fork or download the project. Navigate to the project folder and run:  

```
gradle build
gradle shadowJar 
```
Navigate to the app/build/libs directory and copy the *UShallNotPass.jar* file to the folder you would like, for example desktop and write the following in the terminal:

```
java -jar UShallNotPass.jar
``` 






